import { expect, test } from '../solutions/support/fixtures.ts'
import { unique } from '../solutions/support/test-data.ts'

test('refreshing shows the latest activity', async ({ page, api, signedIn: dashboard }) => {
  await expect(dashboard.activityItems.first()).toBeVisible()
  const description = `Refresh check ${unique()}`
  const { id } = await api.accountWithBalance('0')
  await api.raw.post(`/api/accounts/${id}/deposits`, { data: { amount: 1, description } })
  // On a busy day the activity API takes a second to answer
  await page.route('**/api/activity*', async route => {
    await new Promise(resolve => setTimeout(resolve, 1_000))
    await route.continue()
  })

  await dashboard.refreshActivity()

  expect(await dashboard.activityItems.allTextContents(), 'expected the new deposit').toContainEqual(expect.stringContaining(description))
})
