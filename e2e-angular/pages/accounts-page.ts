import type { Page } from '@playwright/test'

/** The account list at /accounts. Add methods when a test needs them. */
export class AccountsPage {
  constructor(private readonly page: Page) {}

  async open(): Promise<AccountsPage> {
    await this.page.goto('/accounts')
    return this
  }
}
