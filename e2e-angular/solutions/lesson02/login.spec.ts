import { expect, test } from '@playwright/test'

// Lesson 02: actions (fill, click, press) and web-first assertions, using the sign-in flow.

test('a teller can sign in', async ({ page }) => {
  await page.goto('/login')

  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  await expect(page.getByTestId('current-user')).toContainText('Terry Teller')
})

test('a wrong password shows an error', async ({ page }) => {
  await page.goto('/login')

  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('not-the-password')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByRole('alert')).toHaveText('Invalid username or password')
  await expect(page).toHaveURL(/\/login$/)
  // The username is kept so the user only has to retype the password
  await expect(page.getByLabel('Username')).toHaveValue('teller')
  await expect(page.getByLabel('Password')).toBeEmpty()
})

test('a locked user cannot sign in', async ({ page }) => {
  await page.goto('/login')

  await page.getByLabel('Username').fill('locked')
  await page.getByLabel('Password').fill('locked-pass')
  await page.getByLabel('Password').press('Enter') // submitting with the keyboard works too

  await expect(page.getByRole('alert')).toContainText('locked')
})

test('a signed-in user can sign out', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await page.getByRole('button', { name: 'Sign out' }).click()

  await expect(page.getByTestId('flash-message')).toHaveText('You have been signed out')
  await page.goto('/customers')
  await expect(page).toHaveURL(/\/login$/)
})
