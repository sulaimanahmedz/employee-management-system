# Product roadmap

The goal is an employee management application spanning the employee lifecycle.

## Foundation (this initial codebase)

- React/TypeScript application shell and responsive employee directory.
- Spring Boot feature structure, employee API, validation and local persistence.
- Integration tests, build workflow and development documentation.

## Identity and organization

- Sign-in and role-based permissions for administrators, HR, managers and employees.
- Departments, locations, teams and reporting hierarchy.
- Auditing, PostgreSQL and versioned database migrations.

## Employee lifecycle

- Implemented in source: profile viewing and editing (October 21, 2026); execution validation pending.
- Next: search pagination, employment history and document metadata.
- Onboarding and offboarding workflows with assigned tasks.
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
