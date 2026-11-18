# Next feature: authentication and roles

Requested October 25, 2026. This records the authentication implementation and its validation requirements on the feature branch. Main remains unauthenticated; neither branch is ready for enterprise deployment.

Deliver coherent `feature/auth-*` increments after the pending testing foundations. Use Spring Security, separate identity feature/layer packages and the existing React architecture. All merge, coverage, architecture and real-backend E2E gates remain mandatory.

## Current combined lifecycle implementation — November 17, 2026

The authentication branch now contains the combined lifecycle implementation below. Earlier dated sections are historical increment records, not current completion claims. Final validation and merge status are recorded in PR22; source changes alone do not establish completion. Enterprise deployment is not authorized or ready.

| Endpoint group | ADMIN | HR | MANAGER | EMPLOYEE | Anonymous |
| --- | --- | --- | --- | --- | --- |
| Health, CSRF, login, token redemption | Available | Available | Available | Available | Available |
| Own session/profile/password/logout | Own account | Own account | Own account | Own account | Denied |
| Account directory/provision/access/scope/reset issuance/audit | Allowed | Denied | Denied | Denied | Denied |
| Employee collection/directory/profile and nested document/leave/onboarding/history | Denied | Allowed | Denied | Denied | Denied |
| GET self/employees and self/employees/{id} | Denied | Denied | Linked employee plus explicit team IDs | Linked employee only | Denied |
| Other paths or self-service mutations | Denied | Denied | Denied | Denied | Denied |

Unsafe requests always require CSRF, including anonymous reset redemption. Permission is enforced by the backend on every request. ADMIN has no implicit HR privilege. No team/employee relationship means no scoped access; department labels never grant access. Scoped access exposes only the existing employee profile fields, not documents or other nested resources. Employee links are unique per account relationship. Scope changes revoke sessions; role changes away from MANAGER remove team assignments so later promotion cannot restore stale grants. Deleted employee IDs return404 and are not reassigned by this API.

Account passwords use the established BCrypt policy. Password changes require the current password. Every authenticated request verifies a persisted account ID/login/security-version stamp; password, role, enabled-state and scope changes increment the stamp. Disabled or stale sessions receive401 and are invalidated. Requests already admitted before a concurrent change can finish; revocation applies at the next authentication check. Admin mutations lock accounts in ID order and cannot disable/demote the final active administrator.

Recovery is an administrator-assisted, identity-verified manual handoff. The application sends no messages. An active account receives a cryptographically random256-bit token, displayed once in the admin UI response, stored only as SHA-256, expiring in15minutes. Reissuance invalidates the previous token. Redemption locks the account, enforces the password policy, consumes the token and revokes old sessions atomically. Expired/invalid/reused tokens receive a generic error. A bounded shared reset-attempt bucket limits online attempts; operational tuning and rate-limit monitoring remain required before production. Tokens/passwords never enter URLs, browser storage or audit payloads.

Security events are persisted with fixed action codes, timestamp and optional numeric actor/subject IDs. Lifecycle writes and audit events share a transaction. Only ADMIN can read paginated audit records; there is no update/delete audit API. Credential/session/access-rejection events also persist, with unknown identities left unattributed. Failed persistence of session creation invalidates that new session. This is application append-only storage, not tamper-proof external archival.

### Explicit operational readiness work
- MFA: not implemented; assess phishing-resistant WebAuthn and recovery enrollment before privileged production use.
- Enterprise SSO: not implemented; assess OIDC/SAML provider trust, provisioning/deprovisioning, role mapping and local recovery policy.
- PostgreSQL/versioned migrations, tested backup/restore, multi-instance session storage/revocation, TLS/reverse-proxy configuration, audit retention/export/tamper controls, monitoring and load/failure exercises remain operational roadmap items.
- Current H2 schema update is for local development/testing; do not treat it as a production migration strategy. Secure cookies default on; only isolated loopback HTTP tests opt out.
- The presentation helper remains on its separate preview branch. No presentation seeding or default credentials have been enabled here.

## Proposed least-privilege roles

| Role | Intended scope |
| --- | --- |
| Admin | Account provisioning, role assignment, deactivation and security administration |
| HR | Authorized employee lifecycle administration; no security-role elevation |
| Manager | Assigned team only; no arbitrary employee access or role administration |
| Employee | Own account and explicitly permitted self-service operations only |

Specify exact endpoint/field permissions before implementation. Account-to-employee linkage and explicit team relationships are prerequisites for scoped access. Deny access when linkage is absent; department text is not an authorization boundary. Employment changes and document access must not become self-service merely because a user is authenticated. Admin access to HR data must be an explicit permission, not an implicit role bypass.

## Required capabilities and acceptance

- Responsive login, logout, access-denied, session-expiry and account-management screens backed by real authenticated APIs.
- Adaptive password hashing; server-side sessions; secure production cookies, CSRF protection, session fixation protection and expiry. No password/token storage in browser local storage.
- Protected account provisioning, password changes, deactivation and role changes. No hardcoded/default credentials or public role-granting registration. Revocation must take effect on password, role and account-state changes.
- Recovery/reset with expiring single-use tokens stored safely, generic responses and abuse controls. Implement and test delivery boundaries without sending email or external messages; document the unconfigured production delivery prerequisite.
- Login abuse protection and safe security audit events without credential/token disclosure. Document trusted proxy and TLS requirements.
- Backend enforcement for every endpoint, including collections, aggregates, nested employee resources and direct ID access. UI visibility is supplementary.
- Unit, API contract, service/persistence integration and Playwright suites for allowed/denied role actions, anonymous access, cross-user/team access, invalid credentials, CSRF, expiry/logout, revoked sessions and expired/reused reset tokens. Synthetic accounts only.
- Measured 100% line/branch coverage for changed production code and full-project reporting; no threshold weakening. Required tests and builds must pass before merge.
- Assess MFA and enterprise SSO, migrations, operational secret rotation, session storage and recovery in the readiness roadmap. Do not label authentication or the application enterprise-ready until requirements are implemented and verified.

No deployment, paid identity provider, real employee/account data or external message sending is authorized. The existing development automation now prioritizes this feature and preserves the current schedule.

## Credential foundation (feature/auth-accounts, pending integration)

The identity module now has an internal account repository and service. Accounts use unique,
case-normalized login identifiers (3–64 ASCII letters/digits/dots/underscores/hyphens,
starting with a letter or digit). Roles are ADMIN, HR, MANAGER and EMPLOYEE. An identity
response contains only account ID, login and role; password hashes remain in persistence.
Spring Security Crypto is managed by the existing Spring Boot dependency BOM; existing
versions are not upgraded. BCrypt uses a fresh salt and cost 12. Provisioning requires at
least 15 Unicode code points and at most 72 UTF-8 bytes, rejecting overlong input instead
of silently truncating it. Passwords are not trimmed or case-folded. Missing-account
verification also performs a hash comparison against a randomly generated dummy value;
this is not a substitute for login abuse controls or a constant-time guarantee.

This is backend groundwork, not a completed authentication feature. There is deliberately
no public provisioning/verification endpoint, bootstrap account, registration, seed
credential, authenticated session or frontend login yet. Role persistence does not grant
permissions. Existing employee APIs remain unauthenticated. Keep this branch/PR unmerged
until the coherent authenticated flow, endpoint authorization and its acceptance tests
are implemented. Do not expose these internal services through an unprotected controller.

Next on this branch: define employee/account and manager/team relationships; implement
protected administrative provisioning, Spring Security sessions/CSRF/cookies, generic
login errors and abuse controls, safe audit records and React login/logout/access-denied.
Then add account changes with session revocation and expiring single-use reset tokens.
Enforce the matrix on every collection, aggregate and nested endpoint. Add negative unit,
API, integration and real-backend browser scenarios before enabling access. MFA, SSO,
versioned migrations and production session/recovery operations remain readiness work.

## November 6 — Session increment (PR22, still draft)

The branch now enables Spring Security server-side sessions, default session-ID rotation
on login, CSRF on unsafe requests including login/logout, and a 30-minute inactivity timeout.
Session cookies default to Secure, HttpOnly and SameSite=Strict. For loopback-only HTTP
validation set SESSION_COOKIE_SECURE=false; production must retain Secure and use TLS.
Frontend and API must share an origin (Vite proxies /api during development).

| Route | Anonymous | ADMIN | HR | MANAGER / EMPLOYEE |
| --- | --- | --- | --- | --- |
| GET /actuator/health | Health only | Health only | Health only | Health only |
| GET /api/v1/auth/csrf | CSRF token | CSRF token | CSRF token | CSRF token |
| POST /api/v1/auth/login | Credentials + CSRF | Credentials + CSRF | Credentials + CSRF | Credentials + CSRF |
| GET /api/v1/auth/session | 401 | Own identity | Own identity | Own identity |
| POST /api/v1/auth/logout | CSRF required | Own session | Own session | Own session |
| POST /api/v1/admin/accounts | Denied | Provision account | Denied | Denied |
| /api/v1/employees and every nested route | Denied | Denied | Employee lifecycle | Denied |
| Other routes | Denied | Denied | Denied | Denied |

/error is permitted only as framework error rendering; it exposes no business operation.
Manager and employee roles have no business access yet. No department-text shortcut or
unlinked-account fallback grants access. Explicit employee linkage and team relationships
are still required before enabling scoped permissions. Admin does not inherit HR access.

An operator may explicitly supply EMS_BOOTSTRAP_LOGIN and EMS_BOOTSTRAP_PASSWORD to create
one ADMIN when the account table is empty. Both must be supplied; there are no defaults.
Existing accounts are never overwritten. This is a single-instance initial setup mechanism,
not a multi-instance bootstrap or recovery protocol. Remove bootstrap secrets after setup.
The admin can provision accounts through the CSRF-protected account endpoint. There is no
public registration. Account-management UI is still pending; non-HR users see access denied
in the directory UI and can sign out. Passwords remain only in transient form state.

React now shows login, generic failed-login feedback, session checking/retry, logout and
access denied. An API 401 removes the employee workspace and requests a new sign-in.
The browser obtains a new CSRF token after login/logout; no tokens are stored in localStorage.
Existing business E2Es now authenticate as synthetic HR instead of bypassing filters.
Four new session/role scenarios run at all five viewport widths against the real backend.

Remaining merge blockers: login abuse controls, safe security audit events, account-to-employee
and manager/team scope, account-management UI, password/profile changes, role/deactivation
revocation, reset expiry/reuse, and complete acceptance coverage for those capabilities.
The configured inactivity timeout still needs a dedicated expiry integration test.
MFA, SSO, versioned migrations, production session persistence, backup/recovery and security
operations remain readiness work. Do not mark the entire authentication feature complete.

The presentation launcher now creates an ephemeral admin with generated credentials in its
disposable database, provisions an explicitly supplied presentation HR identity, signs out
the admin, and passes an HR session/CSRF token to Java Faker before opening the UI. It never
prints credentials. PRESENTATION_LOGIN/PRESENTATION_PASSWORD are required. Existing seed
opt-in, empty-database checks, deterministic names/example.com emails and cleanup remain.

## November 7 — Single-instance login admission controls

The authentication provider now reserves admission before password verification and releases
its in-flight slot in a finally block, including unexpected persistence failures. A normalized
login gets at most five admitted checks in a fixed 60-second window; a successful sign-in
clears that login's counter. Denied attempts do not extend the window. Counters are shared
across sessions, and unknown and known accounts follow the same rules and generic HTTP 401
response. The UI suggests checking credentials or waiting a minute without exposing whether
an account exists or which limit was reached.

The guard synchronizes admission and caps concurrent password checks at 16. It retains at
most 1,024 bounded login keys, purging expired entries during admission. Overlong identifiers
share an invalid-identifier bucket. No passwords, tokens or forwarded headers are retained.
At capacity, new keys fail closed; existing keys still obey their limits. Unit tests cover
concurrent admission, expiry boundaries, capacity reclamation and release on failure. API,
persistence integration and real-browser tests cover lockout across fresh sessions, generic
failures, recovery after expiry and an independent account remaining usable. Browser tests
use unique synthetic throttle accounts per viewport and do not weaken limits.

This is an initial single-process abuse control, not distributed brute-force protection.
Counters reset on process restart; multiple instances require a shared atomic limiter and
trusted edge/source controls. An attacker can temporarily deny a targeted login or exhaust
admission capacity; there is no permanent account lock or external notification. Deployment
requires capacity tuning, load/abuse testing, trusted-proxy design, alerting and shared-state
availability decisions. Safe audit events, ownership scope, account lifecycle/revocation,
reset workflows and their acceptance gates remain unfinished. Keep PR22 unmerged.

## November 8 — Session recovery acceptance

Passwords now clear after transport failures, including failure to retrieve CSRF, in addition
to HTTP login rejection. Unit and HTTP-fixture integration tests cover both request stages.
The real browser suite toggles browser connectivity without mocking API responses, then
signs in against the real backend. Another scenario signs out in a second tab, verifies the
first tab removes employee content on its next API 401, and signs in with fresh CSRF.
This is request-triggered recovery, not immediate cross-tab logout propagation.

A dedicated embedded-Tomcat integration test checks the configured 1,800-second inactivity
interval, shortens only the synthetic session to one second, and waits for genuine container
expiry. It verifies authentication and old CSRF rejection, followed by successful fresh login.
Production timeout configuration is unchanged. These tests do not establish password/role
change revocation or reset-token lifecycle; those and safe audit/ownership remain incomplete.

## November 9 — Cross-tab sign-out

The November 8 request-triggered limitation is now supplemented by a best-effort browser
storage notification after successful server logout. Other same-origin tabs immediately
clear identity, password and CSRF state when the event is delivered. An opaque random marker
is the only stored value; there are no identities, passwords or session tokens in storage.
A generation guard ignores late session-check results/errors after logout notification.
Failed logout does not publish; storage-denied browsers still complete server logout and
retain API-401 recovery in other tabs. Unrelated storage updates and key removal are ignored;
listeners detach when the shell unmounts. Suspended tabs/browser storage restrictions can
still delay or prevent notifications. This does not implement account-change revocation.

## November 10 — ADMIN provisioning screen

ADMIN now sees Create account, backed by the existing CSRF-protected POST endpoint. The
form supports the four roles, defaults to EMPLOYEE, explains the current restricted scopes,
shows API validation/conflict failures and clears its password after every submission.
Successful creation returns only identity/role; no credential is displayed, persisted in
browser storage or sent externally. Non-secret failed drafts are retained for correction.
A pending request disables inputs and duplicate submission. The server remains the authority
for password byte limits, uniqueness, role authorization and normalization. Tests verify
all assigned roles round-trip through actual login and a new EMPLOYEE cannot provision ADMIN.
This is creation only: account list/edit/deactivation, profile/password changes and reset
are still pending alongside audit, ownership scope and session revocation. No enterprise
readiness or completed authentication claim is made.

## November 11 — Provisioning role acceptance

Real browser tests now select ADMIN, HR and MANAGER in the creation form and sign in as
those new identities. Together with the existing EMPLOYEE flow, all four selections are
covered through actual provisioning and authentication. Tests assert exactly one assigned
role, HR-only employee API access and denied HR/manager provisioning despite valid CSRF.
Frontend integration also removes the creation form and password when its POST receives401.
No account update/revocation/reset behavior is implied by these creation-only tests.

### Account directory increment — November 12 (unmerged)
ADMIN can review account identities through GET /api/v1/admin/accounts and the on-demand
UI directory. Ten-row pages use ascending unique IDs; nonnegative offsets must fit a signed
integer. Responses explicitly contain ID/login/role only. No HR/manager/employee read
permission is added. Refresh restarts at page zero; concurrent provisioning can change
totals between requests, so pagination is not a snapshot. Account mutation, revocation,
password/reset lifecycle, audit and ownership gates remain unfinished.

### November 13 — Credential verification audit foundation (PR22, unmerged)
Credential verification now emits timestamped application-log records with fixed event codes:
AUTH_CREDENTIALS_ACCEPTED (persisted numeric account ID only), AUTH_CREDENTIALS_REJECTED
(including limiter rejection), and AUTH_VERIFICATION_UNAVAILABLE (unexpected verification
failure). No submitted login, password, token, header or exception message/stack is supplied
to this audit logger. Unknown and known-account rejection keep identical generic HTTP errors.
Accepted credentials do not prove that a session was subsequently established. CSRF rejection
occurs before the provider and is outside this increment; logout, authorization and account
mutation auditing remain pending.
These are application logs, not durable/tamper-evident audit storage. Configure access,
retention, collection, volume controls and failure monitoring before production; rejected
records deliberately cannot correlate a submitted identity. No audit retrieval endpoint or
browser disclosure is introduced. Full identity readiness remains blocked by scoped ownership,
account lifecycle/revocation/reset and the remaining audit/operational requirements.
Unit/API/persistence collaboration tests verify outcomes and secret-free records; frontend
unit/integration and real-backend browser tests verify generic errors and successful recovery.
Executed validation and exact head/base SHAs are recorded in PR22.

Audit API validation exposed duplicate failed verification through local/parent authentication
managers. The security chain now uses a dedicated ProviderManager with no parent fallback,
so each HTTP login attempts credentials once. Regression tests assert one audit record per
request and successful correct credentials after four failures (the fifth admitted attempt).

### November 14 — Read-only authenticated account profile (PR22, unmerged)
All four authenticated roles can select View my account to read their stored account ID,
login and assigned role. GET /api/v1/auth/profile resolves only the authenticated principal;
query parameters cannot select another account. Passwords/hashes are never returned.
Anonymous or no-longer-stored identities receive401. The UI clears stale data on failed
refresh, ignores late unmounted responses and removes the profile on session expiry/logout.
This account identity view does not expose employee records or enable scoped employee/team
access. Account mutation, password changes, revocation, reset and complete audit infrastructure
remain unfinished. Exact validation results and source/base SHAs are recorded in PR22.

### November 15 — Session lifecycle audit boundaries (PR22, unmerged)
Successful form-login handling now records AUTH_SESSION_ESTABLISHED after Spring Security
saves authentication and applies its session strategy. Completed logout handling records
AUTH_LOGOUT_PROCESSED after the default logout handlers run. Both events are fixed codes
with no identity, cookie, token, header or exception arguments. A CSRF-rejected logout
produces no completion event and leaves the session usable. A valid anonymous logout can
still complete idempotently, so LOGOUT_PROCESSED does not assert an authenticated actor.
These application-log records have no per-session correlation and do not cover inactivity
expiry, denied authorization or account mutations. Durable collection, retention/access
controls and volume/failure monitoring remain prerequisites. Unit/API and real-container
integration checks verify event boundaries; frontend tests and real-backend browser flows
verify rejected logout followed by successful cleanup. Full validation is recorded in PR22.

### November 16 — Access rejection audit boundary (PR22, unmerged)
Security-filter authentication entry points and access-denied handlers now emit fixed
ACCESS_AUTHENTICATION_REQUIRED and ACCESS_DENIED records through AccessAuditService.
The latter includes CSRF rejection, not only role denial. No URI, query, principal,
request header, cookie, password, token or exception enters this service. HTTP401/403
responses remain empty; successful requests emit neither event. Rejection does not
invalidate a valid session or expand role permissions. Unit/API, real-container and
frontend recovery tests plus a real-backend browser scenario verify these boundaries.
These records are application logs, without actor correlation, durable storage or
retention/access/volume controls. They do not cover controller-generated401 responses,
all application errors or account mutations. Full identity lifecycle, ownership,
revocation/reset and enterprise readiness remain unfinished. Validation is recorded in PR22.

### November 17 — Disable saved-request caching (PR22, unmerged)
The API-driven login uses204 responses and explicit React session loading, not redirects.
The security chain now explicitly uses NullRequestCache so rejected URLs/query strings
are not saved for later replay and anonymous rejected GETs do not create sessions merely
for request caching. CSRF and authenticated server sessions remain enabled. This does not
prevent URLs appearing in browser history or infrastructure access logs; do not put secrets
in URLs. Unit/API, embedded-server, frontend unit/integration and real-backend browser tests
cover absent request-cache sessions, preserved CSRF/login, no redirect/replay and logout.
All authentication lifecycle/ownership/reset and operational gates remain required before
merge; exact validation results are recorded in PR22.
