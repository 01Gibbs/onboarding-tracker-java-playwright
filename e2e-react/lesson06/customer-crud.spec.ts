import { expect, test, type Page } from '@playwright/test'

// Lesson 06 starter. Fill in the sign-in hook and the createCustomer helper first; each test's goal is in the lesson.

test.beforeEach(async ({ page }) => {
  // TODO lesson 06: sign in as teller, and expect current-user to be visible
})

/** Creates a customer through the form with unique data, leaves the browser on their page, returns their full name. */
async function createCustomer(page: Page, firstName: string): Promise<string> {
  throw new Error('TODO lesson 06')
}

test('a created customer can be found by search', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 1')
})

test('editing a customer updates their details', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 2')
})

test('suspending and reactivating a customer', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 3')
})

test('deleting a customer after confirming', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 4')
})

test('dismissing the confirmation keeps the customer', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 5')
})

test('customers with accounts cannot be deleted', async ({ page }) => {
  test.skip(true, 'TODO lesson 06, exercise 6')
})

test.describe('validation', () => {
  test('required fields are reported', async ({ page }) => {
    test.skip(true, 'TODO lesson 06, exercise 7')
  })

  test('an invalid email is rejected', async ({ page }) => {
    test.skip(true, 'TODO lesson 06, exercise 8')
  })

  test('a duplicate email is rejected', async ({ page }) => {
    test.skip(true, 'TODO lesson 06, exercise 9')
  })
})
