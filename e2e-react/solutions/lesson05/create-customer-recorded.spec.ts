import { expect, test } from '@playwright/test'
import { uniqueEmail, uniqueLastName } from '../support/test-data.ts'

// Lesson 05: a test that started as a codegen recording, then was tidied up. The raw recording was roughly:
//
//   await page.goto('http://localhost:5173/login');
//   await page.getByLabel('Username').click();
//   await page.getByLabel('Username').fill('teller');
//   await page.getByLabel('Username').press('Tab');
//   await page.getByLabel('Password').fill('teller-pass');
//   await page.getByRole('button', { name: 'Sign in' }).click();
//   await page.getByRole('link', { name: 'Customers', exact: true }).click();
//   await page.getByRole('link', { name: 'New customer' }).click();
//   await page.getByLabel('First name').click();
//   await page.getByLabel('First name').fill('Grace');
//   ...
//   await page.getByRole('button', { name: 'Save customer' }).click();
//   await expect(page.getByTestId('flash-message')).toContainText('Customer Grace Hopper created');
//
// Codegen added exact: true because the dashboard's "Customers" card (with its count) is also a link whose name contains
// "Customers". Scoping to the main navigation says what we mean. We also used relative URLs, removed the
// redundant click() and press('Tab'), made the data unique (so the test can run again), and asserted the
// outcome rather than just the message.

test('a teller creates a customer', async ({ page }) => {
  const lastName = uniqueLastName()
  const email = uniqueEmail('grace')

  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Customers' }).click()
  await page.getByRole('link', { name: 'New customer' }).click()
  await page.getByLabel('First name').fill('Grace')
  await page.getByLabel('Last name').fill(lastName)
  await page.getByLabel('Email').fill(email)
  await page.getByRole('button', { name: 'Save customer' }).click()

  await expect(page.getByTestId('flash-message')).toHaveText(`Customer Grace ${lastName} created`)
  await expect(page.getByRole('heading', { level: 1 })).toContainText(`Grace ${lastName}`)
  await expect(page.getByText(email)).toBeVisible()
})
