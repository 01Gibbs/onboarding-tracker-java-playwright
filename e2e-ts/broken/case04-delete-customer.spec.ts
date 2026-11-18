import { CustomerDetailPage } from '../solutions/pages/customer-detail-page.ts'
import { expect, test } from '../solutions/support/fixtures.ts'

test('a deleted customer disappears', async ({ page, api, signedIn }) => {
  const { id } = await api.createCustomer()
  await expect(signedIn.heading).toBeVisible()
  const customer = await new CustomerDetailPage(page).open(id)

  await page.getByRole('button', { name: 'Delete customer' }).click()

  await expect(customer.flash).toContainText('deleted')
})
