# Architecture

The repository is a monorepo with separately built frontend and backend applications. The backend is a modular monolith: business features own their controllers, services, DTOs, entities and repositories. Features are `employee`, `document`, `leave`, `onboarding` and `employmenthistory`. Each owns `controller`, `service`, `repository`, `entity` and `dto` subpackages. Add future modules alongside these; create layers only when needed. Shared exception translation lives in `com.ems.common.exception`.

Controllers handle HTTP concerns and delegate to services. Services own business rules and transactions; repositories provide Spring Data persistence without a redundant DAO layer. Entities accept domain values, never API DTOs. Response DTO factories may map entities, but entities cannot depend back on DTOs. Use constructor injection and explicit project imports. Integration tests live under their owning feature; `PackageArchitectureTest` checks package placement and project dependency directions without adding a dependency.

Employment history has its own controller and service. EmployeeService delegates snapshot recording to EmploymentHistoryService, whose write methods require an existing transaction. Employee row locks, snapshot rollback, REST routes, JSON fields, entity names and table names remain unchanged. Cross-feature employee entity references and repository lookups are intentional for ownership checks; cross-feature business operations use services.

## Request flow

React components call feature-specific API functions through the shared HTTP client. `/api/v1` controllers validate request DTOs, call transactional services and return response DTOs. JPA repositories handle persistence. Entities are not exposed directly in HTTP responses.

The Vite development server proxies API requests to Spring Boot, avoiding cross-origin configuration for local development. For production, serve the frontend and API under one origin using a reverse proxy or explicitly configure trusted frontend origins.

## Persistence and error handling

H2 uses a local file under the backend working directory. Hibernate updates the schema for local development only. Replace this with versioned migrations and a production database before deployment. Tests use an isolated in-memory database. No seed data is loaded.

Request validation uses Jakarta Validation. Errors follow Spring problem details, with field-level validation errors and conflict responses for duplicate email addresses. Email addresses are normalized before persistence, and the database unique constraint handles concurrent duplicate submissions.

## Extension points

- Identity and access: implement authentication and role-based authorization across all employee endpoints.
- Organization: replace department text with department and reporting-line relationships.
- Employee lifecycle: add onboarding, employment history and audited status transitions.
- Attendance and leave: create separate feature modules with their own business rules.
- Scale: the UI uses bounded database pagination; add indexes and specialized text search as data grows.
- Operations: add schema migrations, structured observability and deployment configuration.

Payroll, leave approval, attendance tracking, immutable audit history and production hosting remain unimplemented. The unmerged authentication branch adds the session boundary described below. The UI exposes list/create and a dedicated view/edit profile screen using the existing GET/PUT endpoints; deletion is currently API-only. Profile data is fetched when opened. A successful update invalidates the directory; returning fetches the applied search page and organization totals again. Failed saves retain the draft. Requests abandoned while loading a profile are cancelled and ignored. Concurrent edits from different users still follow last-write-wins; optimistic locking is a future improvement.

## Directory queries

The UI uses `GET /api/v1/employees/directory`, a dedicated DTO with a bounded employee page, matching counts and separate global aggregates. A JPA Specification combines literal case-insensitive substring search with status equality. Criteria `locate` avoids treating SQL wildcard characters as patterns. Page requests use an allowlisted sort (ID by default) and validate size/offset before repository access. EmployeeService maps sortBy/direction to fixed Spring Data orders: case-insensitive last/first name, department, job title, or numeric ID. Non-ID sorts append ID ascending for deterministic ties. Arbitrary property paths never reach persistence. No extra dependency or schema change is required.

The original list endpoint remains available for existing callers, but is no longer used by the UI. Filter submission resets page zero. Aborted requests cannot replace newer directory state. Empty pages after deletion or changed filter membership trigger a fetch of the last valid page. Database search and aggregate queries can observe concurrent changes at the configured isolation level; pagination is not a frozen snapshot. Substring scans and aggregate counts still need performance review before large-scale production use.

## Recorded employment history

EmployeeService delegates employment snapshots to EmploymentHistoryService in the same transaction as profile creation/update. Updates take a pessimistic write lock on the employee row to serialize the baseline and change snapshot decision. History does not add optimistic edit-conflict detection: profile updates still use last-write-wins. Read APIs return DTO pages sorted by descending history ID. Database foreign-key cascade removes snapshots on employee deletion. This is a profile history feature, not an immutable compliance audit trail.

## Leave mutation consistency

LeaveRequestService owns create/edit/cancel transactions. Every mutation takes a pessimistic write lock on the employee before loading or modifying a request and checking inclusive pending overlaps. Edits exclude the current request ID and reject cancelled records. The entity accepts type/date values without depending on transport DTOs; the controller validates the existing standalone LeaveRequestInput. Concurrent edits remain last-write-wins, but create/edit/cancel share the same lock ordering. No schema or dependency change is required.

## Identity and sessions (authentication branch, not yet merged)

Identity owns controller/service/repository/entity/dto layers. SessionController maps the
framework authentication/CSRF context; AccountController delegates provisioning to
AccountService. IdentitySecurityConfiguration is infrastructure wiring in the identity
service package and enforces the route matrix in docs/authentication-plan.md. The account
provider delegates credential verification to the transactional account service. No
cross-feature persistence or locking semantics were changed.

Spring Security protects every employee route for HR only. ADMIN provisions accounts but
has no employee-data bypass. Manager/employee access is denied until explicit ownership
relationships exist. Sessions are server-side, CSRF remains enabled, and cookies default
to Secure/HttpOnly/SameSite Strict. Existing HTTP tests use authenticated HR fixtures with
CSRF through the real filter chain; dedicated identity API/E2E tests use actual login.
No architecture or coverage guardrails are relaxed. See the authentication plan for the
remaining abuse-control, revocation, reset and scoped-authorization gates before merge.

LoginAttemptService is an identity service owning synchronized, bounded in-memory login
admission. AccountAuthenticationProvider reserves before credential verification and releases
in finally; successful credentials reset only their normalized login counter. No controller,
repository or entity responsibilities are moved. This single-instance limiter is not a
replacement for distributed controls or security auditing; see the authentication plan.

Cross-tab sign-out is a frontend identity concern: sessionEvents publishes only a random
notification marker in localStorage, never credentials, identity or session/CSRF tokens.
The subscriber clears identity/password/CSRF state; a session generation guard rejects
late session-check responses after sign-out. Server logout remains authoritative and
storage failure cannot undo it. This is best-effort same-origin browser notification,
not server-side password/role-change revocation or a distributed session store.

AccountProvisioning is a focused React identity component using the shared CSRF-aware HTTP
client. AuthShell selects it for ADMIN, while HR retains the employee workspace and other
roles remain denied business access. Backend authorization is unchanged and authoritative;
the existing AccountController delegates to AccountService. Creation defaults to EMPLOYEE,
passwords clear on both success/failure, and a ref guards duplicate pending submissions.

The identity account directory delegates HTTP pagination to AccountService, which validates
a bounded ten-row offset and requests repository pages ordered by unique ID. AccountPage
contains only AccountIdentity DTOs; persistence entities/hashes never leave the service.
The existing ADMIN route rule protects reads as well as provisioning. AccountDirectory
loads on demand, aborts on unmount and ignores late responses. No permission bypass,
new schema, dependency, or transaction/locking changes are introduced.

AuthenticationAuditService is a focused identity service. AccountAuthenticationProvider
records credential outcomes through constructor injection, including admission rejection,
while preserving release-in-finally and rethrowing the original failure. Only fixed event
codes and the persisted numeric success identity reach its logger; no HTTP strings or
exceptions cross that interface. It changes no repository, transaction or role boundaries.
Application-log records are an initial audit sink; durability and broader event coverage
remain requirements described in the authentication plan.

Audit API validation exposed duplicate failed verification through local/parent authentication
managers. The security chain now uses a dedicated ProviderManager with no parent fallback,
so each HTTP login attempts credentials once. Regression tests assert one audit record per
request and successful correct credentials after four failures (the fifth admitted attempt).

AccountProfileController obtains the account key from Authentication and delegates to the
read-only AccountService identity lookup. It maps a missing account to401; entities remain
behind AccountIdentity DTOs. AccountProfile is a focused, on-demand React identity component
using the shared client. Existing role scopes, transactions and locking are unchanged.
