import type { Locator, Page } from '@playwright/test'
import { CustomerDetailPage } from './customer-detail-page.ts'

/** The "New customer" and "Edit customer" form. */
export class CustomerFormPage {
  readonly firstName: Locator
  readonly lastName: Locator
  readonly email: Locator
  readonly phone: Locator
  readonly errorSummary: Locator
  private readonly saveButton: Locator

  constructor(private readonly page: Page) {
    this.firstName = page.getByLabel('First name')
    this.lastName = page.getByLabel('Last name')
    this.email = page.getByLabel('Email')
    this.phone = page.getByLabel('Phone')
    this.errorSummary = page.getByTestId('error-summary')
    this.saveButton = page.getByRole('button', { name: 'Save customer' })
  }

  async fill(first: string, last: string, email: string): Promise<CustomerFormPage> {
    await this.firstName.fill(first)
    await this.lastName.fill(last)
    await this.email.fill(email)
    return this
  }

  async save(): Promise<CustomerDetailPage> {
    await this.saveButton.click()
    return new CustomerDetailPage(this.page)
  }

  /** Saves when the form should come back with errors. */
  async saveExpectingErrors(): Promise<CustomerFormPage> {
    await this.saveButton.click()
    return this
  }
}
