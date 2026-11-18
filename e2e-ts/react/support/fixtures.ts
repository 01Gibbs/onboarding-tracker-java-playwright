import { test as base } from '@playwright/test'
import { LedgerApi } from './ledger-api.ts'
import { TELLER } from './users.ts'

// Lesson 09: custom fixtures. A test asks for what it needs ({ api }) and Playwright builds it, hands it over,
// and tidies up afterwards, exactly like the built-in `page`. `api` is done as a worked example.

interface Fixtures {
  /** The JSON API, signed in as the teller, for arranging data. Disposed after each test. */
  api: LedgerApi
  // TODO lesson 09: add signedIn (a DashboardPage, signed in as the teller) and accountPage (an AccountPage)
}

export const test = base.extend<Fixtures>({
  api: async ({ playwright, baseURL }, use) => {
    const api = await LedgerApi.signIn(playwright.request, baseURL!, TELLER)
    await use(api)
    await api.dispose()
  },
})

export { expect } from '@playwright/test'
