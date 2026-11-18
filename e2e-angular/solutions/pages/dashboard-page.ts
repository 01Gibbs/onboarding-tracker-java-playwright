import type { Locator, Page } from '@playwright/test'
import { NavBar } from './nav-bar.ts'

export class DashboardPage {
  readonly nav: NavBar
  readonly heading: Locator
  readonly activityPanel: Locator
  readonly activityItems: Locator

  constructor(private readonly page: Page) {
    this.nav = new NavBar(page)
    this.heading = page.getByRole('heading', { name: 'Dashboard' })
    this.activityPanel = page.getByRole('region', { name: 'Recent activity' })
    this.activityItems = page.getByTestId('activity-item')
  }

  async open(): Promise<DashboardPage> {
    await this.page.goto('/')
    return this
  }

  /** A summary card, e.g. stat('stat-customers'). */
  stat(testId: string): Locator {
    return this.page.getByTestId(testId)
  }

  async refreshActivity(): Promise<void> {
    await this.page.getByRole('button', { name: 'Refresh' }).click()
  }
}
