import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnds } from '../frontends.ts'

// The reference answers for the React and Angular tracks, run against every front end. Because all front ends
// keep one UI contract (docs/ui-contract.md), the same answers must pass on each: this is the contract check.
//   npm run solutions:react     your track's front end only
//   npm run contract            every front end
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: '../playwright-report/solutions' }]],
  outputDir: '../test-results/solutions',
  use: {
    ...devices['Desktop Chrome'],
    trace: 'retain-on-failure',
  },
  webServer: [backEnd, frontEnds.react.webServer],
  projects: [
    // Signs in once per role, through the React track's front end, and saves the cookies (lesson 10). Cookies
    // ignore ports, so the saved sessions also work when the same answers run against Thymeleaf.
    { name: 'setup', testMatch: /.*\.setup\.ts/, use: { baseURL: frontEnds.react.url } },
    { name: 'react', dependencies: ['setup'], use: { baseURL: frontEnds.react.url } },
    { name: 'thymeleaf', dependencies: ['setup'], use: { baseURL: frontEnds.thymeleaf.url } },
  ],
})
