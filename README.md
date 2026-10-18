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
    App.tsx              # Directory, search, summary and add form
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

The UI lists, searches and adds employees, displays directory totals, and handles loading, empty, validation and connection-error states. The REST API additionally supports employee retrieval, update and deletion. The database enforces unique, case-normalized email addresses. No demo employee or credentials are inserted.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| GET | `/api/v1/employees` | List employees |
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

Backend tests cover the employee lifecycle, invalid input, duplicate emails and health. GitHub Actions provides these checks through manual dispatch only. Commits and pull requests do not trigger builds; dependency downloads and application validation are reserved for the weekly maintenance task.

## Next development

See [architecture](docs/architecture.md) and [roadmap](docs/roadmap.md). This initial codebase is for local development: authentication and authorization are not implemented yet. Add access controls, migrations and a production database before hosting employee data publicly.
