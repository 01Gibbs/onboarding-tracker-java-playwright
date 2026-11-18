import { expect, test } from '../support/fixtures.ts'

// Lesson 11 starter: route GET /api/activity to test the dashboard's "Recent activity" states.
// Finish your DashboardPage's activity locators and refreshActivity() first.

test.beforeEach(async ({ page }) => {
  // TODO lesson 11: sign in as teller (your LoginPage), or use your signedIn fixture from lesson 09
})

test('real activity is shown by default', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 1')
})

test('shows mocked entries', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 2')
})

test('shows an empty state', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 3')
})

test('recovers from a server error after Refresh', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 4')
})

test('handles a network failure', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 5')
})

test('modifies the real response', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 6')
})

test('Refresh calls the API again, once', async ({ page }) => {
  test.skip(true, 'TODO lesson 11, exercise 7')
})
