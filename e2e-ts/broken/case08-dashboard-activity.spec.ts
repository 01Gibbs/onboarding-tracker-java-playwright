import { expect, test } from '../solutions/support/fixtures.ts'

test('refreshing shows recent activity', async ({ signedIn: dashboard }) => {
  await expect(dashboard.heading).toBeVisible()

  await dashboard.refreshActivity()

  expect(await dashboard.activityItems.count(), 'expected some recent activity').toBeGreaterThan(0)
})
