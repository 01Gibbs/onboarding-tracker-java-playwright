import { expect, test } from '@playwright/test'

// Lesson 04 starter. Relative URLs only: the base URL comes from playwright.config.ts.

test.beforeEach(async ({ page }) => {
  // TODO exercise 1: sign in as teller, then expect current-user to be visible
})

test('the dashboard shows the summary cards', async ({ page }) => {
  test.skip(true, 'TODO lesson 04, exercise 1 (test 1)')
})

test('the accounts page lists the seeded accounts', async ({ page }) => {
  test.skip(true, 'TODO lesson 04, exercise 1 (test 2)')
})
