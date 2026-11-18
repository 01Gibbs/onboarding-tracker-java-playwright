import { expect, test } from '@playwright/test'
import { TELLER_STATE } from '../support/auth-state.ts'

// Lesson 10 (part 2): the teller's state was made through the API, with no browser. It works the same.
test.use({ storageState: TELLER_STATE })

test('the teller is already signed in', async ({ page }) => {
  await page.goto('/customers')

  await expect(page.getByTestId('current-user')).toContainText('Terry Teller (Teller)')
})
