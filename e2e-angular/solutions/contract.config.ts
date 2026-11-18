import { defineConfig } from '@playwright/test'
import { backEnd, frontEnd, repoRoot } from '../servers.ts'
import solutions from './playwright.config.ts'

// For maintainers, not part of the course. The contract check: these answers against every Ledgerline front end.
// They all keep one UI contract (docs/ui-contract.md), so the same answers must pass on each.   npm run contract
const otherFrontEnd = {
  command: 'npm run dev -w frontends/react', cwd: repoRoot, url: 'http://localhost:5173', reuseExistingServer: true,
}

export default defineConfig({
  ...solutions,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'contract-report' }]],
  webServer: [backEnd, frontEnd.webServer, otherFrontEnd], // Thymeleaf is served by the back end
  projects: [
    // Cookies ignore ports, so the sessions saved through the Angular app work on every front end
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    { name: 'angular', dependencies: ['setup'], use: { baseURL: frontEnd.url } },
    { name: 'react', dependencies: ['setup'], use: { baseURL: otherFrontEnd.url } },
    // @spa tests check things only a single-page app does (e.g. "Processing…" while a request runs)
    { name: 'thymeleaf', dependencies: ['setup'], use: { baseURL: 'http://localhost:8080' }, grepInvert: /@spa/ },
  ],
})
