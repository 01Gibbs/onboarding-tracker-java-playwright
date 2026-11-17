import { expect, test as setup } from '@playwright/test'
import { AUDITOR, TELLER } from '../support/auth-state.ts'

// Lesson 10: sign in ONCE per role, before any test, and save the browser's storage state (the session cookie)
// to a file. Tests then start already signed in with test.use({ storageState }).
// playwright.config.ts runs this file in the "setup" project, which the other projects depend on.

setup('sign in as the auditor through the UI', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('auditor')
  await page.getByLabel('Password').fill('auditor-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByTestId('current-user')).toContainText('Alex Auditor')

  await page.context().storageState({ path: AUDITOR })
})

// Faster: no browser at all. The `request` fixture keeps cookies too, so sign in through the API and save its state.
setup('sign in as the teller through the API', async ({ request }) => {
  const response = await request.post('/api/auth/session', { data: { username: 'teller', password: 'teller-pass' } })
  expect(response.ok()).toBeTruthy()

  await request.storageState({ path: TELLER })
})
