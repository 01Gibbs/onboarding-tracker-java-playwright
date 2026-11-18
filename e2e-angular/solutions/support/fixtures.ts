import { test as base } from '@playwright/test'
import { AccountPage } from '../pages/account-page.ts'
import { CustomersPage } from '../pages/customers-page.ts'
import { DashboardPage } from '../pages/dashboard-page.ts'
import { LoginPage } from '../pages/login-page.ts'
import { LedgerApi } from './ledger-api.ts'
import { TELLER } from './users.ts'

// Lesson 09: custom fixtures. A test asks for what it needs ({ api, accountPage }) and Playwright builds it,
// hands it over, and tidies up afterwards, exactly like the built-in `page`.

interface Fixtures {
  /** The JSON API, signed in as the teller, for arranging data. Disposed after each test. */
  api: LedgerApi
  /** A browser page already signed in as the teller, showing the dashboard. */
  signedIn: DashboardPage
  loginPage: LoginPage
  customersPage: CustomersPage
  accountPage: AccountPage
}

export const test = base.extend<Fixtures>({
  api: async ({ playwright, baseURL }, use) => {
    const api = await LedgerApi.signIn(playwright.request, baseURL!, TELLER)
    await use(api)
    await api.dispose()
  },
  signedIn: async ({ page }, use) => {
    await use(await new LoginPage(page).signInAs(TELLER))
  },
  loginPage: async ({ page }, use) => use(new LoginPage(page)),
  customersPage: async ({ page }, use) => use(new CustomersPage(page)),
  accountPage: async ({ page }, use) => use(new AccountPage(page)),
})

export { expect } from '@playwright/test'
