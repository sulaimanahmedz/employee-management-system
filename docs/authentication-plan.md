# Next feature: authentication and roles

Requested October 25, 2026. This is an implementation plan, not completed security functionality. The application currently exposes unauthenticated APIs and must not be deployed as an enterprise system.

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
