import { defineConfig, devices, type PlaywrightTestConfig } from '@playwright/test'
import { backEnd, frontEnds, type FrontEnd } from './frontends.ts'

// The shared suites (reference answers, and lesson 13's broken suite) run against one track's front end, or
// against every front end for the contract check. Each track's own config starts only its own front end and
// signs in through it, so a track never depends on another one.

type Suite = 'solutions' | 'broken'

export function suiteConfig(suite: Suite, name: string, tracks: FrontEnd[]): PlaywrightTestConfig {
  const webServers = tracks.flatMap(track => frontEnds[track].webServer ?? [])
  return defineConfig({
    testDir: '.',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: suite === 'solutions' && process.env.CI ? 1 : 0,
    reporter: [['list'], ['html', { open: 'never', outputFolder: `../playwright-report/${suite}-${name}` }]],
    outputDir: `../test-results/${suite}-${name}`,
    use: {
      ...devices['Desktop Chrome'],
      trace: 'retain-on-failure',
      // Lesson 13: actions and assertions give up after 5 s, so the broken suite fails quickly
      ...(suite === 'broken' ? { actionTimeout: 5_000 } : {}),
    },
    expect: suite === 'broken' ? { timeout: 5_000 } : {},
    webServer: [backEnd, ...webServers],
    projects: [
      // Signs in once per role, through the first front end, and saves the cookies (lesson 10). Cookies ignore
      // ports, so the saved sessions work on every front end in the run.
      { name: 'setup', testMatch: /.*\.setup\.ts/, use: { baseURL: frontEnds[tracks[0]].url } },
      ...tracks.map(track => ({
        name: track,
        dependencies: ['setup'],
        use: { baseURL: frontEnds[track].url },
        // @spa tests check things only a single-page app does (e.g. "Processing…" while a request runs)
        ...(track === 'thymeleaf' ? { grepInvert: /@spa/ } : {}),
      })),
    ],
  })
}
