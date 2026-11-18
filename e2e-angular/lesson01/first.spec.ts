import { expect, test } from '@playwright/test'

// Lesson 01 starter. The first test is a complete worked example: read it, run it, then do the TODOs.
// A test that calls test.skip(...) is reported as SKIPPED: replace that line with your code.

test('the sign-in page has the expected title', async ({ page }) => {
  await page.goto('/login')

  await expect(page).toHaveTitle('Sign in · Ledgerline')
})

test('the sign-in page shows its heading', async ({ page }) => {
  await page.goto('/login')

  // TODO exercise 2: expect the heading "Sign in to Ledgerline" to be visible.
  //  Hint: page.getByRole('heading', { name: '...' })
  test.skip(true, 'TODO lesson 01, exercise 2')
})

test('anonymous visitors are sent to the sign-in page', async ({ page }) => {
  // TODO exercise 3: while signed out, go to /customers and expect the URL to end with /login.
  //  Hint: await expect(page).toHaveURL(/\/login$/)
  test.skip(true, 'TODO lesson 01, exercise 3')
})
