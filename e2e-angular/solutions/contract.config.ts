import { defineConfig } from '@playwright/test'
import { backEnd, repoRoot } from '../servers.ts'
import solutions from './playwright.config.ts'

// For maintainers, not part of the course. The contract check: these answers against every Ledgerline front end.
// They all keep one UI contract (docs/ui-contract.md), so the same answers must pass on each.   npm run contract
const frontEnds = [
  { name: 'react', url: 'http://localhost:5173', command: 'npm run dev -w frontends/react' },
  { name: 'angular', url: 'http://localhost:4200', command: 'npm run dev -w frontends/angular' },
  { name: 'thymeleaf', url: 'http://localhost:8080' }, // served by the back end
]

export default defineConfig({
  ...solutions,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'contract-report' }]],
  webServer: [
    backEnd,
    ...frontEnds.flatMap(f =>
      f.command ? [{ command: f.command, cwd: repoRoot, url: f.url, reuseExistingServer: true, timeout: 120_000 }] : []),
  ],
  projects: [
    // Cookies ignore ports, so the sessions saved through the Angular app work on every front end
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    ...frontEnds.map(f => ({
      name: f.name,
      dependencies: ['setup'],
      use: { baseURL: f.url },
      // @spa tests check things only a single-page app does (e.g. "Processing…" while a request runs)
      ...(f.name === 'thymeleaf' ? { grepInvert: /@spa/ } : {}),
    })),
  ],
})
