import { expect, test } from '@playwright/test'
import { AUDITOR_STATE } from '../support/auth-state.ts'

// Lesson 10: every test in this file starts signed in as the auditor, from the state auth.setup.ts saved.
test.use({ storageState: AUDITOR_STATE })

test('the auditor lands on the dashboard without signing in', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByTestId('current-user')).toContainText('Alex Auditor (Auditor)')
})

test('the auditor does not see editing controls', async ({ page }) => {
  // Wait for something that IS there before asserting what isn't: a count of 0 passes instantly while a
  // single-page app is still loading.
  await page.goto('/customers')
  await expect(page.getByTestId('customer-row').first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'New customer' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Alice Archer' }).click()
  await expect(page.getByRole('heading', { name: 'Alice Archer' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Delete customer' })).toHaveCount(0)

  await page.goto('/accounts/1')
  await expect(page.getByTestId('transaction-row').first()).toBeVisible() // the auditor can still read history
  await expect(page.getByRole('region', { name: 'Move money' })).toHaveCount(0)
})

test('the auditor is blocked from changes even without the buttons', async ({ page }) => {
  // Don't just trust the UI: send the request a button would have sent. page.request shares the page's
  // cookies, so this goes to the server as the signed-in auditor.
  const response = await page.request.patch('/api/accounts/1', { data: { status: 'FROZEN' } })

  expect(response.status()).toBe(403)
})
