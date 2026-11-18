import { expect, test } from '@playwright/test'

// Lesson 02 starter. The actions for the first test are done for you; you write the assertions.

test('a teller can sign in', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()

  // TODO exercise 1: expect
  //  - the URL to end with "/"
  //  - the "Dashboard" heading to be visible
  //  - getByTestId('current-user') to contain "Terry Teller"
  test.skip(true, 'TODO lesson 02, exercise 1')
})

test('a wrong password shows an error', async ({ page }) => {
  // TODO exercise 2: sign in with a wrong password, then expect
  //  - the alert (getByRole('alert')) to have exactly the text "Invalid username or password"
  //  - the URL to still end with /login
  //  - Username to still have the value "teller", and Password to be empty
  test.skip(true, 'TODO lesson 02, exercise 2')
})

test('a locked user cannot sign in', async ({ page }) => {
  // TODO exercise 3: sign in as locked / locked-pass by pressing Enter in the password field.
  //  Expect the alert to mention "locked".
  test.skip(true, 'TODO lesson 02, exercise 3')
})

test('a signed-in user can sign out', async ({ page }) => {
  // TODO exercise 4: sign in, click "Sign out", expect the flash message, then prove /customers
  //  sends you back to the sign-in page.
  test.skip(true, 'TODO lesson 02, exercise 4')
})
