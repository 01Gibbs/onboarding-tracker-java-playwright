import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnd } from './servers.ts'

// YOUR Playwright configuration for the React track. Lesson 04 walks through it; lesson 10 adds to it.
//   npm run test:react                      every test in this folder
//   npm run test:react -- lesson02          just one lesson's tests
//   npm run test:react -- --headed          watch the browser
//   npm run test:react -- --ui              UI mode: pick, run, watch and time-travel through tests
export default defineConfig({
  testDir: '.',
  // The reference answers and lesson 13's broken suite have their own configs
  testIgnore: ['**/solutions/**', '**/broken/**'],
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    // page.goto('/customers') opens http://localhost:5173/customers. BASE_URL=... points at another environment.
    baseURL: process.env.BASE_URL ?? frontEnd.url,
    trace: 'retain-on-failure',
  },
  // Starts the back end and the React app if they aren't already running, and waits until they answer.
  webServer: process.env.BASE_URL ? undefined : [backEnd, frontEnd.webServer],
  projects: [
    // TODO lesson 10: add a "setup" project that runs *.setup.ts files, and make chromium depend on it.
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
