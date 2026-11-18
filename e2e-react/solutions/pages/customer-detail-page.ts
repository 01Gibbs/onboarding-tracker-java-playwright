import type { Locator, Page } from '@playwright/test'
import { CustomerFormPage } from './customer-form-page.ts'
import { NavBar } from './nav-bar.ts'
import { OpenAccountPage } from './open-account-page.ts'

/** A single customer at /customers/{id}. */
export class CustomerDetailPage {
  readonly nav: NavBar
  readonly heading: Locator
  readonly status: Locator
  readonly flash: Locator
  readonly accountRows: Locator

  constructor(private readonly page: Page) {
    this.nav = new NavBar(page)
    this.heading = page.getByRole('heading', { level: 1 })
    this.status = this.heading.getByTestId('status-badge')
    this.flash = page.getByTestId('flash-message')
    this.accountRows = page.getByTestId('account-row')
  }

  async open(customerId: number): Promise<CustomerDetailPage> {
    await this.page.goto(`/customers/${customerId}`)
    return this
  }

  /** The value shown for a term in the details list, e.g. detail('Email'): each pair is a group named by its term. */
  detail(term: string): Locator {
    return this.page.getByRole('group', { name: term, exact: true }).getByRole('definition')
  }

  async edit(): Promise<CustomerFormPage> {
    await this.page.getByRole('link', { name: 'Edit' }).click()
    return new CustomerFormPage(this.page)
  }

  async toggleSuspension(): Promise<void> {
    await this.page.getByRole('button', { name: /^(Suspend|Reactivate) customer$/ }).click()
  }

  /** Clicks "Delete customer" and accepts the browser's confirm dialog. */
  async deleteAndConfirm(): Promise<void> {
    this.page.once('dialog', dialog => dialog.accept())
    await this.page.getByRole('button', { name: 'Delete customer' }).click()
  }

  async openAccount(): Promise<OpenAccountPage> {
    await this.page.getByRole('link', { name: 'Open account' }).click()
    return new OpenAccountPage(this.page)
  }
}
