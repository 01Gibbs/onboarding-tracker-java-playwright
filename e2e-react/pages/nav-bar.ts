import type { Locator, Page } from '@playwright/test'
import { AccountsPage } from './accounts-page.ts'
import { CustomersPage } from './customers-page.ts'

/** The header on every signed-in page. */
export class NavBar {
  readonly currentUser: Locator

  constructor(private readonly page: Page) {
    this.currentUser = page.getByTestId('current-user')
  }

  async customers(): Promise<CustomersPage> {
    throw new Error('TODO lesson 07: click "Customers" in the Main navigation')
  }

  async accounts(): Promise<AccountsPage> {
    throw new Error('TODO lesson 07')
  }

  async signOut(): Promise<void> {
    throw new Error('TODO lesson 07')
  }
}
