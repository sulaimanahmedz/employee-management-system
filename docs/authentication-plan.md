# Next feature: authentication and roles

Requested October 25, 2026. This is an implementation plan with partial session functionality on the unmerged authentication branch. Main remains unauthenticated; neither branch is ready for enterprise deployment.

Deliver coherent `feature/auth-*` increments after the pending testing foundations. Use Spring Security, separate identity feature/layer packages and the existing React architecture. All merge, coverage, architecture and real-backend E2E gates remain mandatory.

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
