import type { Locator, Page } from '@playwright/test'
import { CustomerDetailPage } from './customer-detail-page.ts'
import { CustomerFormPage } from './customer-form-page.ts'

/** The customer list at /customers. */
export class CustomersPage {
  readonly rows: Locator
  readonly emptyMessage: Locator
  private readonly table: Locator

  constructor(private readonly page: Page) {
    this.table = page.getByRole('table', { name: 'Customers' })
    this.rows = page.getByTestId('customer-row')
    this.emptyMessage = page.getByText('No customers match your search.')
  }

  async open(): Promise<CustomersPage> {
    await this.page.goto('/customers')
    return this
  }

  async search(text: string): Promise<CustomersPage> {
    await this.page.getByRole('searchbox', { name: 'Search customers' }).fill(text)
    await this.page.getByRole('button', { name: 'Search' }).click()
    return this
  }

  async newCustomer(): Promise<CustomerFormPage> {
    await this.page.getByRole('link', { name: 'New customer' }).click()
    return new CustomerFormPage(this.page)
  }

  row(containing: string): Locator {
    return this.rows.filter({ hasText: containing })
  }

  async openCustomer(fullName: string): Promise<CustomerDetailPage> {
    await this.table.getByRole('link', { name: fullName }).click()
    return new CustomerDetailPage(this.page)
  }
}
