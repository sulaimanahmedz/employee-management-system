# Repository working preferences

- Create new features from latest main on a short feature/<feature-name> branch. Do not commit feature changes directly to main.
- After each feature is implemented, build/compile frontend and backend, run available tests and temporary application integration checks in the same task. Downloads required for validation are authorized; do not upgrade dependencies incidentally.
- Merge the feature pull request only after successful validation against latest main. Record commands and validated commit SHAs; preserve concurrent changes and never force-push.
- Keep GitHub Actions manual-only so committing code does not run builds.
- Keep frontend code in frontend/ and the Spring Boot backend in backend/.
- Use synthetic data only. Do not deploy or send external messages.
- These October 24, 2026 preferences replace weekly-only validation and direct-main development.
