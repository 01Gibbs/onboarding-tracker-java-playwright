import type { Route } from '@playwright/test'
import { expect, test } from '../support/fixtures.ts'

// Lesson 11 (part 2): watching the page WHILE a request is in flight. The route handler keeps hold of the
// request without answering it, so the browser waits while we assert on the loading state; then we let it go.

test('shows a loading state until activity arrives', async ({ page, signedIn: dashboard }) => {
  await expect(dashboard.activityItems.first()).toBeVisible() // let the first load finish before routing
  const held: Route[] = []
  await page.route('**/api/activity*', route => {
    held.push(route)
  })

  await dashboard.refreshActivity()

  await expect(dashboard.activityPanel.getByRole('status')).toHaveText('Loading activity…')
  await expect(dashboard.activityItems).toHaveCount(0)

  await expect.poll(() => held.length).toBe(1) // the route has caught the request: now it can be released
  for (const route of held) {
    await route.continue()
  }

  await expect(dashboard.activityPanel.getByRole('status')).toBeHidden()
  await expect(dashboard.activityItems.first()).toBeVisible()
})
