# Repository working preferences

- Create new features from latest main on a short feature/<feature-name> branch. Do not commit feature changes directly to main.
- After each feature is implemented, build/compile frontend and backend, run available tests and temporary application integration checks in the same task. Downloads required for validation are authorized; do not upgrade dependencies incidentally.
- Merge the feature pull request only after successful validation against latest main. Record commands and validated commit SHAs; preserve concurrent changes and never force-push.
- Keep GitHub Actions manual-only so committing code does not run builds.
- Keep frontend code in frontend/ and the Spring Boot backend in backend/.
- Use synthetic data only. Do not deploy or send external messages.
- These October 24, 2026 preferences replace weekly-only validation and direct-main development.

## Coding and package standards

- Read docs/architecture.md before adding or changing features.
- Organize backend code by feature under com.ems (employee, document, leave, onboarding, employmenthistory), then by controller, service, repository, entity and dto. Add a new feature package when a responsibility is distinct; do not put unrelated classes in employee or create empty layers.
- Controllers handle HTTP validation/mapping and delegate to services. Services own business rules and transaction boundaries. Spring Data repositories provide data access; do not add redundant DAO wrappers.
- Entities accept domain values and must not depend on request/response DTOs, controllers, services or repositories. DTOs must not depend on services/controllers/repositories. Keep repositories independent of HTTP and service layers.
- Use constructor injection, explicit project imports and small focused classes. Put request DTOs in dto, not nested inside controllers. Preserve API contracts, entity/table names and transaction/locking behavior during refactors.
- Cross-feature business operations go through services; shared entity references/repositories remain permitted for employee ownership checks. Employment history writes must join the employee transaction.
- Group integration tests under the owning feature. Keep the architecture checks passing; extend them deliberately when adding a layer, never weaken them merely to pass a change.
- Include package/layer compliance in every PR review and validation record. Keep documentation consistent with the implemented structure.
