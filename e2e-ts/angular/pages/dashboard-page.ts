import type { Locator, Page } from '@playwright/test'
import { NavBar } from './nav-bar.ts'

export class DashboardPage {
  readonly nav: NavBar
  readonly heading: Locator

  constructor(private readonly page: Page) {
    this.nav = new NavBar(page)
    this.heading = page.getByRole('heading', { name: 'Dashboard' })
    // TODO lesson 11: activityPanel (the "Recent activity" region) and activityItems (test id activity-item)
  }

  async open(): Promise<DashboardPage> {
    await this.page.goto('/')
    return this
  }

  async refreshActivity(): Promise<void> {
    throw new Error('TODO lesson 11: click Refresh')
  }
}
