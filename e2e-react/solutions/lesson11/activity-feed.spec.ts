import { expect, test } from '../support/fixtures.ts'
import type { DashboardPage } from '../pages/dashboard-page.ts'

// Lesson 11: the dashboard loads "Recent activity" from GET /api/activity. Routing that request lets us test
// states that are hard to produce with real data.

const ACTIVITY = '**/api/activity*'

// A mock must look like the real response. This one carries the raw fields (type, amount) AND the display text
// the API adds (typeLabel, amountDisplay), so it suits any front end, whichever fields it reads.
const TWO_ENTRIES = [
  {
    id: 1, accountId: 1, accountNumber: '12345678', accountNickname: 'Mocked Current', description: 'Mocked salary',
    type: 'DEPOSIT', typeLabel: 'Deposit', amount: 1234.5, amountDisplay: '+£1,234.50',
  },
  {
    id: 2, accountId: 1, accountNumber: '12345678', accountNickname: 'Mocked Current', description: 'Mocked coffee',
    type: 'WITHDRAWAL', typeLabel: 'Withdrawal', amount: 20, amountDisplay: '-£20.00',
  },
]

let dashboard: DashboardPage

test.beforeEach(async ({ signedIn }) => {
  dashboard = signedIn
  await expect(dashboard.heading).toBeVisible()
})

test('real activity is shown by default', async ({ page }) => {
  // No mocking: wait for the real response while reloading
  const response = page.waitForResponse(ACTIVITY)
  await page.reload()

  expect((await response).status()).toBe(200)
  await expect(dashboard.activityItems.first()).toBeVisible()
})

test('shows mocked entries', async ({ page }) => {
  await page.route(ACTIVITY, route => route.fulfill({ json: TWO_ENTRIES }))

  await dashboard.open()

  await expect(dashboard.activityItems).toHaveCount(2)
  await expect(dashboard.activityItems.first()).toContainText('Deposit · Mocked salary')
  await expect(dashboard.activityItems.first()).toContainText('+£1,234.50')
  await expect(dashboard.activityItems.nth(1)).toContainText('-£20.00')
})

test('shows an empty state', async ({ page }) => {
  await page.route(ACTIVITY, route => route.fulfill({ json: [] }))

  await dashboard.open()

  await expect(dashboard.activityPanel).toContainText('No activity yet.')
})

test('recovers from a server error after Refresh', async ({ page }) => {
  await page.route(ACTIVITY, route => route.fulfill({ status: 500 }))
  await dashboard.open()
  await expect(dashboard.activityPanel.getByRole('alert')).toContainText('Could not load recent activity')

  await page.unroute(ACTIVITY)
  await dashboard.refreshActivity()

  await expect(dashboard.activityItems.first()).toBeVisible()
})

test('handles a network failure', async ({ page }) => {
  await page.route(ACTIVITY, route => route.abort('connectionrefused'))

  await dashboard.open()

  await expect(dashboard.activityPanel.getByRole('alert')).toBeVisible()
})

test('modifies the real response', async ({ page }) => {
  await page.route(ACTIVITY, async route => {
    const response = await route.fetch()
    const entries: { description: string }[] = await response.json()
    await route.fulfill({ response, json: entries.map(entry => ({ ...entry, description: 'PATCHED' })) })
  })

  await dashboard.open()

  await expect(dashboard.activityItems.first()).toContainText('PATCHED')
})

test('Refresh calls the API again, once', async ({ page }) => {
  await expect(dashboard.activityItems.first()).toBeVisible() // the first load has finished
  let calls = 0
  page.on('request', request => {
    if (request.url().includes('/api/activity')) {
      calls++
    }
  })

  const response = page.waitForResponse(ACTIVITY)
  await dashboard.refreshActivity()

  expect((await response).ok()).toBeTruthy()
  expect(calls).toBe(1)
})
