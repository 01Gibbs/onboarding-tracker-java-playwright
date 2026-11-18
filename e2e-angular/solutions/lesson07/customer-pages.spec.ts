import { expect, test } from '@playwright/test'
import { CustomersPage } from '../pages/customers-page.ts'
import { LoginPage } from '../pages/login-page.ts'
import { unique, uniqueEmail, uniqueLastName } from '../support/test-data.ts'
import { TELLER } from '../support/users.ts'

// Lesson 07: the lesson 06 scenarios with page objects. Tests read as intent; locators live in pages/.

test.beforeEach(async ({ page }) => {
  await new LoginPage(page).signInAs(TELLER)
})

test('create, search for and open a customer', async ({ page }) => {
  const lastName = uniqueLastName()

  const form = await (await new CustomersPage(page).open()).newCustomer()
  await form.fill('Nina', lastName, uniqueEmail('nina'))
  const customer = await form.save()
  await expect(customer.flash).toHaveText(`Customer Nina ${lastName} created`)

  const list = await customer.nav.customers()
  await list.search(lastName)
  await expect(list.rows).toHaveCount(1)

  const reopened = await list.openCustomer(`Nina ${lastName}`)
  await expect(reopened.heading).toContainText(`Nina ${lastName}`)
  await expect(reopened.detail('Phone')).toHaveText('Not provided')
})

test('delete a customer', async ({ page }) => {
  const lastName = uniqueLastName()
  const form = await (await new CustomersPage(page).open()).newCustomer()
  await form.fill('Omar', lastName, uniqueEmail('omar'))
  const customer = await form.save()
  await expect(customer.heading).toContainText(lastName)

  await customer.deleteAndConfirm()
  await expect(customer.flash).toContainText('deleted') // the delete has finished before we move on

  const list = await (await customer.nav.customers()).search(lastName)
  await expect(list.emptyMessage).toBeVisible()
})

test('validation errors are shown next to the fields', async ({ page }) => {
  const form = await (await new CustomersPage(page).open()).newCustomer()

  await form.fill('', '', 'nope')
  await form.saveExpectingErrors()

  await expect(form.errorSummary).toBeVisible()
  await expect(form.firstName).toHaveAccessibleDescription('First name is required')
  await expect(form.email).toHaveAccessibleDescription('Enter a valid email address')
})

test('searching for something that does not exist', async ({ page }) => {
  const list = await (await new CustomersPage(page).open()).search(`no-such-customer-${unique()}`)

  await expect(list.emptyMessage).toBeVisible()
  await expect(list.rows).toHaveCount(0)
})
