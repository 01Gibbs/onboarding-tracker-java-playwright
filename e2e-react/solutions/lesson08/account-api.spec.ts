import { expect, test } from '@playwright/test'
import { LedgerApi } from '../support/ledger-api.ts'
import { TELLER } from '../support/users.ts'

// Lesson 08 (part 2): the same ideas behind a small typed client, LedgerApi, which later lessons use to
// arrange data. Money arrives as JSON numbers: compare numbers with numbers (59.75), not with strings.

let api: LedgerApi

test.beforeEach(async ({ playwright, baseURL }) => {
  api = await LedgerApi.signIn(playwright.request, baseURL!, TELLER)
})

test.afterEach(async () => {
  await api.dispose()
})

test('new accounts start active with a zero balance', async () => {
  const customer = await api.createCustomer()

  const account = await api.openAccount(customer.id, 'SAVINGS', 'Rainy Day')

  expect(account.status).toBe('ACTIVE')
  expect(account.balance).toBe(0)
  expect(account.accountNumber).toMatch(/^\d{8}$/)
})

test('deposits and withdrawals update the balance', async () => {
  const { id } = await api.accountWithBalance('100.00')

  const withdrawal = await api.raw.post(`/api/accounts/${id}/withdrawals`, { data: { amount: 40.25 } })

  expect(withdrawal.status()).toBe(201)
  expect(await api.balanceOf(id)).toBe(59.75)
  const history = await api.transactions(id)
  expect(history).toHaveLength(2)
  expect(history[0].type).toBe('WITHDRAWAL') // newest first
})

test('you cannot withdraw more than the balance', async () => {
  const { id } = await api.accountWithBalance('10.00')

  const response = await api.raw.post(`/api/accounts/${id}/withdrawals`, { data: { amount: 10.01 } })

  expect(response.status()).toBe(422)
  expect((await response.json()).message).toBe('Insufficient funds. Available balance is £10.00')
  expect(await api.balanceOf(id)).toBe(10)
})

test('transfers move money between accounts', async () => {
  const from = await api.accountWithBalance('250.00')
  const to = await api.accountWithBalance('0')

  const response = await api.raw.post('/api/transfers', {
    data: { fromAccountId: from.id, toAccountId: to.id, amount: 75, description: 'Rent' },
  })

  await expect(response).toBeOK()
  expect(await api.balanceOf(from.id)).toBe(175)
  expect(await api.balanceOf(to.id)).toBe(75)
  const [outgoing] = await api.transactions(from.id)
  const [incoming] = await api.transactions(to.id)
  expect(incoming.transferId).toBe(outgoing.transferId)
})

test('frozen accounts reject deposits', async () => {
  const { id } = await api.accountWithBalance('5.00')
  await api.setAccountStatus(id, 'FROZEN')

  const response = await api.raw.post(`/api/accounts/${id}/deposits`, { data: { amount: 1 } })

  expect(response.status()).toBe(422)
})

test('accounts with money cannot be closed', async () => {
  const { id } = await api.accountWithBalance('1.00')

  const response = await api.raw.patch(`/api/accounts/${id}`, { data: { status: 'CLOSED' } })

  expect(response.status()).toBe(409)
})
