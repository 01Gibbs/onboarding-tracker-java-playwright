import { expect, test } from '@playwright/test'

// Lesson 07 starter: the lesson 06 scenarios again, with page objects. No locators in this file: they live in
// pages/. Sign in with new LoginPage(page).signInAs(TELLER).

test('create, search for and open a customer', async ({ page }) => {
  test.skip(true, 'TODO lesson 07, exercise 3 (test 1)')
})

test('delete a customer', async ({ page }) => {
  test.skip(true, 'TODO lesson 07, exercise 3 (test 2)')
})

test('validation errors are shown next to the fields', async ({ page }) => {
  test.skip(true, 'TODO lesson 07, exercise 3 (test 3)')
})

test('searching for something that does not exist', async ({ page }) => {
  test.skip(true, 'TODO lesson 07, exercise 3 (test 4)')
})
