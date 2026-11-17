import type { Locator, Page } from '@playwright/test'
import { CustomerDetailPage } from './customer-detail-page.ts'

/** The "New customer" and "Edit customer" form. Fields are public so tests can assert on them directly. */
export class CustomerFormPage {
  readonly firstName: Locator
  readonly lastName: Locator
  readonly email: Locator

  constructor(private readonly page: Page) {
    this.firstName = page.getByLabel('First name')
    this.lastName = page.getByLabel('Last name')
    this.email = page.getByLabel('Email')
  }

  async fill(first: string, last: string, email: string): Promise<CustomerFormPage> {
    throw new Error('TODO lesson 07')
  }

  async save(): Promise<CustomerDetailPage> {
    throw new Error('TODO lesson 07')
  }

  /** Saves when the form should come back with errors. */
  async saveExpectingErrors(): Promise<CustomerFormPage> {
    throw new Error('TODO lesson 07')
  }

  get errorSummary(): Locator {
    throw new Error('TODO lesson 07')
  }
}
