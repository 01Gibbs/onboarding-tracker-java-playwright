import { expect, test } from '../support/fixtures.ts'

// Lesson 12: a small, fast smoke suite. The tag lets it run on its own:
//   npm run solutions:react -- --grep @smoke     (or solutions:angular)
test.describe('smoke', { tag: '@smoke' }, () => {
  test('a teller can sign in and see the dashboard', async ({ signedIn }) => {
    await expect(signedIn.heading).toBeVisible()
  })

  test('the customer list loads', async ({ signedIn, customersPage }) => {
    await expect(signedIn.heading).toBeVisible()

    await customersPage.open()

    await expect(customersPage.rows.first()).toBeVisible()
  })
})
