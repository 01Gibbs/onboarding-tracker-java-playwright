import { expect, test } from '@playwright/test'

// Lesson 08 (part 2) starter: use your LedgerApi (support/ledger-api.ts). Sign it in in beforeEach
// (LedgerApi.signIn(playwright.request, baseURL!, TELLER)) and dispose of it in afterEach.

test('new accounts start active with a zero balance', async () => {
  test.skip(true, 'TODO lesson 08, exercise 7')
})

test('deposits and withdrawals update the balance', async () => {
  test.skip(true, 'TODO lesson 08, exercise 8')
})

test('you cannot withdraw more than the balance', async () => {
  test.skip(true, 'TODO lesson 08, exercise 9')
})

test('transfers move money between accounts', async () => {
  test.skip(true, 'TODO lesson 08, exercise 10')
})

test('frozen accounts reject deposits', async () => {
  test.skip(true, 'TODO lesson 08, exercise 11')
})

test('accounts with money cannot be closed', async () => {
  test.skip(true, 'TODO lesson 08, exercise 12')
})
