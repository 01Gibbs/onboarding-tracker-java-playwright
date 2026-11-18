import { expect, test } from '@playwright/test'

// Lesson 04 starter: prove that every test gets a fresh browser context.
// TODO exercise 2: make the tests in this file run in order (see the lesson), only for this demo.

test('the first test signs in', async ({ page, context }) => {
  test.skip(true, 'TODO lesson 04, exercise 2: sign in, expect a JSESSIONID cookie')
})

test('the second test starts signed out', async ({ page, context }) => {
  test.skip(true, 'TODO lesson 04, exercise 2: expect no cookies, and /customers to send you to /login')
})
