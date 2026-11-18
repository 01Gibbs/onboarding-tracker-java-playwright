import type { Locator, Page } from '@playwright/test'

/** The account list at /accounts. */
export class AccountsPage {
  readonly rows: Locator

  constructor(private readonly page: Page) {
    this.rows = page.getByTestId('account-row')
  }

  async open(): Promise<AccountsPage> {
    await this.page.goto('/accounts')
    return this
  }

  async filterByStatus(statusLabel: string): Promise<AccountsPage> {
    await this.page.getByLabel('Status').selectOption(statusLabel)
    await this.page.getByRole('button', { name: 'Apply filters' }).click()
    return this
  }

  row(containing: string): Locator {
    return this.rows.filter({ hasText: containing })
  }
}
