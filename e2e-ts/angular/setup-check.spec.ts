import { expect, test } from '@playwright/test'

// Lesson 00: proves your machine is ready. If this passes, Node, Playwright, the back end and the Angular app work.
// Your own tests go in the lessonNN folders next to this file: see lessons/angular/01-first-test.md.

test('Ledgerline answers through the Angular app', async ({ request }) => {
  // The Angular app passes /api/... on to the back end, so this checks both are up
  const response = await request.get('/api/health')

  expect(response.ok(), 'Ledgerline is not answering. Is the back end running? ./mvnw -pl app spring-boot:run').toBeTruthy()
  expect(await response.json()).toEqual({ status: 'UP' })
})
