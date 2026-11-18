import { expect, test } from '@playwright/test'

// Lesson 03: finding elements the way a user would - by role, label, text and test id.

test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByTestId('current-user')).toBeVisible()
})

test('navigate using the main navigation', async ({ page }) => {
  const mainNav = page.getByRole('navigation', { name: 'Main' })

  await mainNav.getByRole('link', { name: 'Customers' }).click()

  await expect(page.getByRole('heading', { name: 'Customers' })).toBeVisible()
  // The current page's link is marked with aria-current="page"
  await expect(mainNav.getByRole('link', { name: 'Customers' })).toHaveAttribute('aria-current', 'page')
})

test('search for a customer by placeholder', async ({ page }) => {
  await page.goto('/customers')

  await page.getByPlaceholder('Name or email').fill('archer')
  await page.getByRole('button', { name: 'Search' }).click()

  const rows = page.getByTestId('customer-row')
  await expect(rows).toHaveCount(1)
  await expect(rows).toContainText('Alice Archer')
  await expect(page.getByTestId('result-count')).toHaveText('1 customer')
})

test('filter table rows and chain locators', async ({ page }) => {
  await page.goto('/customers')

  const table = page.getByRole('table', { name: 'Customers' })
  const dansRow = table.getByRole('row').filter({ hasText: 'Dan Evans' })

  await expect(dansRow.getByTestId('status-badge')).toHaveText('Suspended')
  await expect(dansRow.getByRole('cell').first()).toHaveText('CUS-0004')
})

test('find by text and exact matching', async ({ page }) => {
  await page.goto('/customers')

  // Substring, case-insensitive by default...
  await expect(page.getByText('alice.archer@')).toBeVisible()
  // ...or exact when you need it
  await expect(page.getByText('Erin Foster', { exact: true })).toBeVisible()
})

test('every frozen account shows a frozen badge', async ({ page }) => {
  await page.goto('/accounts')

  await page.getByLabel('Status').selectOption('Frozen')
  await page.getByRole('button', { name: 'Apply filters' }).click()

  const rows = page.getByTestId('account-row')
  const notFrozen = rows.filter({ hasNot: page.getByText('Frozen', { exact: true }) })
  // Retried until true. A loop over rows.all() would check a snapshot, which on a single-page app can still be
  // the unfiltered list from before the click (React), or no list while the filtered one loads (Angular).
  // "No row that isn't Frozen" is also true of that empty moment, so check it again once the filtered list is
  // on screen: the unfiltered list always has an active account, so it can't pass the first check.
  await expect(notFrozen).toHaveCount(0)
  await expect(rows.filter({ hasText: 'Holiday Fund' })).toBeVisible()
  await expect(notFrozen).toHaveCount(0)
})

test('open an account from the customer page', async ({ page }) => {
  await page.goto('/customers')
  await page.getByRole('link', { name: 'Alice Archer' }).click()

  const accounts = page.getByRole('table', { name: 'Accounts' })
  const savingsRow = accounts.getByRole('row').filter({ has: page.getByText('Savings', { exact: true }) })
  await savingsRow.getByRole('link').click()

  await expect(page.getByRole('heading', { level: 1 })).toContainText('Rainy Day')
  await expect(page.getByTestId('account-number')).toHaveText('10000002')
})
