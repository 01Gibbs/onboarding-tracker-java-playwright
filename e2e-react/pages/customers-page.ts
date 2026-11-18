import type { Locator, Page } from '@playwright/test'
import { CustomerDetailPage } from './customer-detail-page.ts'
import { CustomerFormPage } from './customer-form-page.ts'

/** The customer list at /customers. */
export class CustomersPage {
  constructor(private readonly page: Page) {}

  async open(): Promise<CustomersPage> {
    throw new Error('TODO lesson 07')
  }

  async search(text: string): Promise<CustomersPage> {
    throw new Error('TODO lesson 07')
  }

  async newCustomer(): Promise<CustomerFormPage> {
    throw new Error('TODO lesson 07')
  }

  get rows(): Locator {
    throw new Error('TODO lesson 07: the customer-row test id')
  }

  get emptyMessage(): Locator {
    throw new Error('TODO lesson 07')
  }

  async openCustomer(fullName: string): Promise<CustomerDetailPage> {
    throw new Error('TODO lesson 07: click their link in the "Customers" table')
  }
}
