import type { Locator, Page } from '@playwright/test'
import type { User } from '../support/users.ts'
import { DashboardPage } from './dashboard-page.ts'

export class LoginPage {
  readonly username: Locator
  readonly password: Locator
  readonly signInButton: Locator
  readonly error: Locator

  constructor(private readonly page: Page) {
    this.username = page.getByLabel('Username')
    this.password = page.getByLabel('Password')
    this.signInButton = page.getByRole('button', { name: 'Sign in' })
    this.error = page.getByRole('alert')
  }

  async open(): Promise<LoginPage> {
    await this.page.goto('/login')
    return this
  }

  async submit(username: string, password: string): Promise<void> {
    await this.username.fill(username)
    await this.password.fill(password)
    await this.signInButton.click()
  }

  /** Opens the sign-in page, signs in, and returns the dashboard. */
  async signInAs(user: User): Promise<DashboardPage> {
    await this.open()
    await this.submit(user.username, user.password)
    return new DashboardPage(this.page)
  }
}
