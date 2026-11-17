import { expect, test } from '@playwright/test'

// Lesson 10 starter: sign in once in a setup project, save the storage state, start each test from it.
// TODO: test.use({ storageState: ... }) once your auth.setup.ts saves the auditor's state.

test('the auditor lands on the dashboard without signing in', async ({ page }) => {
  test.skip(true, 'TODO lesson 10, exercise 2')
})

test('the auditor does not see editing controls', async ({ page }) => {
  test.skip(true, 'TODO lesson 10, exercise 3')
})

test('the auditor is blocked from changes even without the buttons', async ({ page }) => {
  test.skip(true, 'TODO lesson 10, exercise 4')
})
