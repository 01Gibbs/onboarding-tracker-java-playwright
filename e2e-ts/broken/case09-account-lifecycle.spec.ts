import type { Account } from '../solutions/support/ledger-api.ts'
import { expect, test } from '../solutions/support/fixtures.ts'

// These two tests run in the order they are written.
test.describe.configure({ mode: 'default' })

let account: Account | undefined

test('deposit into the account', async ({ api, signedIn, accountPage }) => {
  await expect(signedIn.heading).toBeVisible()
  await accountPage.open(account!.id)

  await accountPage.deposit('10.00')

  await expect(accountPage.balance).toHaveText('£10.00')
})

test('open an account', async ({ api }) => {
  account = await api.accountWithBalance('0')

  expect(account.status).toBe('ACTIVE')
})
