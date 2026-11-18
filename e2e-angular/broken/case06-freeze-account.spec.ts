import { expect, test } from '../solutions/support/fixtures.ts'

test.use({ storageState: 'playwright/.auth/broken-auditor.json' })

test('a teller freezes an account', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('10.00')
  await account.open(id)

  await account.click('Freeze account')

  await expect(account.status).toHaveText('Frozen')
})
