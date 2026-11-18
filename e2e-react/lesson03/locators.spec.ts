import { expect, test } from '@playwright/test'

// Lesson 03 starter. Sign-in is provided; each test's goal is in lessons/react/03-locators.md.

test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByTestId('current-user')).toBeVisible()
})

test('navigate using the main navigation', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 1')
})

test('search for a customer by placeholder', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 2')
})

test('filter table rows and chain locators', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 3')
})

test('find by text and exact matching', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 4')
})

test('every frozen account shows a frozen badge', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 5')
})

test('open an account from the customer page', async ({ page }) => {
  test.skip(true, 'TODO lesson 03, exercise 6')
})
