import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnd } from '../servers.ts'

// Lesson 13: ten cases (11 tests), each case with ONE planted fault. Ledgerline is fine; the tests are wrong.
//   npm run broken:angular
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  // Next to this config, so this suite never overwrites the report or traces of your own tests
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  outputDir: 'test-results',
  // Actions and assertions give up after 5 s, so failures are quick. Opening a page isn't what these cases get
  // wrong, so it keeps the usual allowance and a busy machine can't fail a fixed test.
  use: {
    ...devices['Desktop Chrome'], baseURL: frontEnd.url, trace: 'retain-on-failure',
    actionTimeout: 5_000, navigationTimeout: 30_000,
  },
  expect: { timeout: 5_000 },
  webServer: [backEnd, frontEnd.webServer],
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    { name: 'chromium', dependencies: ['setup'] },
  ],
})
