import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  workers: 1,
  use: { baseURL: 'http://127.0.0.1:5173', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  webServer: [
    {
      command: 'java -jar ../backend/target/employee-management-api-0.1.0-SNAPSHOT.jar',
      url: 'http://127.0.0.1:8080/actuator/health',
      env: { DB_URL: 'jdbc:h2:mem:playwright;DB_CLOSE_DELAY=-1', PORT: '8080' },
      reuseExistingServer: false,
      timeout: 90000,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: false,
    },
  ],
});
