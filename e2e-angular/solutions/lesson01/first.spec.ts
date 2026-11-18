import { expect, test } from '@playwright/test'

// Lesson 01: a first test. `page` is a fixture: a fresh tab in a fresh browser context for every test.

test('the sign-in page has the expected title', async ({ page }) => {
  await page.goto('/login')

  await expect(page).toHaveTitle('Sign in · Ledgerline')
})

test('the sign-in page shows its heading', async ({ page }) => {
  await page.goto('/login')

  await expect(page.getByRole('heading', { name: 'Sign in to Ledgerline' })).toBeVisible()
})

test('anonymous visitors are sent to the sign-in page', async ({ page }) => {
  await page.goto('/customers')

  await expect(page).toHaveURL(/\/login$/)
})
