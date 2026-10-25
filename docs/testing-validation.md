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
