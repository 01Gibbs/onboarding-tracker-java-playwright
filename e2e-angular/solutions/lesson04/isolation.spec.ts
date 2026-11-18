import { expect, test } from '@playwright/test'

// Lesson 04: every test gets a brand-new browser context (an isolated, incognito-like profile). The two tests
// run in order only to prove the point: the second does NOT see the first one's sign-in.
test.describe.configure({ mode: 'default' })

test('the first test signs in', async ({ page, context }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByTestId('current-user')).toBeVisible()
  expect((await context.cookies()).map(cookie => cookie.name)).toContain('JSESSIONID')
})

test('the second test starts signed out', async ({ page, context }) => {
  expect(await context.cookies()).toEqual([])

  await page.goto('/customers')

  await expect(page).toHaveURL(/\/login$/)
})
