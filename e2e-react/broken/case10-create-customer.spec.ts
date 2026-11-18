import { expect, test } from '../solutions/support/fixtures.ts'

test('a teller creates a customer', async ({ signedIn }) => {
  const form = await (await signedIn.nav.customers()).newCustomer()
  await form.fill('Quinn', 'Example', 'quinn.example@example.test')

  const customer = await form.save()

  await expect(customer.flash).toHaveText('Customer Quinn Example created')
})
