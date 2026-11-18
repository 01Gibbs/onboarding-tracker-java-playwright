import { expect, test } from '../solutions/support/fixtures.ts'

test('the dashboard shows an empty state when there is no activity', async ({ page, signedIn: dashboard }) => {
  await expect(dashboard.heading).toBeVisible()
  await page.route('**/api/activity', route => route.fulfill({ json: [] }))

  await dashboard.open()

  await expect(dashboard.activityPanel).toContainText('No activity yet.')
})
