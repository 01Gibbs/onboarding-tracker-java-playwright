import { expect, test } from '@playwright/test'

// Lesson 04: a hook that runs before every test in this file, and relative URLs resolved against the
// baseURL in playwright.config.ts.

test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()
  // Fail here, with a clear message, if sign-in didn't work
  await expect(page.getByTestId('current-user')).toBeVisible()
})

test('the dashboard shows the summary cards', async ({ page }) => {
  await expect(page.getByTestId('stat-customers')).toContainText('Customers')
  await expect(page.getByTestId('stat-active-accounts')).toContainText('Active accounts')
  await expect(page.getByTestId('stat-total-balance')).toContainText('£')
})

test('the accounts page lists the seeded accounts', async ({ page }) => {
  await page.goto('/accounts')

  await expect(page.getByRole('link', { name: '10000001' })).toBeVisible()
})
