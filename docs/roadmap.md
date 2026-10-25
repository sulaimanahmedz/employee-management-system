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
