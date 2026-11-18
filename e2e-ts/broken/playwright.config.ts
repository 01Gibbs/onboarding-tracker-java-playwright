import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnds } from '../frontends.ts'

// Lesson 13: ten cases (11 tests), each case with ONE planted fault. The app is fine; the tests are wrong.
//   npm run broken:react
// Actions and assertions give up after 5 s here, so failures are quick, and every failure keeps a trace.
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  reporter: [['list'], ['html', { open: 'never', outputFolder: '../playwright-report/broken' }]],
  outputDir: '../test-results/broken',
  use: {
    ...devices['Desktop Chrome'],
    actionTimeout: 5_000,
    trace: 'retain-on-failure',
  },
  expect: { timeout: 5_000 },
  webServer: [backEnd, frontEnds.react.webServer],
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/, use: { baseURL: frontEnds.react.url } },
    { name: 'react', dependencies: ['setup'], use: { baseURL: frontEnds.react.url } },
  ],
})
