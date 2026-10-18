# Repository working preferences

- During ordinary development, write and commit code without downloading dependencies, building, testing, or starting the application.
- Dependency downloads, builds, tests and application startup checks are allowed only during the weekly maintenance task, or when the user explicitly requests them.
- Keep GitHub Actions manual-only so committing code does not run builds.
- Keep frontend code in frontend/ and the Spring Boot backend in backend/.
