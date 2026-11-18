import { expect, test } from '@playwright/test'

// Lesson 08 (part 1) starter: the JSON API through the `request` fixture. Get a token in beforeEach.

test('requests without a token are rejected', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 1')
})

test('list customers and filter by status', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 2')
})

test('create, read, update and delete a customer', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 3')
})

test('validation errors list each invalid field', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 4')
})

test('a duplicate email is a conflict', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 5')
})

test('auditors can read but not write', async ({ request }) => {
  test.skip(true, 'TODO lesson 08, exercise 6')
})
