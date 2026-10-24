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

There is no authentication, payroll, leave approval, attendance tracking, audit history, or production hosting in this base. The UI exposes list/create and a dedicated view/edit profile screen using the existing GET/PUT endpoints; deletion is currently API-only. Profile data is fetched when opened. A successful update invalidates the directory; returning fetches the applied search page and organization totals again. Failed saves retain the draft. Requests abandoned while loading a profile are cancelled and ignored. Concurrent edits from different users still follow last-write-wins; optimistic locking is a future improvement.

## Directory queries

The UI uses `GET /api/v1/employees/directory`, a dedicated DTO with a bounded employee page, matching counts and separate global aggregates. A JPA Specification combines literal case-insensitive substring search with status equality. Criteria `locate` avoids treating SQL wildcard characters as patterns. Page requests sort by ID and validate size/offset before repository access. No extra dependency or schema change is required.

The original list endpoint remains available for existing callers, but is no longer used by the UI. Filter submission resets page zero. Aborted requests cannot replace newer directory state. Empty pages after deletion or changed filter membership trigger a fetch of the last valid page. Database search and aggregate queries can observe concurrent changes at the configured isolation level; pagination is not a frozen snapshot. Substring scans and aggregate counts still need performance review before large-scale production use.

## Recorded employment history

EmployeeService delegates employment snapshots to EmploymentHistoryService in the same transaction as profile creation/update. Updates take a pessimistic write lock on the employee row to serialize the baseline and change snapshot decision. History does not add optimistic edit-conflict detection: profile updates still use last-write-wins. Read APIs return DTO pages sorted by descending history ID. Database foreign-key cascade removes snapshots on employee deletion. This is a profile history feature, not an immutable compliance audit trail.
