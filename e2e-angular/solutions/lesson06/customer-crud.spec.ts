import { expect, test, type Page } from '@playwright/test'
import { uniqueEmail, uniqueLastName } from '../support/test-data.ts'

// Lesson 06: create, read, update and delete customers through the UI, with validation and dialogs.

test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('teller')
  await page.getByLabel('Password').fill('teller-pass')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByTestId('current-user')).toBeVisible()
})

/** Creates a customer through the form, leaves the browser on their page, and returns their full name. */
async function createCustomer(page: Page, firstName: string): Promise<string> {
  const lastName = uniqueLastName()
  await page.goto('/customers/new')
  await page.getByLabel('First name').fill(firstName)
  await page.getByLabel('Last name').fill(lastName)
  await page.getByLabel('Email').fill(uniqueEmail(firstName.toLowerCase()))
  await page.getByRole('button', { name: 'Save customer' }).click()
  await expect(page.getByTestId('flash-message')).toContainText('created')
  return `${firstName} ${lastName}`
}

test('a created customer can be found by search', async ({ page }) => {
  const name = await createCustomer(page, 'Hana')

  await page.goto('/customers')
  await page.getByRole('searchbox', { name: 'Search customers' }).fill(name.split(' ')[1])
  await page.getByRole('button', { name: 'Search' }).click()

  const rows = page.getByTestId('customer-row')
  await expect(rows).toHaveCount(1)
  await expect(rows).toContainText(name)
  await expect(rows.getByTestId('status-badge')).toHaveText('Active')
})

test('editing a customer updates their details', async ({ page }) => {
  await createCustomer(page, 'Ivan')
  const newEmail = uniqueEmail('ivan.updated')

  await page.getByRole('link', { name: 'Edit' }).click()
  await expect(page.getByLabel('First name')).toHaveValue('Ivan') // the form is pre-filled
  await page.getByLabel('Email').fill(newEmail)
  await page.getByLabel('Phone').fill('+44 7700 900123')
  await page.getByRole('button', { name: 'Save customer' }).click()

  await expect(page.getByTestId('flash-message')).toContainText('updated')
  await expect(page.getByText(newEmail)).toBeVisible()
  await expect(page.getByText('+44 7700 900123')).toBeVisible()
})

test('suspending and reactivating a customer', async ({ page }) => {
  await createCustomer(page, 'Jade')
  const badge = page.getByRole('heading', { level: 1 }).getByTestId('status-badge')

  await page.getByRole('button', { name: 'Suspend customer' }).click()
  await expect(badge).toHaveText('Suspended')
  await expect(page.getByRole('link', { name: 'Open account' })).toBeHidden()

  await page.getByRole('button', { name: 'Reactivate customer' }).click()
  await expect(badge).toHaveText('Active')
})

test('deleting a customer after confirming', async ({ page }) => {
  const name = await createCustomer(page, 'Kofi')

  // Register the handler BEFORE the click. Check the dialog here, but act on it after the click resolves:
  // a failed expect inside the handler would leave the dialog open.
  const dialog = page.waitForEvent('dialog')
  const click = page.getByRole('button', { name: 'Delete customer' }).click()
  const confirm = await dialog
  expect(confirm.type()).toBe('confirm')
  expect(confirm.message()).toBe(`Delete customer ${name}? This cannot be undone.`)
  await confirm.accept()
  await click

  await expect(page).toHaveURL(/\/customers$/)
  await expect(page.getByTestId('flash-message')).toHaveText(`Customer ${name} deleted`)
  await expect(page.getByTestId('customer-row').first()).toBeVisible() // the list has loaded...
  await expect(page.getByRole('link', { name })).toHaveCount(0) // ...so its absence means something
})

test('dismissing the confirmation keeps the customer', async ({ page }) => {
  const name = await createCustomer(page, 'Lena')

  page.once('dialog', dialog => dialog.dismiss())
  await page.getByRole('button', { name: 'Delete customer' }).click()

  // Nothing happened: still on the same page, still showing the "created" message
  await expect(page).toHaveURL(/\/customers\/\d+$/)
  await expect(page.getByRole('heading', { level: 1 })).toContainText(name)
  await expect(page.getByTestId('flash-message')).toContainText('created')
})

test('customers with accounts cannot be deleted', async ({ page }) => {
  await page.goto('/customers')
  await page.getByRole('link', { name: 'Alice Archer' }).click()

  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: 'Delete customer' }).click()

  await expect(page.getByRole('alert')).toHaveText('Customer cannot be deleted while they have accounts')
})

test.describe('validation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/customers/new')
  })

  test('required fields are reported', async ({ page }) => {
    await page.getByRole('button', { name: 'Save customer' }).click()

    await expect(page.getByTestId('error-summary')).toBeVisible()
    // Each message is the field's accessible description (aria-describedby)
    await expect(page.getByLabel('First name')).toHaveAccessibleDescription('First name is required')
    await expect(page.getByLabel('Last name')).toHaveAccessibleDescription('Last name is required')
    await expect(page.getByLabel('First name')).toHaveAttribute('aria-invalid', 'true')
  })

  test('an invalid email is rejected', async ({ page }) => {
    await page.getByLabel('First name').fill('Max')
    await page.getByLabel('Last name').fill('Mustermann')
    await page.getByLabel('Email').fill('not-an-email')

    await page.getByRole('button', { name: 'Save customer' }).click()

    await expect(page.getByLabel('Email')).toHaveAccessibleDescription('Enter a valid email address')
    await expect(page.getByLabel('First name')).toHaveValue('Max') // what the user typed is kept
  })

  test('a duplicate email is rejected', async ({ page }) => {
    await page.getByLabel('First name').fill('Alice')
    await page.getByLabel('Last name').fill('Again')
    await page.getByLabel('Email').fill('ALICE.ARCHER@example.test')

    await page.getByRole('button', { name: 'Save customer' }).click()

    await expect(page.getByLabel('Email')).toHaveAccessibleDescription('A customer with this email already exists')
  })
})
