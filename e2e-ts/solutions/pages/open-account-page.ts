import type { Page } from '@playwright/test'
import { AccountPage } from './account-page.ts'

/** The "Open account" form at /accounts/new. */
export class OpenAccountPage {
  constructor(private readonly page: Page) {}

  /** The holder is already chosen when you arrive from a customer's page: leave holder out to keep it. */
  async open(type: 'Current' | 'Savings', nickname: string, holder?: string): Promise<AccountPage> {
    if (holder) {
      await this.page.getByLabel('Account holder').selectOption({ label: holder })
    }
    await this.page.getByRole('radio', { name: type }).check()
    await this.page.getByLabel('Nickname').fill(nickname)
    await this.page.getByRole('button', { name: 'Open account' }).click()
    return new AccountPage(this.page)
  }
}
