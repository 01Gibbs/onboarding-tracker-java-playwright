import type { Page } from '@playwright/test'
import { CustomerDetailPage } from '../pages/customer-detail-page.ts'
import { expect, test } from '../support/fixtures.ts'
import { uniqueEmail, uniqueLastName } from '../support/test-data.ts'

// Lesson 14 capstone: one realistic journey through the whole product, driven through the UI and checked at
// the end through the API.

/** Reads the id from URLs like /customers/42 or /accounts/7. */
function idFromUrl(page: Page, collection: 'customers' | 'accounts'): number {
  const match = new RegExp(`/${collection}/(\\d+)$`).exec(new URL(page.url()).pathname)
  if (!match) {
    throw new Error(`Expected a /${collection}/{id} URL but was ${page.url()}`)
  }
  return Number(match[1])
}

test('onboard, fund and tidy up a customer', async ({ page, signedIn, api, accountPage }) => {
  const lastName = uniqueLastName()

  await test.step('create the customer', async () => {
    const form = await (await signedIn.nav.customers()).newCustomer()
    await form.fill('Priya', lastName, uniqueEmail('priya'))
    const customer = await form.save()
    await expect(customer.status).toHaveText('Active')
    await expect(customer.heading).toContainText(lastName)
    await expect(customer.accountRows).toHaveCount(0)
  })
  const customerId = idFromUrl(page, 'customers')
  const customer = new CustomerDetailPage(page)

  const spendingId = await test.step('open a current account', async () => {
    const spending = await (await customer.openAccount()).open('Current', 'Spending')
    await expect(spending.flash).toContainText('opened')
    return idFromUrl(page, 'accounts')
  })
  const goalsId = await test.step('open a savings account', async () => {
    await (await (await customer.open(customerId)).openAccount()).open('Savings', 'Goals')
    await expect(accountPage.flash).toContainText('opened')
    return idFromUrl(page, 'accounts')
  })

  await test.step('fund the current account and save some', async () => {
    await accountPage.open(spendingId)
    await accountPage.deposit('500.00')
    await expect(accountPage.balance).toHaveText('£500.00')
    await accountPage.transfer('200.00', goalsId)
    await expect(accountPage.balance).toHaveText('£300.00')
  })

  await test.step('the customer page sums up both accounts', async () => {
    await customer.open(customerId)
    await expect(customer.accountRows).toHaveCount(2)
    await expect(customer.accountRows.filter({ hasText: 'Spending' })).toContainText('£300.00')
    await expect(customer.accountRows.filter({ hasText: 'Goals' })).toContainText('£200.00')
  })

  await test.step('savings cannot close while it holds money, then can', async () => {
    await accountPage.open(goalsId)
    await accountPage.clickAndConfirm('Close account')
    await expect(accountPage.flash).toHaveText('Account balance must be zero before it can be closed')

    await accountPage.transfer('200.00', spendingId)
    await expect(accountPage.balance).toHaveText('£0.00')
    await accountPage.clickAndConfirm('Close account')
    await expect(accountPage.status).toHaveText('Closed')
  })

  await test.step('the API agrees', async () => {
    expect(await api.balanceOf(spendingId)).toBe(500)
    expect((await api.account(goalsId)).status).toBe('CLOSED')
    expect(await api.transactions(spendingId)).toHaveLength(3) // deposit, transfer out, transfer in
  })
})
