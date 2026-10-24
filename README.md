# Employee Management System

A foundation for a full employee management application: employee profiles, organization structure, onboarding, attendance, leave, and administrative workflows.

## Technology

- **UI:** React 19, TypeScript, Vite 7, responsive CSS.
- **API:** Java 17, Spring Boot 3.5, Spring MVC, Jakarta Validation, Spring Data JPA.
- **Local database:** H2 stored on disk; no database installation required.
- **Build checks:** GitHub Actions for the frontend build and backend integration tests.

## Start locally

Prerequisites: Node.js 22.12+ with npm, Java 17+, Maven 3.6.3+.

Terminal 1:

```bash
cd backend
mvn spring-boot:run
```

Terminal 2:

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173. Vite forwards `/api` requests to http://localhost:8080. Add employees using the form; the directory starts empty and retains records across backend restarts in `backend/data/`.

## Project structure

```text
frontend/
  src/
    features/employees/   # Employee types and API calls
    lib/                 # Shared HTTP client
    App.tsx              # Directory, search, summary and profile navigation
    styles.css           # Responsive application styling
backend/
  src/main/java/com/ems/
    employee/            # Controller, service, repository, entity and DTOs
    common/              # Shared error handling
  src/main/resources/    # Runtime configuration
  src/test/              # API integration tests
  pom.xml
docs/
  architecture.md
  roadmap.md
.github/workflows/ci.yml
```

## Initial functionality

The UI lists, searches and adds employees, displays directory totals, and handles loading, empty, validation and connection-error states. Select an employee name to load their full profile, then use **Edit profile** to update their name, work email, department, job title or status. Saving refreshes both the profile and directory totals. Cancel or Back to directory asks before discarding edited fields; failed saves retain the draft for correction. The REST API also supports deletion. The database enforces unique, case-normalized email addresses. No demo employee or credentials are inserted.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/api/v1/employees` | Legacy unpaginated list |
| GET | `/api/v1/employees/directory` | Search/filter a page of employees with organization totals |
| GET | `/api/v1/employees/{id}` | Get one employee |
| POST | `/api/v1/employees` | Create employee; returns 201 and Location |
| PUT | `/api/v1/employees/{id}` | Replace employee details |
| DELETE | `/api/v1/employees/{id}` | Delete employee; returns 204 |
| GET | `/actuator/health` | Backend health |

Example request for POST or PUT:

```json
{
  "firstName": "Aisha",
  "lastName": "Khan",
  "email": "aisha@example.com",
  "department": "Engineering",
  "jobTitle": "Developer",
  "status": "ACTIVE"
}
```

All fields are required. Status is `ACTIVE`, `ON_LEAVE`, or `INACTIVE`. Invalid input returns 400, missing employees return 404, and duplicate email addresses return 409 using problem-detail responses.

## Configuration

Backend environment variables: `PORT` (8080), `DB_URL` (`jdbc:h2:file:./data/employees`), `DB_USERNAME` (`sa`), `DB_PASSWORD` (empty). This scaffold includes the H2 driver only; changing database vendors also requires adding the corresponding JDBC dependency and migration configuration.

Frontend: optionally copy `frontend/.env.example` to `frontend/.env.local`. `VITE_API_BASE_URL` is empty by default. For a separate API origin, configure CORS on the backend or use a same-origin reverse proxy. Vite's API proxy applies only to the development server; production hosting must route `/api` to the backend.

## Build and verify

```bash
cd frontend
npm ci
npm run build
```

```bash
cd backend
mvn verify
java -jar target/employee-management-api-0.1.0-SNAPSHOT.jar
```

Backend test source covers the employee lifecycle, full profile updates, invalid input, duplicate-email rollback, retaining an employee's own email, missing profiles and health. GitHub Actions provides these checks through manual dispatch only. Commits and pull requests do not trigger builds; dependency downloads and application validation now run after each completed feature before merge.

## Next development

See [architecture](docs/architecture.md) and [roadmap](docs/roadmap.md). This initial codebase is for local development: authentication and authorization are not implemented yet. Add access controls, migrations and a production database before hosting employee data publicly.

## Profile feature validation

Profile viewing/editing was added on October 21, 2026. Only static source review was performed for this change; compilation, automated tests and browser execution remain pending the weekly maintenance check. See [profile acceptance checks](docs/profile-validation.md). Existing GET/PUT endpoints are reused without a schema or dependency change.

## Search and pagination

The directory now searches on the server. Enter a full or partial name, email, department or job title, optionally select a status and rows per page, then select **Apply filters** (or press Enter). **Clear filters** restores all employees and 10 rows per page. Previous/Next navigate matching results; the overview cards always show organization-wide totals. Profile changes refresh the directory when you return, preserving the applied filters and page where possible.

Example: `GET /api/v1/employees/directory?q=engineering&status=ACTIVE&page=0&size=10`.

- `q`: optional, at most 200 characters; trimmed, case-insensitive literal substring search. Percent and underscore are ordinary characters.
- `status`: optional `ACTIVE`, `ON_LEAVE` or `INACTIVE`.
- `page`: zero-based, defaults to 0; non-negative.
- `size`: 1–100, defaults to 10. The page offset must fit a signed 32-bit integer.
- Response: `items`, `page`, `size`, `totalElements`, `totalPages`, `totalEmployees`, `activeEmployees`, `departments`. Matching counts apply to filters; the final three counts cover all employees. Department counts use distinct stored department names.
- Ordering is ascending employee ID. Out-of-range pages return empty items and matching counts; the UI moves back to the last available page.
- Existing `GET /api/v1/employees` keeps its array response for compatibility.

Added October 24, 2026. Static source review only; compilation, tests and browser execution are pending the weekly check. See [directory acceptance checks](docs/directory-validation.md).

## Document register

Each employee profile includes a document register for names (1–150 characters), categories (`CONTRACT`, `CERTIFICATE`, `OTHER`) and optional ISO expiry dates. Add metadata or remove an entry with confirmation. This does not upload files, store document numbers, or provide access control. Historical expiry dates are allowed. Entries are newest first and are removed when their employee is deleted.

`GET` and `POST /api/v1/employees/{employeeId}/documents` list/create metadata; `GET` and `DELETE /api/v1/employees/{employeeId}/documents/{documentId}` retrieve/remove one entry. Missing employees or a document belonging to another employee return 404. Invalid metadata returns 400. No dependencies were added.

Development now uses `feature/<short-name>` branches. Each completed feature is compiled and validated before its PR is merged into main; this replaces the earlier weekly-only validation policy. GitHub Actions remains manual-only.

## Employment history

Profiles show a read-only, paginated employment timeline, newest first. Creating an employee records their initial department, job title and status. Saving a change to any of these fields appends a snapshot in the same transaction; name/email-only or unchanged saves do not add events. Existing employees get a `BASELINE` of their previous details on their next successful save, followed by a `CHANGED` event if applicable. No past dates are invented.

`GET /api/v1/employees/{id}/history?page=0&size=10` returns `items`, `page`, `size`, `totalElements` and `totalPages`. Page must be nonnegative, size 1–100, and offset must fit a signed integer. Entries include `id`, `recordedAt` (UTC instant), `kind` (`CREATED`, `BASELINE`, `CHANGED`), `department`, `jobTitle` and `status`. Missing employees return 404. Entries are snapshots at save time, not effective-dated employment records or a compliance audit log. The UI formats timestamps in the viewer's local timezone. History is deleted with its employee. Actor attribution and retention controls require future identity/audit work.

## Onboarding checklist

Each profile has a checklist with a required task title (up to 150 characters), required owner label (up to 100), and optional ISO due date. Add tasks, mark them complete, reopen them or remove them with confirmation. Completion totals include all pages; lists show 10 tasks per page, newest first. Owner labels are descriptive text, not authenticated assignments, and no notification is sent. Past due dates are allowed. Checklist completion does not change employee status. Tasks are deleted with their employee.

- `GET /api/v1/employees/{id}/onboarding-tasks?page=0`: returns `items`, `page`, `totalPages`, `totalElements`, `completedTasks`. Nonnegative page and signed-integer offset required.
- `POST` to the collection: `{ "title": "Prepare laptop", "owner": "IT team", "dueOn": "2026-12-01" }`; creates a pending task and returns 201/Location.
- `GET` or `DELETE /api/v1/employees/{id}/onboarding-tasks/{taskId}`: retrieve/remove an employee-scoped task.
- `PUT /api/v1/employees/{id}/onboarding-tasks/{taskId}/completion`: `{ "completed": true }` or `false`; idempotently sets completion. Missing completion is invalid.

Missing employees or tasks (including another employee's task) return 404. Invalid input returns 400. Templates, identity-based assignment, reminders, edit-conflict detection and offboarding workflows remain future work.

Checklist tasks can also be edited using **Edit**: correct the title, owner label or due date and choose **Save task changes**. Cancel asks before discarding changed fields; failed saves retain the draft. Clearing the date removes it. Completion is preserved. `PUT /api/v1/employees/{id}/onboarding-tasks/{taskId}` accepts the same detail fields as creation and returns the saved task. Detail and completion updates lock the task row so they do not overwrite each other's fields; edits to the same details remain last-write-wins.

Use **Show onboarding tasks** to view all, pending or completed tasks. Filtering happens before pagination and changing the filter resets to the first page. Completing, reopening or removing tasks refreshes the active filter and moves back from an empty last page. A newly added pending task will not appear under the Completed filter; switch to Pending or All to see it. Overall completion totals remain independent of the filter.

The checklist GET endpoint accepts optional `completed=true` or `completed=false`; omit it for all tasks. `totalElements`/`totalPages` describe matches, while the added `totalTasks` and existing `completedTasks` describe the employee's entire checklist. Invalid boolean values return 400. Other endpoints and default list behavior remain compatible.

## Leave requests

Profiles now record annual, personal or unpaid full-day leave requests with start/end dates and a paginated history (10 per page, newest first). Requests begin **Pending** and can be cancelled with confirmation; cancellation is idempotent and retains the record. Failed submissions retain entered dates. Dates are inclusive calendar days, including weekends and holidays, not deducted entitlement. Historical dates and single-day requests are allowed. End dates before start dates return 400; dates overlapping another pending request for the same employee return 409, including shared boundary dates. Creation and cancellation serialize on the employee row to protect overlap checks. Cancelled requests do not block new requests.

- `GET /api/v1/employees/{id}/leave-requests?page=0`: `items`, `page`, `totalPages`, `totalElements`; page must be nonnegative with a signed-integer offset.
- `POST` to the collection: `{ "type": "ANNUAL", "startOn": "2026-12-01", "endOn": "2026-12-03" }`. Types: `ANNUAL`, `PERSONAL`, `UNPAID`. Returns 201 and Location.
- `GET /api/v1/employees/{id}/leave-requests/{requestId}`: retrieve one scoped request, including `calendarDays` and `status` (`PENDING`/`CANCELLED`).
- `PUT /api/v1/employees/{id}/leave-requests/{requestId}/cancellation`: cancel, no body required.

Missing employees or another employee's request return 404. Records are deleted with their employee. This is a request register, not approval or entitlement processing: there are no approval actions, authenticated requesters, balances, half-days, holiday rules, notifications or automatic employee-status changes. Those require future policy/identity work. No reasons or medical details are collected. Execution validation is recorded against the exact feature commit in its PR before merge.

### Find leave requests

Use the leave register's **Request status**, **Period from** and **Period to** controls, then **Apply leave filters**. Clear restores all requests. The applied-filter summary identifies the results currently shown even while changing draft filter fields. Filters combine with AND and apply before pagination. Period matching is inclusive overlap, so a request starting before the selected period is included if it ends within or after it. Either date may be omitted. Cancelling or adding refreshes the current filter and returns from an empty last page; new requests outside the filter remain hidden until filters are cleared.

The collection GET accepts optional `status=PENDING|CANCELLED`, `from=YYYY-MM-DD`, and `to=YYYY-MM-DD`. Invalid status/date values or a reversed period return 400. Page totals describe matching requests only. Defaults and all mutation endpoints remain unchanged. Feature PR records build, test and runtime validation against the exact commit.

### Edit document metadata

Choose **Edit** beside a document, correct its name, category or expiry date, then **Save metadata changes**. Clearing the date removes the expiry. Cancel asks before discarding changed fields; failed saves retain the draft. Removal is disabled during editing. `PUT /api/v1/employees/{employeeId}/documents/{documentId}` accepts the same validated fields as creation and returns the updated record with its original ID. Missing or cross-employee records return 404. Concurrent detail edits remain last-write-wins; file contents and edit history are not included. Validation results are recorded in the feature PR before merge.

### Document register pages

The profile loads 10 document records at a time, newest first, with Previous/Next controls and a total count. Adding returns to the first page, editing preserves the page, and removal returns from an empty last page. Page navigation is disabled while editing. `GET /api/v1/employees/{id}/documents/page?page=0` returns `items`, `page`, `totalPages` and `totalElements`. Page must be nonnegative and its offset must fit a signed integer; invalid pages return 400 and missing employees return 404. Out-of-range pages return empty items with totals. The original collection GET retains its array response for compatibility. Execution validation is recorded in the feature PR.
