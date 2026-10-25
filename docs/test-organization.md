# Test suite organization

| Layer | Location | Purpose |
| --- | --- | --- |
| Backend unit | src/test/java/com/ems/<feature>/unit | Domain behavior without Spring |
| Backend API | src/test/java/com/ems/<feature>/api | HTTP validation, status codes, serialized contracts with real application wiring |
| Backend integration | src/test/java/com/ems/<feature>/integration | Persistence/service collaboration without HTTP |
| Architecture | src/test/java/com/ems/architecture | Package and dependency rules |
| Frontend unit | frontend/tests/unit/<feature> | Isolated API and utility contracts |
| Frontend integration | frontend/tests/integration/<feature> | Components with real HTTP client and simulated responses |
| Browser E2E | frontend/e2e/<feature> | User journeys against real frontend/backend |

API tests are also integration tests by execution scope, but have their own directory so their contract purpose is clear. Do not duplicate tests solely to fill folders. Test helpers live in e2e/support. Playwright contains four named business suites: Directory, Profiles, Documents and Onboarding. Each scenario runs against five viewport profiles (320,390,412,768,1440px) using Chromium. These are emulated screen/touch conditions, not certification on every physical phone or browser.

Run frontend unit/integration suites with npm test or npm run test:coverage, and browser suites with npm run test:e2e. Backend mvn verify discovers *Test classes recursively; package reorganization does not change discovery. Coverage thresholds and architecture checks remain unchanged.
