import type { Locator, Page } from '@playwright/test'

/** A single account at /accounts/{id}: its details, the "Move money" form and the transaction history. */
export class AccountPage {
  readonly balance: Locator

  constructor(private readonly page: Page) {
    this.balance = page.getByTestId('account-balance')
    // TODO lesson 09: status, flash, moveMoney (the region), amount, toAccount, submit, transactionRows
  }

  async open(accountId: number): Promise<AccountPage> {
    await this.page.goto(`/accounts/${accountId}`)
    return this
  }

  async chooseType(label: 'Deposit' | 'Withdrawal' | 'Transfer'): Promise<AccountPage> {
    throw new Error('TODO lesson 09')
  }

  async deposit(amount: string): Promise<AccountPage> {
    throw new Error('TODO lesson 09')
  }

  async withdraw(amount: string): Promise<AccountPage> {
    throw new Error('TODO lesson 09')
  }

  async transfer(amount: string, toAccountId: number): Promise<AccountPage> {
    throw new Error('TODO lesson 09: toAccount.selectOption(String(id)) picks the option whose value is the id')
  }

  async filterTransactions(typeLabel: string): Promise<void> {
    throw new Error('TODO lesson 09')
  }

  async click(buttonName: string): Promise<void> {
    throw new Error('TODO lesson 09')
  }

  /** Clicks a confirm-protected button (Close account, Delete account) and accepts the dialog. */
  async clickAndConfirm(buttonName: string): Promise<void> {
    throw new Error('TODO lesson 09')
  }
}
