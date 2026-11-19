import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnd } from '../servers.ts'

// The reference answers, against the Angular front end.   npm run solutions:angular
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // Next to this config, so this suite never overwrites the report or traces of your own tests
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  outputDir: 'test-results',
  use: { ...devices['Desktop Chrome'], baseURL: frontEnd.url, trace: 'retain-on-failure' },
  webServer: [backEnd, frontEnd.webServer],
  projects: [
    // Lesson 10: sign in once per role and save the cookies; every other test depends on it
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    { name: 'chromium', dependencies: ['setup'] },
  ],
})
