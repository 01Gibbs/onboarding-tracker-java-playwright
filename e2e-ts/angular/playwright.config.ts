import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnds } from '../frontends.ts'

// YOUR Playwright configuration for the Angular track. Lesson 04 walks through it; lesson 10 adds to it.
//   npm run test:angular                      every test in this folder
//   npm run test:angular -- lesson02          just one lesson's tests
//   npm run test:angular -- --headed          watch the browser
//   npm run test:angular -- --ui              UI mode: pick, run, watch and time-travel through tests
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never', outputFolder: '../playwright-report/angular' }]],
  outputDir: '../test-results/angular',
  use: {
    // page.goto('/customers') opens http://localhost:4200/customers. BASE_URL=... points at another environment.
    baseURL: process.env.BASE_URL ?? frontEnds.angular.url,
    trace: 'retain-on-failure',
  },
  // Starts the back end and the Angular app if they aren't already running, and waits until they answer.
  webServer: process.env.BASE_URL ? undefined : [backEnd, frontEnds.angular.webServer],
  projects: [
    // TODO lesson 10: add a "setup" project that runs *.setup.ts files, and make chromium depend on it.
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
