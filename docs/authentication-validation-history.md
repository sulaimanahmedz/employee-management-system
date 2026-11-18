# Historical PR22 validation records

Archived on November 17, 2026 before consolidating the PR description. These records describe earlier heads and must not be read as validation of later changes.

## November 17, 2026 — Scheduled employee-management development: disable saved-request caching

Continued feature/auth-accounts. API login returns204 and React loads session identity explicitly; it has no saved-request redirect/replay workflow. RequestCacheConfiguration now supplies NullRequestCache, explicitly injected into the security chain. Rejected URLs/query strings are no longer saved for replay and anonymous rejected GETs do not create a session merely for caching. CSRF, session fixation protection and authenticated sessions stay enabled. This does not remove URLs from browser history/infrastructure logs or scrub pre-existing sessions; no secrets should be placed in URLs.

Added backend unit/API checks for no cache-created session, no saved data/replay, preservation of unrelated session attributes and existing-CSRF-session login; real-container integration verifies no anonymous rejection cookie. Frontend unit/integration and real-backend browser coverage check login without replay/redirect and normal role/profile/logout behavior. README, architecture, roadmap and authentication plan updated.

Source: head `339edf0a0f644d9a7d0b12a2c2ea0ff33f362bbd`; tree `6bd373448473c19fdb59855cfed46d9f4225f2ed`; main/base `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`. GitHub/staged trees matched; latest main already incorporated. Feature commit only; expected-head update, no force push.

Committed-source validation:
- Maven3.9.9, Corretto JDK17 downloaded; `npm ci --prefix frontend`; `npx playwright install chromium`. Dependencies unchanged.
- `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: PASS, **100 tests**, zero failures/errors/skips. Runtime proxy settings generated from HTTPS_PROXY, TLS verification retained. Architecture passed.
- Full JaCoCo: **481/481 lines,150/150 branches=100%**,57 classes. New RequestCacheConfiguration2/2lines; changed IdentitySecurityConfiguration19/19lines; neither class contains instrumented branches (N/A).
- Frontend `npm run build` and `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS, **117 tests/28files**; full938/938lines,684/684branches=100%, functions/statements100%. No frontend production changes.
- Reports: backend/target/site/jacoco/{index.html,jacoco.xml}, backend/target/surefire-reports; frontend/coverage/{index.html,coverage-final.json,lcov.info}. Logs: /tmp/ems-nov17-{backend,front,install,browser}.log.
- `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e` from frontend: **PASS,155 executions in4.5m =31 distinct scenarios ×5 viewports** (320/390/412/768/1440), Chromium141.0.7390.37, Playwright1.56.1. Real React/Vite proxy and packaged Spring Boot used isolated synthetic in-memory H2 and randomized synthetic credentials. Backend health/startup passed. New no-cookie anonymous rejection, no-redirect login/no replay, role/profile and logout flow passed at all widths. No mocked backend. Chromium emulation only, not physical-device/cross-browser certification.
- Both temporary server ports8080/5173 were confirmed closed in the same execution after teardown; exit0. frontend/test-results/.last-run.json: passed/no failedTests; no failure-only screenshots/traces produced. Log: /tmp/ems-nov17-e2e.log. Final remote/local head and main/base matched the SHAs above; clean worktree. No merge.

Package review: infrastructure wiring stays in identity/service; no controller/entity/DTO/repository, role, transaction or locking changes. Feature-owned backend unit/api/integration, frontend unit/integration and e2e/identity suites retained; no weakened coverage/architecture gates. Actions remain manual-only.

**DRAFT / UNMERGED:** password/reset, account mutation/deactivation/revocation and employee/team ownership remain incomplete. Durable actor-attributed audit, MFA/SSO and operational readiness remain open. No deployment, external messages or real data.

---

## November 16, 2026 — Scheduled employee-management development: access rejection audit boundary

Continued feature/auth-accounts. Added AccessAuditService with fixed ACCESS_AUTHENTICATION_REQUIRED and ACCESS_DENIED codes from the security filter entry point/access-denied callbacks. ACCESS_DENIED includes CSRF rejection, not just role denial. Neither method accepts HTTP/request data or exceptions: no URI, query, principal, forwarded header, cookie, password or token is logged. HTTP401/403 bodies remain empty and successful requests do not generate these events. Existing sessions/permissions remain unchanged.

Tests cover fixed payloads/levels/no arguments or throwable, anonymous and CSRF rejection, all four roles' deny-by-default routes, allowed session reads, a real embedded-container HR session retaining its profile after denied administrative access, frontend generic error/retry, and a new real-backend browser rejection/privacy flow. README, architecture, authentication plan and roadmap updated.

Source: head `6c10a9d7a4b1a6c41f94ef87a1b55e4ef04a87b5`, tree `c791167a2483522041c74de7570759c07fb60d20`, base/main `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`. GitHub tree matched staged tree. Latest main incorporated (already up to date); feature-branch commit only, expected-head ref update without force.

Validation on committed source:
- Downloaded Maven3.9.9 and Corretto JDK17; `npm ci --prefix frontend`; pinned dependencies unchanged. `npx playwright install chromium` succeeded (automatic fallback download after a primary archive error).
- `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: PASS, **98 tests**, zero failures/errors/skips. Proxy settings generated from runtime HTTPS_PROXY; TLS verification retained. Architecture checks passed.
- Full JaCoCo: **478/478 lines,150/150 branches=100%**,56 classes. New AccessAuditService6/6lines; changed IdentitySecurityConfiguration18/18lines; these classes have no instrumented branches (N/A). No exclusions/threshold changes.
- Frontend `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS, **115 tests/27files**. Full coverage938/938lines,684/684branches=100%, functions/statements100%. No frontend production changes.
- Reports: backend/target/site/jacoco/{index.html,jacoco.xml}, backend/target/surefire-reports; frontend/coverage/{index.html,coverage-final.json,lcov.info}. Logs: /tmp/ems-nov16-backend.log, /tmp/ems-nov16-front.log, /tmp/ems-nov16-install.log, /tmp/ems-nov16-browser.log.
- `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e` from frontend: **PASS,150 executions in4.3m =30 distinct scenarios ×5 viewports** (320/390/412/768/1440), Chromium141.0.7390.37, Playwright1.56.1. Real React/Vite proxy plus packaged Spring Boot, isolated synthetic in-memory H2 and randomized synthetic credentials; no mocked backend. Health/startup succeeded. New anonymous/manager denial, empty-error privacy, preserved session/profile and logout browser flow passed at every viewport. Chromium emulation only, not physical-device or cross-browser certification.
- Both temporary server ports8080/5173 were confirmed closed in the same execution after teardown; command exit0. frontend/test-results/.last-run.json reports passed, no failedTests; no failure-only screenshot/trace artifacts produced. Log: /tmp/ems-nov16-e2e.log. Remote/local feature head and main/base rechecked after validation and match the SHAs above; worktree clean. No merge performed.

Package/layer review: fixed event formatting remains in identity/service; security configuration delegates without passing HTTP context. No controller/entity/DTO/repository or transaction/locking changes. Backend unit/api/integration and frontend unit/integration/e2e feature organization preserved; architecture checks unchanged. Actions remain manual-only.

**DRAFT / UNMERGED.** These are application logs, not durable/actor-attributed compliance records. Controller-generated401s, general application errors and account changes are outside this event scope. Log volume/access/retention/collection/failure controls remain open. Full password/reset, account mutation/deactivation, revocation and account/employee/team ownership remain merge blockers; MFA/SSO and enterprise readiness are unfinished. No deployment, real data or external messages.

---

### November 15, 2026 — scheduled employee-management development: session audit boundaries

Implemented and committed fixed-code AUTH_SESSION_ESTABLISHED and AUTH_LOGOUT_PROCESSED events through the existing audit service and security success callbacks. CSRF-rejected logout must retain the session and emit no logout-success event; valid anonymous logout is deliberately idempotent and records processing, not an authenticated actor or proven account revocation. New payloads contain no request values, account names, cookies, session IDs, tokens or exceptions. Unit, HTTP contract, real embedded-server and frontend recovery tests cover these boundaries; a real-backend browser scenario was added.

Committed head: `a8ba241ae46d16b4b21828db3bd8773585dca101`; tree: `71e41c74e9d1e560d0cc8282e065d0ea07211188`; main/base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`. Latest main incorporated (already up to date), no force push.

Validation on committed source:
- Dependencies: Maven 3.9.9, Corretto JDK 17, pinned npm dependencies via `npm ci --prefix frontend`; `npx playwright install chromium`. No dependency versions upgraded.
- Backend: `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify` passed. Runtime proxy settings generated from HTTPS_PROXY; TLS verification retained. 92 tests, zero failures/errors/skips, architecture checks passed. Full JaCoCo: 472/472 lines and 150/150 branches (100%). Changed production classes: AuthenticationAuditService 12/12 lines; IdentitySecurityConfiguration 18/18 lines; neither has instrumented branches (N/A, not a fabricated branch count).
- Frontend: `npm run build` and `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism` passed from frontend. 113 tests in 27 files. Full measured coverage 938/938 lines and 684/684 branches, with statements/functions also 100%. No frontend production changes.
- Reports: backend/target/site/jacoco/{index.html,jacoco.xml}, backend/target/surefire-reports; frontend/coverage/{index.html,coverage-final.json,lcov.info}. Logs: /tmp/ems-nov15-backend.log, /tmp/ems-nov15-front.log, /tmp/ems-nov15-install.log, /tmp/ems-nov15-browser.log.
- Real-backend Playwright: `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e` (frontend) passed: **145 executions, 29 distinct scenarios × 5 viewports**, 6.1 minutes, Chromium 141.0.7390.37 (Playwright 1.56.1), widths 320/390/412/768/1440. Packaged Spring Boot backend health and Vite startup succeeded; browser/API traffic used the actual frontend proxy and isolated in-memory synthetic H2 database, with randomly generated synthetic credentials. Successful logout, CSRF rejection preserving authenticated access and idempotent anonymous logout passed across every viewport. No mocked backend in E2E. Log: /tmp/ems-nov15-e2e.log; frontend/test-results/.last-run.json reports passed with no failedTests; failure-only screenshots/traces were not produced. This is Chromium emulation, not physical-device or cross-browser certification. Both temporary server ports 8080 and 5173 were verified closed in the same execution after the suite.
- Final remote/local head and main/base recheck matched the SHAs above; worktree clean. No merges. Initial pre-commit backend/frontend checks also passed; final results above were rerun on the committed source. No validation gate was weakened, skipped or represented as passed without execution.

Static/package review: changes remain in identity/service, configuration delegates event formatting to the service, no DTO/entity/repository dependency changes, no transaction/locking or permission changes. Backend unit/api/integration and frontend unit/integration identity structure retained; browser suite in e2e/identity. Architecture checks were not weakened. README, architecture, authentication plan and roadmap updated.

Disposition: DRAFT / UNMERGED. Logs are not durable or tamper-evident and lack actor/session correlation; expiry/authorization/account-change audit coverage, retention/access/volume/collection controls remain unresolved. Password change/reset, deactivation and role/account/password session revocation, account-to-employee/team linkage and scoped permissions still block full authentication completion. MFA/SSO and other enterprise readiness gates remain open. No deployment, external messages or real account data.

---

## November 14, 2026 — Scheduled employee-management development: authenticated account profile

**DRAFT / UNMERGED.** Continued the existing feature/auth-accounts branch. No duplicate feature or unrelated PR.

### Completed
Added a read-only My account view for ADMIN, HR, MANAGER and EMPLOYEE. GET /api/v1/auth/profile resolves the account exclusively from Authentication, not a caller-selected ID/login. Responses contain ID/login/assigned role only; never credentials or hashes. Query parameters cannot select another account. Anonymous and no-longer-stored identities receive401; arbitrary profile-ID routes remain denied and writes are unsupported.

The on-demand React component uses the shared client, disables its loading button, clears stale results on failure, permits refresh/retry, aborts on unmount and ignores late results. Session expiry/logout removes the account display. This exposes account identity only, not employee records or team scopes; existing business permissions remain unchanged. Read-only missing-account handling is not global session revocation.

Added feature-owned unit, API, persistence integration, frontend unit/integration and four named role-specific real-browser scenarios. README, architecture, identity plan and roadmap updated. No dependency/schema upgrades.

### Source
- Validated head: `aa455a52b822d86eb0e8335cd8acd141c05d519a`.
- Tree: `d46a4e325d532c0ad1db74298c44ccbd5af75ddc`; GitHub and local staged trees matched before commit.
- Main/base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. No force push or main commit.
- Remote head/main rechecked after validation: unchanged; worktree clean.

### Executed validation
- Downloaded Maven3.9.9 and Corretto JDK17; existing application dependencies remained pinned. Used runtime proxy and system truststore, with TLS verification enabled.
- Final committed source: `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: **PASS,91 tests**, zero failures/errors/skips. Includes architecture, API, service/persistence collaboration, embedded-server startup and session expiry.
- Full Java coverage: **468/468 lines,150/150 branches =100%**,55 classes. New AccountProfileController3/3lines (no branches); changed AccountService23/23lines,10/10branches; IdentitySecurityConfiguration18/18lines (no branches).
- Reports: `backend/target/site/jacoco/index.html`, `jacoco.xml`, `backend/target/surefire-reports/`.
- Frontend `npm ci`; final `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: **PASS,111 tests/26files**. Full instrumented frontend **938/938 lines,682/682 branches =100%**, functions/statements100%. New AccountProfile and changed AuthShell both100% line/branch coverage.
- Frontend reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`. No exclusions/threshold changes.
- `npx --prefix frontend playwright install chromium`: PASS.
- `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e`: **PASS,140 executions in5.1m =28 distinct scenarios ×5 viewport projects**. Chromium141.0.7390.37/build1194, Playwright1.56.1; widths320/390/412/768/1440. Chromium emulation only, not physical-device or Firefox/WebKit validation.
- Real React/Vite proxy and packaged Spring Boot with isolated synthetic H2/accounts. Four new role-specific flows passed: own profile, denied alternate-account routes, ignored account-selection parameters, connectivity failure/stale-result removal/retry, no horizontal overflow, logout removal and anonymous rejection. No mocked API replacement.
- Health startup and frontend/backend integration passed. Both temporary servers stopped; ports8080/5173 confirmed closed in the same execution after teardown, command exit0. `frontend/test-results/.last-run.json`: passed/no failed tests. No failure-only screenshot/trace artifacts produced.
- Logs: `/tmp/ems-nov14-backend.log`, `/tmp/ems-nov14-front.log`, `/tmp/ems-nov14-e2e.log`, `/tmp/ems-nov14-install.log`, `/tmp/ems-nov14-browser.log`. Preliminary build/tests also passed; final committed-source results above are authoritative. Static diff checks passed.

### Architecture and remaining blockers
AccountProfileController handles HTTP and delegates to the read-only AccountService lookup. DTOs separate persistence from transport; constructor injection and feature/layer organization retained. No cross-feature transaction or locking changes. Unit/API/integration and frontend/e2e tests remain in their owning feature suites. Architecture checks pass. Actions remain manual-only.

**No merge:** account mutation/deactivation, password/profile changes, revocation, reset lifecycle, scoped employee/team ownership and complete durable audit/operational infrastructure remain unfinished. MFA/SSO, migrations and enterprise readiness remain pending. Main remains unauthenticated. No deployment, real employee data or external messages.

---

## November 13, 2026 — Scheduled employee-management development: credential audit foundation

**DRAFT / UNMERGED.** Resumed feature/auth-accounts; no duplicate branch or unrelated feature.

### Changes and discovered defect
Added AuthenticationAuditService with fixed credential-verification event codes: accepted (stored numeric account ID only), rejected (including limiter rejection), and verification unavailable. No submitted login, password, token, header, exception message or stack is passed to this audit logger. Existing generic HTTP errors and credential erasure remain unchanged. These records describe credential verification, not proof of session establishment.

The first API audit test failed: **8 expected records,15 actual**. It exposed duplicate rejected verification through local and parent authentication managers, which also consumed login admission twice. Fixed the security chain with a dedicated ProviderManager and no parent fallback. API tests now assert one record per attempt and that four wrong passwords still permit a correct fifth attempt. The real-browser regression exercises that boundary too. No thresholds/assertions were weakened.

Added backend unit/API checks for exact payloads, limiter rejection, unexpected errors, original exception propagation and capacity release; extended real credential/persistence expiry recovery to verify audit outcomes. Added frontend unit/integration privacy/recovery tests and a named real-backend browser scenario. Documentation records limitations and architecture.

The first browser run on the manager-wiring fix had **115 passed,5 failed**: every new scenario stopped on Chromium's `Network.getResponseBody: No data found` when inspecting an empty401 fetch body. Corrected the test to inspect the same empty-body contract through a real APIResponse while retaining the complete browser rejection/four-failure/fifth-success flow; no API mocking or weakened contract. Initial failure screenshots/traces/error context were copied to `/tmp/ems-nov13-initial-browser-artifacts/`, with log `/tmp/ems-nov13-e2e-initial.log`. Early progress updates had overstated this scenario's status; this record gives the observed outcomes. Both servers were confirmed stopped after that failed run.

### Source
- Initial audit commit: `721907a0dd93c10c7922371dced446433f23a957`.
- Manager-wiring fix: `2c3211ca5f91c28f594c1550a8f385b3f728a184`.
- **Final validated head:** `6606baa1ea6ab901b7b3b13481163afc3fdc6894`.
- Final tree: `ea7120d81854e3be8b9d6c1d0c8170688deac7a9`; GitHub and local staged tree SHAs matched before commit.
- Main/base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. No force push or main commit.

### Executed validation
- Downloaded Maven3.9.9 and Corretto JDK17; pinned project dependencies unchanged. Runtime proxy/system truststore used without disabling TLS verification.
- Final committed source: `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: **PASS,88 tests**, zero failures/errors/skips. Includes architecture, actual embedded-server startup, persistence and session expiry.
- Full backend JaCoCo: **464/464 lines,150/150 branches =100%**,54 classes. New AuthenticationAuditService8/8lines, changed provider19/19lines, changed IdentitySecurityConfiguration18/18lines; these classes contain no JaCoCo branches. Reports: `backend/target/site/jacoco/index.html`, `jacoco.xml`, `backend/target/surefire-reports/`.
- Frontend `npm ci`, final committed-source `npm run build`, `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: **PASS,107 tests/24files**. Full frontend **906/906 lines,666/666 branches =100%**, functions/statements100%. No frontend production changes. Reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`. No exclusions or threshold changes.
- `npx --prefix frontend playwright install chromium`: PASS.
- Final committed source: `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e`: **PASS,120 executions in3.6m =24 distinct scenarios ×5 viewport projects**. Chromium141.0.7390.37/build1194, Playwright1.56.1; widths320/390/412/768/1440. Chromium emulation only, not physical devices or Firefox/WebKit.
- Real React/Vite proxy and packaged Spring Boot with isolated synthetic H2/accounts. Health startup, login rejection/recovery, corrected fifth-attempt boundary and existing business flows passed. The APIResponse contract assertion supplements, rather than replaces, the browser UI flow. No API mocking.
- Both temporary servers stopped; ports8080/5173 confirmed closed in the same execution after teardown; command exit0. `frontend/test-results/.last-run.json` reports passed/no failed tests. Final failure-only screenshots/traces produced none; initial failed-run artifacts remain at the separately recorded path.
- Logs: `/tmp/ems-nov13-backend-final.log`, `/tmp/ems-nov13-front-final.log`, `/tmp/ems-nov13-e2e-final.log`, `/tmp/ems-nov13-install.log`, `/tmp/ems-nov13-browser.log`. Initial failing backend run: `/tmp/ems-nov13-backend-pre.log`; fixed and fully revalidated rather than hidden.
- Remote head/main rechecked after final validation: unchanged; clean worktree. Static diff checks passed.

### Architecture, scope and merge blockers
Identity service constructor injection keeps HTTP strings and exceptions out of the audit interface. Provider admission/release semantics are preserved; no repository, schema, role or transaction/locking changes. Feature-owned unit/API/integration and frontend/e2e suites retained. Architecture checks pass; Actions remain manual-only.

**No merge:** these are application logs, not durable/tamper-evident audit storage. CSRF rejection happens before this provider and is not covered; logout, authorization and account-change events remain pending. Collection, retention/access controls, log-volume controls and failure monitoring still need implementation/verification. Rejected events intentionally cannot correlate a submitted identity. No audit retrieval endpoint is exposed.

Scoped ownership/linkage, account editing/deactivation, profile/password changes, revocation and reset lifecycle/full acceptance remain unfinished. MFA/SSO, migrations and operational readiness remain pending. Main remains unauthenticated. No deployment, real employee data or external messages.

---

## November 12, 2026 — Scheduled employee-management development: administrative account directory

**DRAFT / UNMERGED.** Continued feature/auth-accounts; no duplicate branch or unrelated feature.

### Changes
ADMIN can review account IDs, logins and roles through an on-demand responsive directory and GET /api/v1/admin/accounts?page=0. Ten-row pages use ascending unique IDs. The service rejects negative/overflowing offsets; response DTOs omit credentials. Anonymous clients receive 401; HR, MANAGER and EMPLOYEE receive 403. Previous/Next navigate; View accounts refreshes page zero. Failed reads clear stale results and permit retry. Requests abort on unmount; late results are ignored; shared-client 401 removes the authenticated workspace. Refresh is explicit after provisioning; pagination is not a concurrent snapshot.

Added focused backend unit, API and persistence integration tests; frontend unit/integration tests; one new named real-backend browser scenario for pagination, invalid pages, connectivity failure/recovery and layout fit. Extended EMPLOYEE browser coverage to reject directory reads. Updated README, architecture, roadmap and identity plan. No schema/dependency upgrades or permission expansion to other roles.

### Committed source
- Head: `c66a3060ba8db9437a9116a6633fbf1ed8a64961`.
- Tree: `47d1392e42bbdbcef0eea674579e3265fb4ea75c`; GitHub tree matched the local staged tree before commit.
- Main/base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. No force push or main commit.
- A single large GitHub tree request timed out (422); incremental tree creation succeeded with the exact same expected tree.

### Executed validation
- Downloaded Maven3.9.9 and Corretto JDK17 for compilation; pinned project dependencies unchanged. Used runtime proxy and system truststore with TLS verification enabled.
- `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: PASS on committed source; **85 tests**, zero failures/errors/skips. Includes architecture, persistence, actual embedded-server startup and session expiry.
- Full backend JaCoCo: **449/449 lines,150/150 branches =100%**,53 classes. Changed AccountService22/22lines,10/10branches; AccountController5/5lines; new AccountPage1/1line (latter two have no branches). Reports: `backend/target/site/jacoco/index.html`, `jacoco.xml`, `backend/target/surefire-reports/`.
- Frontend `npm ci`; committed-source `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS, **105 tests/24files**. Full project **906/906 lines,665/665 branches**,100% functions/statements. AccountDirectory and changed AccountProvisioning have100% lines/branches; no exclusions/threshold changes. Reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`.
- `npx playwright install chromium`: PASS.
- `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e`: PASS, **115 executions in3.3m =23 distinct scenarios ×5 viewport projects**. Chromium141.0.7390.37/build1194, Playwright1.56.1; widths320/390/412/768/1440. These are Chromium emulations, not physical-device or Firefox/WebKit certification.
- Real React/Vite API proxy and packaged Spring Boot with isolated synthetic H2/accounts. Health startup, successful pagination, invalid-page rejection, connectivity failure/retry and responsive layout assertions passed without mocked APIs.
- Temporary servers stopped; ports8080/5173 confirmed closed in the same execution after teardown; command exit0. `frontend/test-results/.last-run.json`: passed, no failed tests. Failure-only screenshot/trace artifacts produced none.
- Logs: `/tmp/ems-nov12-backend.log`, `/tmp/ems-nov12-front-final.log`, `/tmp/ems-nov12-install.log`, `/tmp/ems-nov12-e2e.log`. Preliminary tests also passed; final committed-source counts above are authoritative.
- Head/main rechecked after validation: unchanged; worktree clean. Static diff checks passed.

### Architecture and disposition
Controllers delegate to AccountService; the service owns read-only pagination/DTO mapping and repositories handle persistence. Identity DTOs never expose entities or hashes. New tests remain in feature/unit, feature/api, feature/integration, frontend tests/unit/identity, tests/integration/identity and e2e/identity. Architecture checks pass; existing transactions and locking unchanged. GitHub Actions remain manual-only.

**No merge:** safe audit records, explicit account/employee/team linkage and scoped authorization, account editing/deactivation, profile/password changes, account-change revocation and reset lifecycle/full acceptance are still incomplete. MFA/SSO, migrations and operational readiness also remain pending. Main remains unauthenticated. No deployment, real employee data or external messages.

---

## November 11, 2026 — Scheduled employee-management development: provisioning acceptance

**DRAFT / UNMERGED.** Resumed the existing authentication branch. This is the current validation record; earlier records below are historical.

### Completed
Added three distinct real-browser role-selection/provisioning/login cases for ADMIN, HR and MANAGER, complementing EMPLOYEE. Verify exactly the assigned authority, intended workspace, HR-only employee routes, and denied HR/manager administrative escalation with valid CSRF. Added frontend unit cases for EMPLOYEE/MANAGER selection and real-client integration for removing administration/credentials after provisioning receives401. Updated roadmap and identity plan. No production code or dependency changes today.

### Validated source
- Head: `b4fe41201d88da0634fff58c4cfb75db12f6f5b5`.
- Tree: `a063c7ed15d5227787c226b45e03dd533cf2bd50`; local and GitHub trees matched before commit.
- Main/base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. Head/base rechecked after validation, unchanged. Worktree clean. No force push.

### Executed commands and results
- Downloaded Maven3.9.9 and Corretto JDK17; existing project dependency versions unchanged. Used runtime proxy settings and system trusted certificates without disabling TLS verification.
- `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: **PASS**, compilation/package and **82 tests**, zero failures/errors/skips; architecture and embedded-server startup/expiry checks included.
- Full backend JaCoCo: **442/442 lines,146/146 branches,100%**. Reports `backend/target/site/jacoco/index.html`, `jacoco.xml`, `backend/target/surefire-reports/`.
- Frontend `npm ci`; `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: **PASS**, **100 tests in22files**, full project **100% lines/branches/functions/statements**. Reports `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`. No changed production-code scope today; complete production coverage still measured.
- `npx playwright install chromium`: PASS. `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e`: **110 passed in3.2m =22 distinct scenarios ×5 viewport projects**.
- Chromium141.0.7390.37/build1194, Playwright1.56.1; widths320/390/412/768/1440. Emulated Chromium sizes only, not physical devices or Firefox/WebKit.
- Real React/Vite and packaged Spring Boot, isolated synthetic H2/accounts. No mocked API replacement. Health startup and frontend/backend integration succeeded.
- Both temporary server ports8080/5173 confirmed closed in the same execution after teardown; command exit0. `frontend/test-results/.last-run.json` reports passed/no failed tests. Failure-only traces/screenshots produced none.
- Logs: `/tmp/ems-nov11-backend.log`, `/tmp/ems-nov11-e2e.log`, `/tmp/ems-nov11-install.log`. Static diff check passed.

### Architecture and remaining blockers
Tests remain in tests/unit/identity, tests/integration/identity and named e2e/identity suites. Backend architecture checks pass. No layer, permission, transaction, coverage-threshold or exclusion changes. Actions remain manual-only.

**No merge:** auditing, employee/team linkage and scoped authorization, account editing/deactivation, password/profile changes, account-change revocation, reset lifecycle and full acceptance remain incomplete. MFA/SSO, migrations and operational readiness remain pending. Main remains unauthenticated. No deployment, external messages or real data.

### Restored November 10 record (executed yesterday, recorded here today)
The previous run committed the ADMIN creation screen, role selection/default EMPLOYEE, password clearing, duplicate-submit protection, API error feedback and assigned-role API tests:
- Head `a863a1261f94c6a785c340b8d7b1ff7e31c48cea`; tree `83160ca748118d4f3e658e5005bfa0fe923b7b5c`; same main/base above.
- `npm ci`, `npm run build`, `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`:97 tests/22files, full frontend100% across all coverage metrics.
- Initial Maven compile failed with “release version17 not supported” in the environment; downloaded Corretto17.0.21. First retry hit PKIX trust failure. Configured the existing system truststore, kept TLS verification enabled, then ran `JAVA_HOME=/tmp/ems-jdk17 MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts /tmp/apache-maven-3.9.9/bin/mvn -U -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`:PASS,82 tests and442/442lines,146/146branches100%. -U retried failed artifact resolution; no project dependency upgrades.
- Browser install succeeded; `PATH=/tmp/ems-jdk17/bin:$PATH npm run test:e2e`:95 passed/3.0m,19 scenarios×5 widths, including actual creation, invalid password, duplicate login, new EMPLOYEE login and denied escalation. Same Chromium version above.
- Both servers stopped and ports confirmed closed. Reports used the same paths above; logs were `/tmp/ems-nov10-backend-final.log`, `/tmp/ems-nov10-e2e.log`, `/tmp/ems-nov10-install.log`.
- No merge. The PR record update was not completed in that run; this restores the actual observed results without claiming they were executed today. Today's full validation separately covers the current head.

---

## November 9, 2026 — Scheduled employee-management development: cross-tab sign-out

**DRAFT / UNMERGED.** Continued the existing branch; this record is the current validation status. Earlier records below are historical.

### Change and limitations
Successful server logout publishes an opaque random localStorage notification marker. Other same-origin tabs clear identity/password/CSRF state when the event arrives, without another directory request. No identity, credentials or session tokens are stored. A generation guard ignores late session-check success/error results after sign-out. Failed logout publishes nothing; disabled storage does not turn successful server logout into failure. API-401 recovery remains the fallback. Browser suspension or storage restrictions can delay/prevent notifications; this is not password/role/account-change revocation.

Added feature-owned unit tests for notification publication, storage denial, unrelated/removal events, listener cleanup and stale asynchronous session results; frontend integration verifies removal of the real workspace without additional HTTP. Updated the real-backend two-tab scenario and added browser storage-denial coverage. README/architecture/roadmap/identity plan updated. Backend production behavior is unchanged; existing backend unit/API/integration/architecture tests were rerun. No dependencies upgraded.

### Source
- Validated head: `85d27009db66cb451dabbdf808cfee553ceafbd2`.
- Validated tree: `838a9604c9e02cf4fa0ac3c6fe356a88cde77911`; GitHub tree matched local staged tree before commit.
- Validated base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. Remote head/base rechecked after validation and unchanged. Clean worktree; no force push.

### Executed validation
- Downloaded Maven 3.9.9 without upgrading project dependencies. `/tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: **PASS**, build/package and **81 tests**, zero failures/errors/skips. Includes architecture, real embedded-server startup/persistence and inactivity expiry.
- Backend JaCoCo full project: **442/442 lines, 146/146 branches = 100%**. Reports: `backend/target/site/jacoco/index.html`, `backend/target/site/jacoco/jacoco.xml`, `backend/target/surefire-reports/`.
- From frontend: `npm ci`; `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: **PASS**, **92 tests / 20 files**. Full project and changed AuthShell/sessionEvents: **100% lines, branches, functions, statements**. Reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`.
- `npx playwright install chromium`: PASS. `npm run test:e2e`: **90 passed in 2.4m = 18 distinct scenarios × 5 viewport projects**. Chromium 141.0.7390.37/build1194, Playwright1.56.1, widths320/390/412/768/1440. Browser emulation only, not physical-device or cross-browser certification.
- Real React/Vite proxy and packaged Spring Boot with isolated synthetic H2/accounts; no API mocks in E2E. Browser storage-denial injection changes only the storage API, not server responses. Health startup check and business integration passed.
- Temporary servers stopped; same-execution socket checks confirmed **8080 and5173 closed**, command exit0.
- `frontend/test-results/.last-run.json`: passed, no failed tests. No failure-only traces/screenshots produced. Workspace logs: `/tmp/ems-nov9-backend.log`, `/tmp/ems-nov9-e2e.log`, `/tmp/ems-nov9-install.log`.
- Static diff check passed. Preliminary frontend coverage also passed before the final committed-source run; final counts above are authoritative.

### Architecture and disposition
Frontend changes stay in the identity feature, with tests in tests/unit/identity, tests/integration/identity and e2e/identity. No backend layer dependencies, transactions, permissions, architecture checks, exclusions or coverage thresholds changed. GitHub Actions remain manual-only.

**No merge:** safe security auditing, explicit employee/team ownership and scoped authorization, account management/profile/password changes, account-change revocation, reset lifecycle and complete acceptance coverage remain unfinished. Main is still unauthenticated. MFA/SSO, migrations and operational readiness remain pending. No deployment, external messages or real data. Continue this draft branch.

---

## November 8, 2026 — Scheduled employee-management development: session recovery

**Current status: DRAFT / UNMERGED.** Continued the existing authentication branch; no duplicate PR or main commit. This record supersedes earlier validation counts below, which remain historical.

### Changes and review
- Clear the transient login password in finally after CSRF or login transport failures; existing HTTP rejection cleanup remains. No password persistence or default credentials.
- Added HTTP-fixture frontend integration tests for both failed request stages and recovery, plus the unit regression.
- Added real embedded-Tomcat session expiry integration: assert the configured 1,800-second default; shorten only the synthetic test session to one second; verify actual inactivity expiry, 401 for protected reads, 403 for stale CSRF login, and successful fresh-token reauthentication with a different session ID. Production configuration and security filters are unchanged.
- Added two named real-backend Playwright recovery scenarios: browser connectivity loss/recovery without mocked API responses, and logout in another tab followed by request-triggered removal of stale employee content and fresh-CSRF reauthentication. This does not claim immediate cross-tab notification or password/role-change revocation.
- Updated README, roadmap and identity delivery plan. No dependency version changes.

### Committed source and base
- Validated head: `d7f80036b568922fac89e8efb259337544e8e6f3`.
- Validated tree: `7aa69f0d1bba74e618f47b2533659b32468e914d` (local staged tree matched GitHub tree before commit).
- Base/main: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- `git fetch origin`; `git merge --ff-only origin/main`: already up to date. No force push. Local worktree clean. Remote PR head and main rechecked after validation and unchanged.

### Actual commands and results
- Downloaded Maven 3.9.9; used a runtime proxy settings file without logging secrets.
- `/tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: **PASS**, compilation/package, **81 backend tests**, zero failures/errors/skips, including architecture, API, persistence and real application startup checks.
- JaCoCo full-project **442/442 lines, 146/146 branches (100%)**, 52 classes. No backend production changes in this increment. Reports: `backend/target/site/jacoco/index.html`, `jacoco.xml`, `backend/target/surefire-reports/`.
- From frontend: `npm ci`; `npm run build`; `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: **PASS**, **86 tests / 19 files**, full-project **100% lines, branches, functions and statements**. Changed production AuthShell also 100%. Reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info`.
- `npx playwright install chromium`: PASS, pinned browser installed.
- `npm run test:e2e`: **85 passed in 2.3 minutes = 17 distinct scenarios × 5 viewport projects**, including the two new recovery scenarios. Chromium 141.0.7390.37/build1194, Playwright 1.56.1; widths 320, 390, 412, 768 and 1440. Emulation only, not physical devices or Firefox/WebKit validation.
- Playwright started the real packaged Spring Boot backend, waited for health, and used the real React/Vite API proxy with isolated synthetic H2 and synthetic identities. Both temporary server ports **8080 and 5173 were confirmed closed in the same execution after teardown**; command exit 0.
- `frontend/test-results/.last-run.json`: passed, no failed tests. Failure-only trace/screenshots generated none. Workspace logs: `/tmp/ems-nov8-backend.log`, `/tmp/ems-nov8-e2e.log`, `/tmp/ems-nov8-browser-install.log`.
- `git diff --cached --check`: PASS before commit.

### Package/layer compliance and remaining blockers
The new backend integration test lives under identity/integration; frontend unit/integration tests remain under their owning feature; browser tests are in e2e/identity. No layer dependencies, transactions, route permissions, architecture checks, coverage exclusions or thresholds changed. Actions remain manual-only.

**No merge:** safe security auditing, explicit employee/team ownership and scoped authorization, account-management/profile/password flows, password/role/deactivation revocation, recovery/reset lifecycle and their complete acceptance coverage remain unfinished. MFA/SSO, migrations and production session/operational readiness also remain unresolved. Main remains unauthenticated. No deployment, external messages or real data used. Continue on this draft branch.

---

## Current status — November 7, 2026

Scheduled employee-management development continued on the existing authentication branch. **Draft; no merge performed.** Added initial single-instance login abuse controls. Main remains unchanged.

### This increment
- A synchronized identity service reserves login admission before password verification. Five admitted attempts per normalized login in a fixed 60-second window; successful authentication resets its key. Denied requests do not extend the window. Fresh sessions, casing and surrounding whitespace do not bypass the key.
- At most 16 concurrent credential checks and 1,024 retained bounded login keys. Expired counters are reclaimed; overlong identifiers share an invalid-key bucket. Capacity limits fail closed. Provider releases admission in finally, including persistence failures.
- Known, unknown and limited logins retain the same generic HTTP 401 response. React suggests checking credentials or waiting a minute; no account-existence or limit-state disclosure. No passwords/tokens/forwarded headers are retained by the limiter.
- Added unit tests for expiry boundaries, concurrent admission, memory capacity, invalid keys, bypassed hash work when limited, and release on unexpected errors. Added real credential/persistence expiry recovery, API cross-session/known-versus-unknown checks, frontend HTTP rejection/recovery integration and a real-browser throttle scenario with unique synthetic accounts per viewport.
- Documented single-process limitations: restart resets counters, multiple instances need shared atomic state, targeted temporary denial/capacity exhaustion remain availability risks, and trusted edge controls/load testing/tuning/alerting remain required. This is not complete distributed brute-force protection.

### Committed-source validation
- Validated head: `b845cc7e8c2dc55417c8459bf14e6451da5b359f`.
- Tree: `d709833b4312877519985455bdcbdd8fac10f32f`; local tree matched GitHub tree, then fetched commit was validated with a clean worktree.
- Base, rechecked after validation: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`. No newer main changes or other open feature PRs were found.
- `/tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: PASS, **80 tests**, zero failures/errors/skips; includes architecture, API and actual application startup integration. Full backend **442/442 lines, 146/146 branches (100%)**. Reports: `backend/target/site/jacoco/index.html`, `backend/target/site/jacoco/jacoco.xml`, `backend/target/surefire-reports/`.
- `npm ci`, `npm run build`, `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS. **84 tests in 19 files**; full frontend **100% lines/branches/functions/statements**. Reports: `frontend/coverage/index.html`, `coverage-final.json`, `lcov.info` under that directory. No dependency or lockfile changes.
- `npx playwright install chromium`: succeeded via automatic fallback mirrors after truncated primary browser-component archives. `npm run test:e2e`: **75 passed (2.0m)** = **15 distinct scenarios × five viewport projects**. Chromium 141.0.7390.37 / Playwright 1.56.1; widths 320/390/412/768/1440. These are emulated Chromium sizes, not physical-device or cross-browser certification.
- Real Spring Boot + React/Vite proxy + isolated synthetic H2 database. New browser coverage verifies five failed attempts block a correct password in a fresh context and an independent account still signs in. Expiry recovery is verified with real credential/persistence integration and a controlled Clock, not claimed as a browser wait test. Limits were not disabled or raised for test execution.
- Backend health/startup and existing employee frontend/backend flows passed. Playwright stopped its temporary servers; socket checks in the same execution confirmed 8080 and 5173 closed. Browser status: `frontend/test-results/.last-run.json`; no failed tests or failure-only trace/screenshot artifacts. Logs: `/tmp/ems-nov7-final-backend.log`, `/tmp/ems-nov7-final-front.log`, `/tmp/ems-nov7-e2e.log` (workspace-local).
- Static diff checks passed. Production changes stay in identity/service and the existing React identity feature; constructor dependencies, service boundaries and employee locking are preserved. Test suites remain feature-organized. No coverage/architecture thresholds or exclusions changed; Actions stay manual-only.

### Remaining gates
Safe security audit records; explicit account/employee and manager/team ownership; account-management UI; profile/password changes and role/deactivation revocation; expiring single-use reset flows; dedicated session inactivity-expiry and complete lifecycle/scoped-access acceptance. Distributed abuse controls and MFA/SSO, migrations and production session/recovery operations remain readiness work. Keep this PR draft and continue this branch. No deployment, external messaging, real employee data or automation changes.

---
## Historical validation records (current status above takes precedence)

## Current status — November 6, 2026

Scheduled employee-management development continues on this existing draft branch. **Do not merge yet.** The branch now implements login/logout, CSRF-protected server sessions, an HR-only employee workspace and ADMIN-only account provisioning. Main remains unchanged; full authentication/authorization is still unfinished.

### Implemented this run
- Spring Security provider delegates to the credential service. Generic failed-login responses, session-ID rotation, CSRF on unsafe requests (including login/logout), 30-minute inactivity timeout and Secure/HttpOnly/SameSite=Strict cookie defaults.
- Deny-by-default route matrix: HR alone accesses all employee/nested routes; ADMIN provisions accounts but has no HR-data bypass. MANAGER/EMPLOYEE business access stays denied until ownership relationships are defined. Own session/logout is available to signed-in roles.
- Explicit empty-store ADMIN bootstrap from operator-supplied environment credentials; no defaults, public registration or overwriting existing accounts. Single-instance bootstrap only; remove bootstrap secrets after setup.
- Responsive React login, generic rejection/retry, session-expiry handling, logout and access-denied views. No browser localStorage credentials/tokens.
- Existing API and E2E suites now authenticate as synthetic HR through the real security chain rather than disabling filters. Added identity unit/API/frontend integration and real-backend browser scenarios.
- Authenticated Faker presentation startup provisions a disposable HR account before Java seeding and UI startup; no credentials printed. Fixed immediate startup after browser teardown while retaining live-listener rejection.
- Added pinned @types/node 24.10.0 (+ its required undici-types) for Node-based Playwright configuration. Existing dependency versions were not upgraded; Spring Security uses the existing Boot BOM.

### Source and validation record
- Base, rechecked: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`.
- Session implementation and full application validation: `040cdf817190e208109462f44d5dd703e02227d1`, tree `471fbae973bcf3a7ff6c923d5cfae3f7a83707cd`.
- Final head: `0f3c7cdee35772e40191a268798d28e0dddcc194`, tree `27807fce93e06f71919c2a31a0b38aa02ca952db`. The follow-up changes only presentation scripts/tests and roadmap. Backend/frontend trees are unchanged from the fully validated session commit. Presentation checks validate the follow-up content; no claim that the complete application suite was rerun after this helper-only commit.
- `/tmp/apache-maven-3.9.9/bin/mvn -f backend/pom.xml -s /tmp/ems-settings.xml -B clean verify`: PASS, **72 tests**, no failures/errors/skips. Includes architecture checks and an actual embedded-server authenticated startup/persistence test. Full backend **417/417 lines, 126/126 branches (100%)**. Reports: `backend/target/site/jacoco/{index.html,jacoco.xml}`, `backend/target/surefire-reports/`.
- `npm ci`, `npm run build`, `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS. **83 tests in 18 files**; full frontend **100% lines/branches/functions/statements**. Reports: `frontend/coverage/{index.html,coverage-final.json,lcov.info}`.
- `npx playwright install chromium`: succeeded with fallback mirrors after truncated primary downloads. `npm run test:e2e`: **70 passed (1.8m)** on committed session source: **14 distinct scenarios × 5 viewport projects**, comprising 10 existing business scenarios and four new authentication/role scenarios. Chromium 141.0.7390.37, Playwright 1.56.1; widths 320,390,412,768,1440. Emulated Chromium viewports only, not all physical devices or browsers.
- Browser tests used the real Spring Boot JAR and Vite proxy with an isolated in-memory H2 database, randomized synthetic passwords and real ADMIN provisioning/HR login. Backend health was checked before tests. No mocked API replacement for E2E.
- `mvn -f tools/presentation/pom.xml dependency:build-classpath -Dmdep.outputFile=target/classpath.txt` (same Maven/settings): PASS. `python tools/presentation/tests/integration/test_startup.py`: PASS for five example.com records before UI startup, empty-store/opt-in guards and server cleanup. `python tools/presentation/tests/integration/test_port_guard.py`: PASS for rejecting active listeners on 8080 and 5173.
- Initial final-sequence presentation startup failed with EADDRINUSE after browser teardown. Diagnostics reproduced the TIME_WAIT port-preflight problem. Fixed with SO_REUSEADDR (not SO_REUSEPORT); repeated `npm run test:e2e -- e2e/identity/sessions.spec.ts --project=small-phone` followed immediately by presentation startup passed, and active-listener guard tests passed. These supplemental executions do not increase the distinct-scenario count.
- Early validation failures (short two-character HR fixture login, startup test lacking authentication, missing Node types and invalid RTL option) were corrected; no gates were weakened. Subsequent application builds/tests passed.
- Workspace logs: `/tmp/ems-nov6-final-{backend,build,front,e2e}.log`, `/tmp/ems-nov6-handoff-fixed.log`. Browser traces/screenshots are failure-only; successful full run produced no failure artifacts. Presentation helper coverage is not included in the Java/TypeScript 100% metric.
- Temporary servers stopped; port checks found 8080/5173 closed. Local worktree clean; static diff checks pass. No deployment or external messaging.

### Architecture and remaining gates
Identity code stays in feature/controller/service/repository/entity/dto packages with constructor injection and service transactions. No redundant DAOs, entity-to-DTO dependency, changed employee lock ordering, architecture exclusions or weakened coverage thresholds. Actions remain manual-only.

Remaining before merge: login abuse controls and safe audit records; explicit account/employee and manager/team relationships; account-management UI; profile/password changes and role/deactivation revocation; expiring single-use reset workflow; dedicated inactivity-expiry and full lifecycle/scoped-access acceptance tests. MFA/SSO, migrations and production session/recovery operations remain enterprise-readiness work. Existing passing checks do not make these unfinished capabilities complete. Continue this branch; do not create a duplicate authentication PR. **No merge performed.**

---
## Historical record — November 5 (superseded by current status above)

## Scheduled employee-management development

Authentication is the next product feature. This draft starts its credential foundation; **do not merge yet**. Existing employee APIs are still unauthenticated. No login, session or authorization flow is claimed complete.

### Changes
- Add feature-owned identity entity/repository/service/DTO packages. Account identities exclude password hashes; no HTTP controller, registration endpoint, startup account or default credentials is introduced.
- Persist normalized unique login identifiers and ADMIN/HR/MANAGER/EMPLOYEE roles. Role storage confers no permissions.
- Use Spring Security Crypto (existing Boot BOM version) with salted BCrypt cost 12, minimum 15 Unicode code points and maximum 72 UTF-8 bytes. Overlong passwords are rejected, never silently truncated. Unknown accounts still perform a dummy-hash comparison; abuse controls remain required.
- Add nine focused unit/persistence integration tests covering salting, exact-password verification, Unicode/byte limits, invalid identifiers/roles/passwords, absent accounts, duplicate logins and all-role persistence.
- Update authentication plan, architecture, README and roadmap with the unfinished boundary.

### Source and validation
- Base: `3bbbd965bb21fcd40071a2e3a0e0a52da9eecbb6`
- Head: `7e23eec18865ef791fe3390e515db4c6ba007e3a`
- Validated source tree: `71659931c5273f84851ec2b53328369ca4dcc2c8` (local staged tree equals GitHub commit tree).
- `/tmp/apache-maven-3.9.9/bin/mvn -s /tmp/ems-auth-settings.xml -B clean verify` in backend: PASS, 67 tests, no failures/errors/skips; package/dependency architecture checks included.
- JaCoCo full project: 374/374 lines, 116/116 branches (100%). New identity source: 39/39 lines, 16/16 branches (100%). Report: `backend/target/site/jacoco/index.html`, XML: `backend/target/site/jacoco/jacoco.xml`; test reports: `backend/target/surefire-reports/`.
- `npm ci` and `npm run build` in frontend: PASS. No dependency upgrades or lockfile edits.
- `npm run test:coverage -- --maxWorkers=1 --no-file-parallelism`: PASS, 76 tests in 16 files; full frontend 100% lines/branches/functions/statements. No frontend production changes. Reports: `frontend/coverage/index.html`, `frontend/coverage/coverage-final.json`, `frontend/coverage/lcov.info`.
- `npx playwright install chromium`: PASS after automatic fallback mirrors recovered truncated primary downloads (Chromium/FFmpeg/headless shell). No test bypass.
- `npm run test:e2e`: PASS, 50 executions of 10 distinct existing business scenarios, 1.7 minutes. Playwright 1.56.1 / Chromium 141.0.7390.37 (build 1194), widths 320/390/412/768/1440. These are emulated Chromium viewports, not physical-device or cross-browser certification.
- Browser suites ran against the real Spring Boot JAR and Vite proxy with isolated `jdbc:h2:mem:playwright;DB_CLOSE_DELAY=-1`, synthetic records and cleanup. Playwright waited for backend `/actuator/health` before execution. Server teardown completed; subsequent socket checks found both 8080 and 5173 closed.
- Browser status artifact: `frontend/test-results/.last-run.json` reports passed with no failed tests. Trace/screenshot retention is failure-only; no failure artifacts were produced. Execution logs: `/tmp/ems-auth-e2e.log`; backend/frontend logs: `/tmp/ems-auth-backend.log`, `/tmp/ems-auth-frontend-tests.log` (workspace-local, not published artifacts).
- No authentication-specific browser results are claimed: the credential service is internal and the login/authorization UI and endpoints are not implemented.
- `git diff --cached --check`: PASS.

### Architecture review
Identity has focused entity/repository/service/DTO classes; no empty controller layer or DAO wrapper. Services own transactions and constructor injection; entities accept domain values, not transport DTOs. Existing employee locks and business services are unchanged. No architecture or coverage exclusions/thresholds were weakened. Actions remain manual-only.

### Unmet merge gates / next work on this branch
Implement a coherent secure session + React login/logout/access-denied flow, protected administrative provisioning and least-privilege enforcement for every API. Define explicit employee/account and manager/team links before scoped access. Add CSRF, cookie/fixation/expiry controls, login abuse controls, safe audit records, account/password/role-change revocation and single-use expiring reset tokens. Add focused authentication API/frontend integration and real-backend Playwright suites, including allowed/denied roles, cross-user access and error flows, and retain measured 100% changed-code coverage. Existing browser regressions alone cannot satisfy these gates. Keep the PR draft and unmerged until they pass.

No deployment, external messages or real employee/account data. No application processes are intentionally retained.

### Final disposition — November 5, 2026
Remote head/base were rechecked after validation and remain the SHAs above. Local worktree is clean at the committed head. Both builds, 67 backend tests, 76 frontend tests and all 50 existing browser executions pass; full backend and frontend line/branch coverage is 100%. **No merge performed:** authentication-specific API/frontend/E2E acceptance and the actual session/authorization flow remain incomplete. Resume this draft branch rather than starting a duplicate authentication branch. Recurring development is not disabled.
