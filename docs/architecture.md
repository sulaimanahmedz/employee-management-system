# Architecture

The repository is a monorepo with separately built frontend and backend applications. The backend is a modular monolith: business features own their controllers, services, DTOs, entities and repositories. Add future modules alongside `com.ems.employee`, keeping shared infrastructure in `com.ems.common`.

## Request flow

React components call feature-specific API functions through the shared HTTP client. `/api/v1` controllers validate request DTOs, call transactional services and return response DTOs. JPA repositories handle persistence. Entities are not exposed directly in HTTP responses.

The Vite development server proxies API requests to Spring Boot, avoiding cross-origin configuration for local development. For production, serve the frontend and API under one origin using a reverse proxy or explicitly configure trusted frontend origins.

## Persistence and error handling

H2 uses a local file under the backend working directory. Hibernate updates the schema for local development only. Replace this with versioned migrations and a production database before deployment. Tests use an isolated in-memory database. No seed data is loaded.

Request validation uses Jakarta Validation. Errors follow Spring problem details, with field-level validation errors and conflict responses for duplicate email addresses. Email addresses are normalized before persistence, and the database unique constraint handles concurrent duplicate submissions.

## Extension points

- Identity and access: implement authentication and role-based authorization across all employee endpoints.
- Organization: replace department text with department and reporting-line relationships.
- Employee lifecycle: add onboarding, employment history and audited status transitions.
- Attendance and leave: create separate feature modules with their own business rules.
- Scale: introduce paginated directory queries before loading large employee datasets.
- Operations: add schema migrations, structured observability and deployment configuration.

There is no authentication, payroll, leave approval, attendance tracking, audit history, or production hosting in this base. The UI exposes list/create; the API also exposes get/update/delete for future screens.
