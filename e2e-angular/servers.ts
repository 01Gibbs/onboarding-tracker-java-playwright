import path from 'node:path'
import { fileURLToPath } from 'node:url'
import type { PlaywrightTestConfig } from '@playwright/test'

// Where Ledgerline runs, and how Playwright starts it if it isn't running yet. Every config in this folder
// uses it, so there is one place to change a port.

type WebServer = NonNullable<PlaywrightTestConfig['webServer']>

export const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/** The Spring Boot back end. The Angular app talks to it. */
export const backEnd = {
  command: process.platform === 'win32' ? 'mvnw.cmd -pl app spring-boot:run' : './mvnw -pl app spring-boot:run',
  cwd: repoRoot,
  url: 'http://localhost:8080/api/health',
  reuseExistingServer: true, // started by you in another terminal? Then Playwright uses that one
  timeout: 180_000,
} satisfies WebServer

/** The Angular front end: the app your tests drive. */
export const frontEnd = {
  url: 'http://localhost:4200',
  webServer: {
    command: 'npm run dev -w frontends/angular',
    cwd: repoRoot,
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 120_000, // ng serve compiles the whole app before it answers
  } satisfies WebServer,
}
