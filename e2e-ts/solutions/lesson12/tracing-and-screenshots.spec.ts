import { existsSync } from 'node:fs'
import { expect, test } from '../support/fixtures.ts'

// Lesson 12: collecting evidence. playwright.config.ts traces every test and keeps the trace if it fails
// (trace: 'retain-on-failure'). While that is on, Playwright owns tracing for every context in the test, so to
// trace by hand this file turns it off and starts and stops tracing itself.
// Open the result with: npx playwright show-trace <path>
test.use({ trace: 'off' })

test('record a trace and screenshots by hand', async ({ page, context, signedIn, accountPage }, testInfo) => {
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  await expect(signedIn.heading).toBeVisible()

  await accountPage.open(1)
  await expect(accountPage.balance).toBeVisible()
  const fullPage = testInfo.outputPath('account-full.png')
  const balance = testInfo.outputPath('account-balance.png')
  await page.screenshot({ path: fullPage, fullPage: true })
  await accountPage.balance.screenshot({ path: balance })

  const trace = testInfo.outputPath('account-page.zip')
  await context.tracing.stop({ path: trace })
  await testInfo.attach('balance', { path: balance, contentType: 'image/png' }) // shows in the HTML report

  expect(existsSync(trace)).toBeTruthy()
  expect(existsSync(fullPage)).toBeTruthy()
  expect(existsSync(balance)).toBeTruthy()
})
