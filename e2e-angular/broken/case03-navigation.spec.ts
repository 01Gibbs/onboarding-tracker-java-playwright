import { expect, test } from '../solutions/support/fixtures.ts'

test('a teller opens the customers from the dashboard', async ({ page, signedIn }) => {
  await expect(signedIn.heading).toBeVisible()

  await page.getByRole('link', { name: 'Customers' }).click()

  await expect(page.getByRole('heading', { name: 'Customers' })).toBeVisible()
})
