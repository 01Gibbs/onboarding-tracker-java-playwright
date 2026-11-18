import { defineConfig, devices } from '@playwright/test'
import { backEnd, frontEnd } from '../servers.ts'

// The reference answers, against the React front end.   npm run solutions:react
export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { ...devices['Desktop Chrome'], baseURL: frontEnd.url, trace: 'retain-on-failure' },
  webServer: [backEnd, frontEnd.webServer],
  projects: [
    // Lesson 10: sign in once per role and save the cookies; every other test depends on it
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    { name: 'chromium', dependencies: ['setup'] },
  ],
})
