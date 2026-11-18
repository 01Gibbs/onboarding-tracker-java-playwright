import type { Locator, Page } from '@playwright/test'

/** A single account at /accounts/{id}: its details, the "Move money" form and the transaction history. */
export class AccountPage {
  readonly heading: Locator
  readonly status: Locator
  readonly accountNumber: Locator
  readonly balance: Locator
  readonly flash: Locator
  readonly moveMoney: Locator
  readonly amount: Locator
  readonly toAccount: Locator
  readonly description: Locator
  readonly submit: Locator
  readonly transactionRows: Locator

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { level: 1 })
    this.status = this.heading.getByTestId('status-badge')
    this.accountNumber = page.getByTestId('account-number')
    this.balance = page.getByTestId('account-balance')
    this.flash = page.getByTestId('flash-message')
    this.moveMoney = page.getByRole('region', { name: 'Move money' })
    this.amount = this.moveMoney.getByLabel('Amount')
    this.toAccount = this.moveMoney.getByLabel('To account')
    this.description = this.moveMoney.getByLabel('Description')
    this.submit = this.moveMoney.getByRole('button', { name: 'Submit' })
    this.transactionRows = page.getByTestId('transaction-row')
  }

  async open(accountId: number): Promise<AccountPage> {
    await this.page.goto(`/accounts/${accountId}`)
    return this
  }

  async chooseType(label: 'Deposit' | 'Withdrawal' | 'Transfer'): Promise<AccountPage> {
    await this.moveMoney.getByRole('radio', { name: label }).check()
    return this
  }

  async deposit(amount: string): Promise<AccountPage> {
    return this.move('Deposit', amount)
  }

  async withdraw(amount: string): Promise<AccountPage> {
    return this.move('Withdrawal', amount)
  }

  async transfer(amount: string, toAccountId: number): Promise<AccountPage> {
    await this.chooseType('Transfer')
    await this.toAccount.selectOption(String(toAccountId)) // by option value: the account's id
    await this.amount.fill(amount)
    await this.submit.click()
    return this
  }

  async filterTransactions(typeLabel: string): Promise<void> {
    await this.page.getByLabel('Show').selectOption({ label: typeLabel })
  }

  async click(buttonName: string): Promise<void> {
    await this.page.getByRole('button', { name: buttonName }).click()
  }

  /** Clicks a confirm-protected button (Close account, Delete account) and accepts the dialog. */
  async clickAndConfirm(buttonName: string): Promise<void> {
    this.page.once('dialog', dialog => dialog.accept())
    await this.click(buttonName)
  }

  private async move(type: 'Deposit' | 'Withdrawal', amount: string): Promise<AccountPage> {
    await this.chooseType(type)
    await this.amount.fill(amount)
    await this.submit.click()
    return this
  }
}
