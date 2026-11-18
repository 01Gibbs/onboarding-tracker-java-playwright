import { expect, test as setup } from '@playwright/test'

// Saves a signed-in session for each role, for the cases that start signed in. (This file is fine.)
for (const [username, password] of [['teller', 'teller-pass'], ['auditor', 'auditor-pass']]) {
  setup(`sign in as ${username}`, async ({ request }) => {
    const response = await request.post('/api/auth/session', { data: { username, password } })
    await expect(response).toBeOK()
    await request.storageState({ path: `playwright/.auth/broken-${username}.json` })
  })
}
