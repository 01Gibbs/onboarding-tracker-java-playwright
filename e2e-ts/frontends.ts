import path from 'node:path'
import { fileURLToPath } from 'node:url'
import type { PlaywrightTestConfig } from '@playwright/test'

// Where each Ledgerline front end runs, and how Playwright starts it if it isn't running yet. Shared by every
// track's playwright.config.ts, so there is one place to change a port.

type WebServer = NonNullable<PlaywrightTestConfig['webServer']>

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

/** The Spring Boot back end. Every front end talks to it; the Thymeleaf front end is served by it. */
export const backEnd = {
  command: process.platform === 'win32' ? 'mvnw.cmd -pl app spring-boot:run' : './mvnw -pl app spring-boot:run',
  cwd: repoRoot,
  url: 'http://localhost:8080/api/health',
  reuseExistingServer: true, // started by you in another terminal? Then Playwright uses that one
  timeout: 180_000,
} satisfies WebServer

export const frontEnds = {
  react: {
    url: 'http://localhost:5173',
    webServer: {
      command: 'npm run dev -w frontends/react',
      cwd: repoRoot,
      url: 'http://localhost:5173',
      reuseExistingServer: true,
    } satisfies WebServer,
  },
  thymeleaf: {
    url: 'http://localhost:8080',
    webServer: undefined,
  },
} as const

export type FrontEnd = keyof typeof frontEnds
