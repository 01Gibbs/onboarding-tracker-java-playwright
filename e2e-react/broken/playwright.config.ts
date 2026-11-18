import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnd } from '../servers.ts'

// Lesson 13: ten cases (11 tests), each case with ONE planted fault. Ledgerline is fine; the tests are wrong.
//   npm run broken:react
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  // Actions and assertions give up after 5 s, so failures are quick
  use: { ...devices['Desktop Chrome'], baseURL: frontEnd.url, trace: 'retain-on-failure', actionTimeout: 5_000 },
  expect: { timeout: 5_000 },
  webServer: [backEnd, frontEnd.webServer],
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    { name: 'chromium', dependencies: ['setup'] },
  ],
})
