import type { Locator, Page } from '@playwright/test'
import { AccountsPage } from './accounts-page.ts'
import { CustomersPage } from './customers-page.ts'

/** The header on every signed-in page. */
export class NavBar {
  readonly currentUser: Locator
  private readonly nav: Locator

  constructor(private readonly page: Page) {
    this.nav = page.getByRole('navigation', { name: 'Main' })
    this.currentUser = page.getByTestId('current-user')
  }

  async customers(): Promise<CustomersPage> {
    await this.nav.getByRole('link', { name: 'Customers' }).click()
    return new CustomersPage(this.page)
  }

  async accounts(): Promise<AccountsPage> {
    await this.nav.getByRole('link', { name: 'Accounts' }).click()
    return new AccountsPage(this.page)
  }

  async signOut(): Promise<void> {
    await this.page.getByRole('button', { name: 'Sign out' }).click()
  }
}
