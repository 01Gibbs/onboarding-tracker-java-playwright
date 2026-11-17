import { expect, test } from '../solutions/support/fixtures.ts'

test('a transfer moves money to another account', async ({ api, signedIn, accountPage: account }) => {
  const from = await api.accountWithBalance('100.00')
  const to = await api.accountWithBalance('0')
  await expect(signedIn.heading).toBeVisible()
  await account.open(from.id)

  await account.toAccount.selectOption(String(to.id))
  await account.chooseType('Transfer')
  await account.amount.fill('40.00')
  await account.submit.click()

  await expect(account.balance).toHaveText('£60.00')
})
