# Test foundation validation — October 25, 2026

Status: **unmerged; coverage gates fail**. Source and base commit IDs are recorded in PR #19. No production behavior was modified.

## Executed checks
- Dependency installation added pinned test tools; existing locked package versions were compared and did not change.
- `npm run build --prefix frontend`: TypeScript compilation (including test configurations and E2E source) and Vite production build passed. An initial Testing Library option typing error was corrected before the final build.
- `npm run test:coverage --prefix frontend`: 7 tests passed; command failed the coverage gate. Full-source line/statement coverage 13.93%, branch coverage 59.42%, function coverage 30.76%. Shared HTTP client has 100% coverage; this is not full-application coverage.
- `mvn -f backend/pom.xml -s /tmp/ems-maven-settings.xml --batch-mode clean verify` using Maven 3.9.9: compilation, packaging and 55 tests (including 2 architecture checks) passed. Verification failed the JaCoCo gate: 333/335 lines (99.40%) and 99/100 branches (99%). Runtime proxy settings are environment-specific and are not committed.
- `cd frontend && npm exec -- playwright install chromium`: Chromium 141.0.7390.37 / Playwright build 1194 installed.
- `npm run test:e2e --prefix frontend`: 1 Chromium E2E passed against packaged Spring Boot and Vite with in-memory synthetic H2. Backend health, browser UI, Vite/API integration, task validation/create/edit/completion/filtering and persisted results were checked. Synthetic employee cleanup passed; ports 8080/5173 were confirmed closed after servers stopped.
- `git diff --check`: passed.

## Remaining coverage work
Backend: application bootstrap lines 9–10 and one EmployeeService employment-change predicate branch at line 98 are uncovered.
Frontend: directory/app bootstrap, employee profile, documents, leave, employment history and employee API helpers lack coverage; onboarding has partial coverage (83.16% lines, 57.14% branches). Extend meaningful unit and integration tests rather than excluding these paths or relaxing thresholds.

Reproduce reports at `backend/target/site/jacoco/index.html` and `jacoco.xml`, and `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`. These generated reports are intentionally not committed. Playwright retains failure traces/screenshots under `frontend/test-results/` when tests fail.

## Architecture review
Tests remain in their owning feature; backend unit tests exercise domain behavior without a Spring context. Existing API integration and package/dependency checks remain intact. Frontend component tests use the real HTTP client with simulated HTTP responses; the Playwright suite separately uses the real backend. No entities, controllers, services, transactions, schema or production dependencies changed. GitHub Actions remains manual-only.

The repository is not enterprise-ready. Authentication/authorization, production migrations, actor-attributed auditing, security, concurrency and operational recovery remain roadmap requirements. This PR must remain unmerged until the coverage gates pass.

## October 25 follow-up: close API helper and status-change gaps
- Added three frontend API helper unit tests: scoped routes, payloads, AbortSignal forwarding, encoded search/sort/paging and optional status/history filters.
- Added an API integration regression proving that a status-only employment update records exactly one change and a repeated unchanged status records none.
- Frontend build passed; 10 frontend tests passed. Full-project coverage: 16.66% lines/statements, 64.93% branches, 45.16% functions; both API helper modules now have 100% coverage.
- Backend: 56 tests passed, 333/335 lines (99.40%) and 100/100 branches (100%). JaCoCo verification still fails on bootstrap lines 9–10. No coverage thresholds were changed.
- A real-browser walkthrough exposed a separate profile issue: related sections can repeat after saving a profile. EmployeeProfile gives each sibling feature component the same key; a regression and fix are needed on a bounded follow-up. Reopening the profile refreshes the sections. This run does not alter production behavior.
- PR remains unmerged. E2E revalidation and exact source/base SHAs are recorded in PR #19.
# October 25 — real entry-point startup coverage

Added `application/integration/ApplicationStartupIntegrationTest`: invokes the actual main method, starts the embedded server on a random loopback port with a unique in-memory database, asserts health UP, creates a synthetic employee over HTTP and retrieves persisted fields. A test-only Spring initializer captures the context for guaranteed shutdown in finally. It is absent from the packaged application. No production source or architecture rules changed.

Validation on parent `85e2ab106350ad0e1147fb55bca0e22486b32c5e` plus this increment, based on main `511f2a6d6b637e5068dae89b003616e1e2b24d74` (resulting head recorded in PR):
- `/tmp/apache-maven-3.9.9/bin/mvn -o -f backend/pom.xml --batch-mode clean verify`: PASS, 57 tests including architecture checks. JaCoCo: 335/335 lines, 100/100 branches, 165/165 methods. Reports `backend/target/site/jacoco/{index.html,jacoco.xml}`.
- `npm run build --prefix frontend`: PASS TypeScript and Vite.
- `npm run test:coverage --prefix frontend`: 10 tests PASS; coverage gate FAIL: 16.66% lines/statements, 64.93% branches, 45.16% functions. Reports `frontend/coverage/`. Remaining gaps include App/main, profile, documents, history, leave and partial onboarding. PR remains unmerged.
- `npm run test:e2e --prefix frontend`: PASS, 1 existing onboarding scenario / 1 Chromium execution against real UI and isolated backend. No new product behavior added in this increment; the five-viewport suite organization remains in dependent PR #20. Temporary servers stopped.
- Initial startup test failed because the old property-based initializer registration was ignored; replaced with test-only `META-INF/spring.factories` registration and reran clean verify successfully. No mocked bootstrap or coverage exclusions.

Authentication with roles is the next requested product feature; see `docs/authentication-plan.md`. This does not claim authentication is implemented.

## Employment history coverage increment — October 25

Resumed PR19 at parent `2b12fc54a4d9e32a9d0ac0285dc77f8e425ddae7`; latest main `511f2a6d6b637e5068dae89b003616e1e2b24d74` is already an ancestor and was rechecked unchanged. Resulting validated head/tree are recorded in the PR.

- Added six component unit cases under `frontend/tests/unit/employmenthistory/`: loading, both empty states, event/status display, pagination and filter reset, typed/untyped error retry, and late success/failure from aborted employee requests.
- Added one HTTP-client integration case under `frontend/tests/integration/employmenthistory/`: error mapping, retry and exact filtered/paginated request contract.
- Added a named real-backend Employment history E2E suite: persisted change display, filtered empty/results states and invalid-kind rejection, executed at320/390/412/768/1440px Chromium widths with isolated employee cleanup. This is one distinct scenario executed five times; including existing onboarding, two distinct scenarios/six executions passed. This checks functional use at those widths, not visual perfection or all physical devices.
- `npm ci --prefix frontend`: installed locked dependencies without lockfile changes. `npm run build --prefix frontend`: PASS.
- `npm run test:coverage --prefix frontend`:17 tests PASS. EmploymentHistory.tsx:100% lines,branches,functions,statements. Full-project:23.22% lines/statements,75.23% branches,54.28% functions; strict coverage gate FAIL. No exclusions/thresholds weakened. Reports:frontend/coverage.
- Maven3.9.9 `-s /tmp/history-settings.xml -f backend/pom.xml --batch-mode clean verify`: PASS,57 tests including architecture;335/335 lines and100/100 branches. Initial offline attempt found no cached dependencies; downloaded pinned dependencies using environment-specific proxy settings and reran successfully. Reports:backend/target/site/jacoco.
- `npm exec -- playwright install chromium`, then `npm run test:e2e --prefix frontend`: PASS,6 executions in20.5s against packaged backend and Vite proxy; temporary servers stopped.
- Initial integration assertion omitted the existing size=10 parameter; corrected the assertion to the actual contract and reran successfully.

No production code, package/layer boundaries, transactions or schema changed. New tests use the required feature/unit and feature/integration grouping; broader existing-test reorganization remains in dependent PR20. Authentication remains the next product feature after these foundations pass. PR19 must remain unmerged while App/main, profile, documents, leave and partial onboarding coverage gaps remain.

## Employee profile validation increment — October 25

Resumed PR19 at `638bc41c679940d48ee697448740b78add1c98ad`. Latest main `511f2a6d6b637e5068dae89b003616e1e2b24d74` is already an ancestor; both remote heads were rechecked unchanged before committing. Resulting head/tree recorded in PR19.

- Added11 component unit cases in tests/unit/employees covering view/focus/back navigation, unchanged and dirty cancellation, discard confirmation, beforeunload cleanup, duplicate-submit prevention, pending-save controls, normalized successful saves, typed/untyped errors with retry, and aborted stale success/failure.
- Added one tests/integration/employees case using actual feature/HTTP helpers and related components to verify submitted JSON and draft retention after409.
- Added one distinct profile editing/conflict-recovery E2E scenario, executed at320/390/412/768/1440px Chromium widths, with synthetic fixtures under e2e/support. Conflicting email updates preserve stored fields; corrected data persists. Cleanup removes both employees.
- `npm ci --prefix frontend`: pinned lockfile install; no package/lock changes.
- `npm run build --prefix frontend`:PASS after correcting a test-only unsupported exact option to an anchored role-name matcher.
- `npm run test:coverage --prefix frontend`:29 tests PASS; EmployeeProfile.tsx100% lines/branches/functions/statements. Full project65.16% lines/statements,76.59% branches,42.10% functions; gate FAIL. Loading real related components adds their runtime function counters to the denominator, so function percentages are not comparable to component-mocked results alone. Reports frontend/coverage. No thresholds or exclusions changed.
- Maven3.9.9 `-s /tmp/profile-settings.xml -f backend/pom.xml --batch-mode clean verify`:PASS57 tests including architecture,335/335 lines and100/100 branches. Environment proxy settings only; pinned dependencies downloaded. Reports backend/target/site/jacoco.
- `npm exec -- playwright install chromium`; `npm run test:e2e --prefix frontend`:PASS11 executions in39.5s. Three distinct business scenarios: history five widths, profile editing five widths, onboarding once. Real packaged backend, isolated H2 and Vite proxy; ports8080/5173 stopped. These are functional Chromium viewport checks, not universal device/browser certification.
- React reports the known duplicate sibling keys in EmployeeProfile. This increment does not suppress the warning or duplicate the fix already pending in PR20. Passing targeted tests does not resolve that known issue.

No production code, package/layer boundaries, transactions or schema changed. PR19 remains unmerged: App/main and partial document/leave/onboarding coverage gaps remain, as does the separate pending profile-key fix. Authentication/roles remain the next product feature after validation foundations.
