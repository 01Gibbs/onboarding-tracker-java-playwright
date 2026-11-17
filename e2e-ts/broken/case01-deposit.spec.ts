import { expect, test } from '../solutions/support/fixtures.ts'

test('a deposit shows the new balance', async ({ api, signedIn, accountPage: account }) => {
  const { id } = await api.accountWithBalance('1000.00')
  await expect(signedIn.heading).toBeVisible()
  await account.open(id)

  await account.deposit('250.00')

  await expect(account.balance).toHaveText('£1250.00')
})
