import { expect, test } from '../support/fixtures.ts'

// Lesson 09: arrange with the API, act and assert in the UI (and double-check with the API). Custom fixtures
// hand each test a signed-in API client and page objects; every test makes its own account.

test.beforeEach(async ({ signedIn }) => {
  await expect(signedIn.heading).toBeVisible()
})

test('a deposit increases the balance', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('100.00')
  await account.open(id)
  await expect(account.balance).toHaveText('£100.00')

  await account.deposit('25.50')

  await expect(account.flash).toHaveText('Deposit of £25.50 completed')
  await expect(account.balance).toHaveText('£125.50')
  await expect(account.transactionRows.first()).toContainText('Deposit')
  await expect(account.transactionRows.first()).toContainText('+£25.50')
})

test('assertions wait for the slow server, with no sleeps', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('100.00')
  await account.open(id)

  // The server takes ~750 ms on purpose. toHaveText retries until the new balance appears.
  await account.withdraw('10')

  await expect(account.balance).toHaveText('£90.00')
})

test('the form shows "Processing…" while the server works', { tag: '@spa' }, async ({ api, accountPage: account }) => {
  // @spa: a single-page app updates the page while the request runs. (A full-page form submission, as on the
  // Thymeleaf front end, navigates away instead, and Playwright waits for the new page.)
  const { id } = await api.accountWithBalance('100.00')
  await account.open(id)

  await account.deposit('1')

  await expect(account.moveMoney.getByRole('button', { name: 'Processing…' })).toBeDisabled()
  await expect(account.submit).toBeEnabled()
  await expect(account.balance).toHaveText('£101.00')
})

test('withdrawing too much shows an error and changes nothing', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('50.00')
  await account.open(id)

  await account.withdraw('50.01')

  await expect(account.amount).toHaveAccessibleDescription('Insufficient funds. Available balance is £50.00')
  await expect(account.amount).toHaveAttribute('aria-invalid', 'true')
  await expect(account.amount).toHaveValue('50.01')
  await expect(account.balance).toHaveText('£50.00')
  expect(await api.balanceOf(id)).toBe(50)
})

test('amounts are validated', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('0')
  await account.open(id)

  await account.deposit('12.345')

  await expect(account.amount).toHaveAccessibleDescription('Amount can have at most 2 decimal places')
})

test('the "To account" field only appears for transfers', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('0')
  await account.open(id)

  await expect(account.amount).toBeVisible() // the form has loaded, so "hidden" means hidden
  await expect(account.toAccount).toBeHidden()
  await account.chooseType('Transfer')
  await expect(account.toAccount).toBeVisible()
  await account.chooseType('Deposit')
  await expect(account.toAccount).toBeHidden()
})

test('a transfer updates both accounts', async ({ api, accountPage: account }) => {
  const from = await api.accountWithBalance('300.00')
  const to = await api.accountWithBalance('0')
  await account.open(from.id)

  await account.transfer('120.00', to.id)

  await expect(account.flash).toContainText('Transfer of £120.00 to')
  await expect(account.flash).toContainText(to.accountNumber)
  await expect(account.balance).toHaveText('£180.00')
  await expect(account.transactionRows.first()).toContainText(`to ${to.accountNumber}`)

  // Check the receiving side in the UI and through the API
  await account.open(to.id)
  await expect(account.balance).toHaveText('£120.00')
  expect(await api.balanceOf(to.id)).toBe(120)
})

test('a transfer needs a destination', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('10.00')
  await account.open(id)

  await account.chooseType('Transfer')
  await account.amount.fill('5')
  await account.submit.click()

  await expect(account.toAccount).toBeVisible()
  await expect(account.toAccount).toHaveAttribute('aria-invalid', 'true')
  await expect(account.toAccount).toHaveAccessibleDescription('Choose an account to transfer to')
})

test('frozen accounts cannot move money', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('10.00')
  await api.setAccountStatus(id, 'FROZEN')

  await account.open(id)

  await expect(account.status).toHaveText('Frozen') // wait for the page before asserting what's absent
  await expect(account.moveMoney).toBeHidden()
})

test('freeze, unfreeze and close through the buttons', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('0')
  await account.open(id)

  await account.click('Freeze account')
  await expect(account.status).toHaveText('Frozen')
  await account.click('Unfreeze account')
  await expect(account.status).toHaveText('Active')

  await account.clickAndConfirm('Close account')
  await expect(account.status).toHaveText('Closed')
  await expect(account.flash).toContainText('is now closed')
})

test('filter the transaction history', async ({ api, accountPage: account }) => {
  const { id } = await api.accountWithBalance('100.00')
  await api.withdraw(id, '30.00', 'Cash')
  await account.open(id)
  await expect(account.transactionRows).toHaveCount(2)

  await account.filterTransactions('Withdrawal')

  await expect(account.transactionRows).toHaveCount(1)
  await expect(account.transactionRows).toContainText('Cash')
})
