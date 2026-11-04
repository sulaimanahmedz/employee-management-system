import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  workers: 1,
  projects: [
    { name: 'small-phone', use: { viewport: { width: 320, height: 740 }, isMobile: true, hasTouch: true } },
    { name: 'iphone-size', use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true } },
    { name: 'android-size', use: { viewport: { width: 412, height: 915 }, isMobile: true, hasTouch: true } },
    { name: 'tablet', use: { viewport: { width: 768, height: 1024 }, hasTouch: true } },
    { name: 'desktop', use: { viewport: { width: 1440, height: 1000 } } },
  ],
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
