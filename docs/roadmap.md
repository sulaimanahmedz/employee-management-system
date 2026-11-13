# Product roadmap

The goal is an employee management application spanning the employee lifecycle.

## Foundation (this initial codebase)

- React/TypeScript application shell and responsive employee directory.
- Spring Boot feature structure, employee API, validation and local persistence.
- Integration tests, build workflow and development documentation.

## Identity and organization

- Next requested product feature (October 25): full authentication and role-based access. See [authentication delivery plan](authentication-plan.md). Complete pending validation foundations first; no unrelated product feature takes priority.
- Sign-in and role-based permissions for administrators, HR, managers and employees.
- Departments, locations, teams and reporting hierarchy.
- Auditing, PostgreSQL and versioned database migrations.

## Employee lifecycle

- Implemented in source: profile viewing and editing (October 21, 2026); execution validation pending.
- Implemented in source: server-side search, status filtering and pagination (October 24, 2026); execution validation pending.
- Implemented in source: document metadata register (October 24, 2026); validation status is recorded in its feature PR.
- Implemented in source: recorded employment history (October 24, 2026); validation results are recorded in its feature PR.
- Implemented in source: onboarding checklist with owner labels; validation recorded in its feature PR.
- Next: identity-based task assignment, onboarding templates and offboarding workflows.
- Employee self-service and manager views.

## Time and operations

- Attendance records, leave balances and approval workflows.
- Holiday calendars, announcements and notifications.
- Reports and exports governed by role permissions.

## Delivery readiness

- Frontend component and end-to-end tests, accessibility verification.
- Deployment environments, backups, monitoring and recovery procedures.
- Payroll integrations only after requirements and access policies are defined.

## Completed increments

### October 21, 2026 — Employee profiles
- Open an employee from the directory and retrieve their profile from the backend.
- Edit all current profile fields and update the directory and summary counts after saving.
- Preserve drafts after errors, confirm discarding edits, and prevent duplicate save submissions.
- Added backend test source for profile persistence, invalid updates, duplicate-email rollback, unchanged email and deleted employees.
- Validation: static source review only. No dependency downloads, builds, compilation, tests or application startup were performed for this increment.

### October 24, 2026 — Searchable, paginated directory
- Added a bounded server-side directory endpoint with literal substring search, optional status filtering and stable ID ordering.
- Added Apply/Clear filters, page size selection, Previous/Next controls and matching result counts.
- Kept organization totals independent of filters and refreshed directory data after employee changes.
- Preserved the original list endpoint for compatibility.
- Added integration test source and browser acceptance instructions.
- Validation: static source review only; no downloads, builds, compilation, tests or application runs.

### October 24, 2026 — Employee document metadata
- Added per-employee document names, categories and optional expiry dates with list/add/remove UI.
- Added scoped REST endpoints, input validation, missing-record handling and deletion cleanup.
- Added integration tests for metadata lifecycle, cross-employee isolation, invalid input and employee deletion.
- File uploads and document contents remain out of scope; authentication and authorization remain prerequisites for production use.
- Execution validation is recorded in the feature pull request; do not consider the feature validated until those checks pass.

### October 24, 2026 — Recorded employment history
- Added transactional snapshots for profile creation and department/job-title/status changes, with explicit baselines for existing profiles.
- Added a bounded, employee-scoped history endpoint and a read-only profile timeline with pagination, retry and empty states.
- Added tests for normalization/no-op saves, baseline semantics, rollback, scoping, pagination and deletion cleanup.
- Builds, tests and runtime checks are recorded against the feature commit in its pull request before merge.
- Effective dates, actor attribution, retained audit records and manual historical imports remain future work.

### October 24, 2026 — Onboarding checklist
- Added employee-scoped tasks with title, owner label, optional due date, completion and reopening.
- Added paginated profile checklist and progress totals; confirmed removal and loading/error/retry states.
- Added tests for scoping, idempotent completion, validation, pagination and deletion cleanup.
- Validation commands and exact commit SHAs are recorded in the feature PR before merge.
- Identity-based assignments, templates, reminders and offboarding remain future work.

### October 24, 2026 — Edit onboarding task details
- Added scoped task-detail updates and profile edit/cancel controls, retaining drafts on failures and completion state on successful edits.
- Clearing an optional due date is supported; validation and missing/cross-employee records follow existing API conventions.
- Serialized detail/completion updates with a task-row lock to preserve independent fields.
- Regression tests and execution results are recorded in the feature PR before merge.

### October 24, 2026 — Onboarding task filters
- Added server-side pending/completed filtering before pagination and profile filter controls.
- Kept overall checklist progress independent of filters and refreshed page position after mutations.
- Added regression tests for filtered pagination, cross-employee isolation, global totals and invalid filter values.
- Validation commands and exact source commit are recorded in the feature PR before merge.

### October 24, 2026 — Leave request register
- Added employee-scoped annual/personal/unpaid full-day requests, inclusive date validation, pending-overlap prevention and idempotent cancellation.
- Added paginated profile history, confirmation before cancellation and draft retention after failures.
- Added API tests for calendar-day semantics, overlap boundaries, cancellation/re-request, scoping, pagination, invalid input and employee deletion.
- Execution validation commands/results and head/base SHAs are recorded in the feature PR before merge.
- Next: authenticated approval roles, entitlement balances, organization holiday rules and half-day policies; no policy assumptions or status automation introduced.

### October 24, 2026 — Leave status and period filters
- Added combined status and inclusive date-overlap filtering before pagination, scoped to the employee.
- Added Apply/Clear controls, applied-filter summary and matching empty states; mutations preserve applied filters.
- Added regressions for boundary overlap, open-ended periods, filtered paging, isolation and invalid filters.
- Execution validation results and commit SHAs are recorded in the feature PR before merge.

### October 24, 2026 — Edit document metadata
- Added scoped metadata updates and profile Edit/Save/Cancel controls; clearing expiry is supported.
- Retained drafts after failures and confirmed discarding changed metadata.
- Added regression tests for persisted updates, invalid input, cross-employee isolation and missing records.
- Builds, tests and runtime checks are recorded against the feature commit in its PR before merge.

### October 24, 2026 — Paginated document register
- Added a bounded document-page endpoint and profile Previous/Next navigation with totals.
- Preserved the legacy list API and edit drafts; refreshes after mutations recover from empty last pages.
- Added API tests for ordering, page boundaries, isolation, invalid pages and compatibility.
- Build, test and runtime validation results are recorded in the feature PR before merge.

### October 24, 2026 — Document renewal review
- Added an inclusive expiry cutoff filter before document pagination and matching profile Apply/Clear controls.
- Excluded undated records from filtered results, retained current filters after mutations and displayed the applied cutoff.
- Added regression coverage for cutoff boundaries, undated records, filtered pagination, isolation and invalid dates.
- Execution checks and exact commit SHAs are recorded in the feature PR before merge.

### October 24, 2026 — Onboarding deadline review
- Added inclusive due-date filtering combined with completion state before pagination.
- Added profile cutoff/Clear controls while keeping overall progress independent of filters.
- Added tests for boundaries, undated exclusions, combined filters, pagination, isolation and invalid dates.
- Execution validation and exact head/base SHAs are recorded in the feature PR before merge.

### October 25, 2026 — Document category review
- Added category filtering combined with expiry review before pagination and matching Apply/Clear controls.
- Preserved undated records for category-only queries and displayed applied categories.
- Added tests for combined filters, pagination, isolation, compatibility and invalid category values.
- Execution validation and exact head/base SHAs are recorded in the feature PR before merge.

### October 25, 2026 — Leave type review
- Added leave-type filtering combined with status and date period before pagination.
- Added an Apply/Clear-compatible type selector and applied-type summary.
- Added regressions for combined filtering, pagination, cancellation, isolation and invalid types.
- Execution validation and exact commit SHAs are recorded in the feature PR before merge.

### October 25, 2026 — Backend package and responsibility refactor
- Split employee, document, leave, onboarding and employment history into feature packages with controller/service/repository/entity/dto layers.
- Removed entity dependencies on HTTP DTOs; extracted employment history controller/service and the onboarding completion DTO.
- Preserved history writes in the employee transaction, database mappings and HTTP contracts; regrouped existing regression tests by feature.
- Added package/dependency architecture checks and persistent coding standards in AGENTS.md.
- Actual build, regression and runtime integration results with exact head/base SHAs are recorded in the refactor PR before merge.

### October 25, 2026 — Correct pending leave requests
- Added scoped pending-request editing for type and dates, preserving record identity and status.
- Serialized edits with creation/cancellation and excluded the edited record from inclusive overlap checks; cancelled records cannot be changed.
- Added UI edit/save/cancel with draft retention, discard confirmation and active-filter refresh.
- Added regressions for unchanged/self-overlapping edits, collisions and rollback, cancellation, adjacent dates, employee scoping and invalid input.
- Package/layer review, builds, tests and runtime results are recorded against exact head/base SHAs in the feature PR before merge.

### October 25, 2026 — Sortable employee directory
- Added allowlisted server-side sorting by ID, name, department and job title in both directions before pagination.
- Added case-insensitive text ordering with stable ID ties, preserving search/status filters and global totals.
- Added draft/apply/clear sort controls and applied order summary; profile return preserves order.
- Added regression coverage for ordering, page boundaries, equal values, combined filters, empty results and unsupported sort parameters.
- Package/layer review, builds, tests and runtime checks are recorded against exact feature/base SHAs in the PR before merge.

### October 25, 2026 — Employment history event filters
- Added employee-scoped event-kind filtering before pagination and a profile selector with matching counts/empty states.
- Preserved newest-first ordering and original unfiltered behavior.
- Added regressions for filtered pagination, scoping, invalid kinds and legacy baselines.
- Package review, build/test/runtime checks and validated SHAs are recorded in the feature PR before merge.

### October 25, 2026 — Document name search
- Added bounded, literal case-insensitive name search combined with document category/expiry filters before pagination.
- Added Apply/Clear search input and applied text summary, preserving existing edit drafts and filtered refresh behavior.
- Added pagination, scoping, combined-filter, wildcard and length regression coverage.
- Package/layer review and build/test/runtime results are recorded with exact SHAs in the feature PR.

### October 25, 2026 — Test foundations (pending validation gates)
- Added backend unit tests, JaCoCo reports and 100% line/branch gate.
- Added frontend unit/component integration tests, Vitest coverage reports and 100% thresholds.
- Added Playwright real-backend onboarding lifecycle coverage with synthetic database and cleanup.
- This branch must remain unmerged until coverage and E2E gates pass; see its PR for actual results.
- Task-title/owner search was not committed in the previous workspace and remains future work.

### Enterprise readiness priorities
- [ ] Close inherited frontend/backend coverage gaps to 100%.
- [ ] Authentication and role-based authorization with negative access tests.
- [ ] Production database migrations and database integration tests.
- [ ] Actor-attributed auditability and secure data handling.
- [ ] Concurrency, observability, backup/restore and operational recovery tests.
The current unauthenticated development application is not enterprise-ready.

### October 25, 2026 — Responsive document actions and organized tests (feature branch)
- Moved document action buttons below names and metadata, with wrapping long names and touch-sized controls.
- Improved narrow layouts, form input sizing, wrapping navigation and contained table scrolling.
- Fixed sibling key collisions that duplicated profile sections after edits.
- Separated backend unit/API/integration tests and frontend unit/integration tests; grouped E2E by business feature with shared helpers.
- Four distinct Playwright scenarios execute across five viewport profiles (20 runs). Exact validation and coverage blockers are recorded in the PR.
- A narrated walkthrough previews these unmerged changes; it does not demonstrate actual file upload.

### October 25 — Employment history validation (PR19 pending)
- Added unit coverage for loading, filtering, paging, error recovery and abandoned requests; added HTTP-client integration coverage.
- EmploymentHistory.tsx now measures100% line/branch/function coverage; full-project frontend coverage still fails the unchanged gate.
- Real-backend history scenario passes at five Chromium viewport widths. Backend57 tests and100% line/branch coverage pass. Exact head/base and commands are recorded in PR19; no product behavior changed or merge performed.

### October 25 — Profile validation (PR19 pending)
- Added11 profile unit cases, HTTP conflict/draft-retention integration coverage and a real-browser conflict/recovery scenario at five widths.
- Profile component now measures100% line/branch/function coverage. Full frontend line coverage65.16%; strict project gate still fails.
- Backend57 tests and100% line/branch coverage pass; three E2E scenarios/11 viewport executions pass. Existing sibling-key fix stays on PR20; no production code changed or merge performed.

### October 25 — Leave request validation (PR19 pending)
- Added 11 unit cases, a real-client HTTP conflict integration case, and a real-backend browser lifecycle/overlap scenario at five widths.
- Leave component now measures 100% lines and 98.31% branches. Frontend 41 tests pass; full-project coverage still fails the unchanged gate (71.72% lines, 87.27% branches).
- Backend 57 tests pass with 100% line/branch coverage. No production change or merge; authentication remains next after validation foundations.

### October 25 — Application startup and directory validation (PR19 pending)
- Added six directory unit cases, actual React entry-point/HTTP recovery integration coverage, and a real-backend directory scenario at five viewport widths.
- App.tsx and main.tsx now have 100% line/branch coverage. All 48 frontend tests pass; full frontend coverage remains below the unchanged gate (91.66% lines,90.61% branches).
- Backend57 tests and100% line/branch coverage pass. Document/onboarding coverage and two leave guards remain; no production behavior changed or merge performed. Authentication stays next after foundations.

### October 25 — Document register validation (PR19 pending)
- Added 12 document unit cases, real-client validation/retry integration coverage and a real-backend filtering/edit/removal scenario at five widths.
- Document component now measures100% line/branch/function coverage. All61 frontend tests pass; full frontend coverage remains below the unchanged gate (97.67% lines,95.48% branches).
- Backend57 tests and100% line/branch coverage pass. Onboarding and two leave guards remain. No production change or merge; authentication remains the next product feature.

### October 26 — Onboarding validation (PR19 pending)
- Added 11 onboarding unit cases, an HTTP validation/retry integration case and a real-backend deadline/completion/removal browser scenario at five viewport widths.
- Onboarding now measures 100% line/branch/function coverage; all 73 frontend tests pass. Full frontend coverage is 100% lines and 99.64% branches, leaving two leave-request pending guards uncovered.
- Backend 57 tests and 100% line/branch coverage pass. No production changes or merge; authentication remains next after validation foundations. Exact browser results and validated SHAs are recorded in PR19.

### November 4 — Close leave save-race coverage gaps
- Added two unit regressions proving that simultaneous edit/cancel actions cannot replace an in-flight leave draft before busy controls render, including retention after a failed save.
- All 75 frontend tests pass with full 100% line/branch/function coverage; no production guard, exclusion or threshold was changed.
- Full build/backend/E2E and merge results are recorded against the resulting commit in PR19. PR20 follows in dependency order; authentication remains the next product feature once foundations pass.

### November 4 — Integrate validated foundations and fix document filter spacing
- PR19 merged as `a79e323251b35e5c36cf9bb91a1ca95aacea4ef6` with 100% frontend/backend line and branch coverage, 75 frontend tests, 57 backend tests and 31 browser executions passing.
- Integrated that main into PR20, preserving both roadmap histories and moving all frontend tests into tests/unit or tests/integration. Six feature groups now contain ten E2E scenarios, each run once by five viewport projects (50 executions).
- Addressed issue #21 with a dedicated document filter-action row: 12px spacing, at least 44px controls, equal full-width phone buttons. Added geometric browser assertions alongside long-name wrapping/edit/remove coverage.
- PR20 requires fresh builds, coverage, architecture, real-backend browser and isolated Faker startup checks against the integrated tree before merge. Final commands/results and SHAs are recorded in PR20. Authentication remains next; the application is not enterprise-ready.

### November 5 — Authentication credential foundation (unmerged)
- Added the identity feature with immutable account persistence, normalized unique logins,
  four stored roles, salted BCrypt cost-12 hashing and credential-free identity projection.
- Added focused unit and database integration tests for valid/invalid credentials,
  missing accounts, Unicode/byte limits, duplicate logins and role round trips.
- No public account endpoint, session, bootstrap credential or authorization is enabled.
  Existing APIs remain unauthenticated. Continue this branch with secured sessions,
  protected provisioning, scoped permissions and React flows before merge.
- Build, coverage and existing browser regression results are recorded in the PR;
  authentication-specific API/frontend/E2E gates remain pending. This is not a completed
  authentication feature or enterprise readiness claim.

### November 6 — Authenticated sessions and initial role gate (PR22, unmerged)
- Added Spring Security login/logout, server sessions, CSRF, secure cookie defaults,
  session identity and protected ADMIN account provisioning with explicit empty-store bootstrap.
- Added React login/retry/logout/expiry/access-denied flows. HR can access employee APIs;
  ADMIN has no HR bypass, and MANAGER/EMPLOYEE business access is denied pending linkage.
- Added unit/API/integration and real-backend browser scenarios; authenticated existing
  business fixtures without disabling security. Full validation results are recorded in PR22.
- Adapted Java Faker presentation startup to disposable authenticated admin/HR sessions.
- Login abuse/audit, scoped ownership, account lifecycle/revocation/reset and their full
  acceptance tests remain merge gates. No merge or enterprise readiness is claimed.
- Fixed presentation startup immediately after browser teardown: port preflight permits
  closed TIME_WAIT sockets while still refusing live listeners; failures retain launcher diagnostics.

### November 7 — Login abuse admission controls (PR22, unmerged)
- Added five-attempt/60-second per-login admission, a 16-check concurrency cap and a
  1,024-key memory bound. Fresh sessions do not reset limits; successful login resets its key.
- Kept generic 401 responses and added neutral retry guidance to the login form.
- Added unit concurrency/capacity/expiry tests, real credential/persistence integration,
  API session-isolation checks and a browser throttle scenario at five viewport widths.
- Distributed controls and safe audit, ownership, account lifecycle/revocation and reset
  gates remain pending. Validation commands, coverage and source SHAs are recorded in PR22.

### November 8 — Credential cleanup and session recovery (PR22, unmerged)
- Clear the transient password after failed CSRF retrieval or login transport, as well as
  after HTTP rejection. Users explicitly re-enter credentials when connectivity returns.
- Add frontend unit/integration and real-backend browser connection-loss/recovery coverage.
- Add real-container inactivity expiry coverage: verify the 30-minute default, shorten only
  one synthetic test session, reject expired authentication/CSRF and permit fresh login.
- Add a two-tab browser logout/re-authentication scenario at all five viewport widths.
  Stale content disappears on the next API request; immediate cross-tab notification is not implemented.
- No backend production configuration or permission changes. Audit, scoped ownership,
  account lifecycle/revocation and reset remain required before merging authentication.
  Actual validation results and source SHAs are recorded in PR22.

### November 9 — Cross-tab sign-out (PR22, unmerged)
- Successful server logout now sends an opaque same-origin notification so other tabs
  remove their workspace without waiting for a business API request.
- Late session-check responses/errors cannot restore identity or replace sign-out feedback.
- Disabled storage does not turn a successful server logout into a UI failure; API 401
  recovery remains the fallback. Failed server logout does not publish a notification.
- Added unit, frontend integration and real-backend browser coverage, including storage
  failure. No backend permission/session configuration changes or dependency upgrades.
- Full validation and exact source/base SHAs are recorded in PR22. Audit, ownership,
  account lifecycle/revocation and reset remain authentication merge blockers.

### November 10 — Administrative account creation screen (PR22, unmerged)
- ADMIN now has a responsive account-creation screen using the existing protected API,
  with EMPLOYEE as the default role and explicit role selection.
- Show server validation/conflict feedback, retain non-secret inputs after failure,
  clear passwords after every submission and prevent duplicate pending submissions.
- Add unit/frontend integration, assigned-role backend API and real-backend browser tests
  for creation, short-password rejection, duplicates, new-account login and denied escalation.
- No public registration, employee-data bypass, invitations or external messages.
  Account editing/deactivation, password changes, reset, auditing and scoped ownership
  remain unfinished; exact validation and source SHAs are recorded in PR22.

### November 11 — Complete provisioning role acceptance (PR22, unmerged)
- Added browser provisioning/login cases for ADMIN, HR and MANAGER, complementing the
  existing EMPLOYEE scenario. Assert the single assigned authority, permitted workspace,
  employee-route boundaries and denied administrative escalation with valid CSRF.
- Added explicit least-privilege role-selection unit cases and expired-session provisioning
  integration coverage that removes administration and credentials.
- No production behavior or dependencies changed. This completes additional acceptance
  coverage for the creation screen; account lifecycle, audit, ownership and reset remain pending.
- November 10 validation and fresh committed-source results are recorded in PR22.

### November 12 — Administrative account directory (PR22, unmerged)
- Add ADMIN-only, ten-row paginated account review (ID/login/role), stable ID ordering
  and invalid-offset rejection. No credential fields are serialized.
- Add an on-demand responsive directory, Previous/Next and refresh, with stale-result
  removal on failure and session-expiry cleanup through the shared client.
- Add feature-owned unit/API/persistence/frontend integration and real-backend E2E
  tests for pagination, permission boundaries, invalid pages and connectivity recovery.
- Validation commands, exact committed source and coverage results are recorded in PR22.
  Auditing, scoped ownership, account mutation/revocation and password/reset lifecycle
  remain required; this increment does not complete authentication or enable deployment.

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
