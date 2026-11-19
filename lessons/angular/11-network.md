# Lesson 11: Network interception and mocking

**Goal:** control the network to test states that are hard to produce for real (empty lists, server errors,
outages, slow responses), and observe the requests the page makes.

**Time:** about 2 hours

**Docs:** [Mock APIs](https://playwright.dev/docs/mock) · [Network](https://playwright.dev/docs/network) · [Route](https://playwright.dev/docs/api/class-route)

## Concepts

Every page of the Angular app gets its data from `/api/...`, so the network is where you control it. The
dashboard's **Recent activity** panel (`GET /api/activity?limit=8`) has loading, list, empty and error states:
a perfect target.

### Routing

Register routes **before** the request happens (usually before `goto`):

```ts
await page.route('**/api/activity*', route => route.fulfill({ json: [] }))
```

| Method | Effect |
|--------|--------|
| `route.fulfill({ json, status, ... })` | Answer with your own response; nothing reaches the server |
| `route.abort('connectionrefused')` | Simulate a network failure |
| `route.continue()` | Send it on to the server (optionally changed) |
| `route.fetch()` then `route.fulfill({ response, json })` | Call the real server, then change its answer |
| `route.fallback()` | Pass it to the next matching handler |
| `page.unroute(pattern)` | Remove your routes |

Glob patterns: `*` matches anything except `/`, and `**` matches anything including `/`. The pattern has to
match the **whole URL**, including `?query`: `**/api/activity` does **not** match `/api/activity?limit=8`.

### A mock must look like the real thing

The real `/api/activity` returns entries with raw fields (`type`, `amount`) and the text the pages show
(`typeLabel`, `amountDisplay`, `description`, `accountNickname`). Look at a real response in DevTools →
Network, and copy its shape. A mock with missing fields tests a page that can never happen.

### Waiting for and observing traffic

```ts
const response = page.waitForResponse('**/api/activity*')   // start waiting first...
await page.reload()                                         // ...then trigger it
expect((await response).status()).toBe(200)

page.on('request', request => { if (request.url().includes('/api/')) calls++ })
```

### Holding a request to see a loading state

A route handler doesn't have to answer straight away. Keep the `Route`, assert while the browser waits, then
release it:

```ts
const held: Route[] = []
await page.route('**/api/activity*', route => { held.push(route) })
await dashboard.refreshActivity()
await expect(panel.getByRole('status')).toHaveText('Loading activity…')
await expect.poll(() => held.length).toBe(1)      // the handler has the request
for (const route of held) await route.continue()
```

> **Count from a known point.** The dashboard asks for `/api/activity` as soon as it appears, and a test can't
> know exactly when that is. A test that counts requests should start counting after that first load has
> finished, as exercise 7 does.

## Exercise

Complete the starter `e2e-angular/lesson11/activity-feed.spec.ts`. Sign in during `beforeEach`, and finish
`activityPanel`, `activityItems` and `refreshActivity()` in your `DashboardPage`.

1. **Real data.** `waitForResponse` around `page.reload()` → `200`, and at least one activity item visible.
2. **Mocked list.** Fulfill with two entries (a deposit of 1,234.50 "Mocked salary" and a withdrawal of 20).
   Expect exactly 2 items, the first containing "Deposit · Mocked salary" and "+£1,234.50", the second "-£20.00".
3. **Empty.** Fulfill `[]` → "No activity yet."
4. **Server error, then recovery.** Fulfill `500` → the alert "Could not load recent activity. Use Refresh to try again." Then `unroute`
   and click **Refresh** → real items appear.
5. **Network failure.** `abort('connectionrefused')` → the alert is visible.
6. **Patch the real response.** Use `route.fetch()`, set every description to `PATCHED`, and fulfill.
7. **Count calls.** After the first load finishes, count `/api/activity` requests. Click Refresh while waiting
   for the response, and expect exactly **1** more call.
8. Create `e2e-angular/lesson11/in-flight-state.spec.ts` yourself: hold the request, expect the "Loading
   activity…" status and zero items, release it, then expect the status to disappear and items to appear.
   *Tip: wait for the first load to finish before you add the route, or you'll race with it.*

## Discussion

- Mocking makes tests fast and deterministic, but what does it **stop** you from testing?
- Where else in a real product would `abort` or `500` tests be valuable?

## Checkpoint

- [ ] 8 passing tests, none depending on what activity actually exists (except 1, 4 and 7)

**Reference:** `lesson11/`

Next: [12: Debugging, tracing and scaling the suite](12-debugging-and-scale.md)
