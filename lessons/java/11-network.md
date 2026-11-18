# Lesson 11: Network interception and mocking

**Goal:** control the network to test states that are hard to produce for real (empty lists, server
errors, outages, slow responses), and observe the requests the page makes.

**Time:** about 2 hours

**Docs:** [Mock APIs](https://playwright.dev/java/docs/mock) · [Network](https://playwright.dev/java/docs/network) · [Route](https://playwright.dev/java/docs/api/class-route)

## Concepts

The dashboard's **Recent activity** panel loads `GET /api/activity?limit=8` with `fetch()`, and has
loading, list, empty and error states. That makes it a perfect target.

### Routing

Register routes **before** the request happens (usually before `navigate`):

```java
page.route("**/api/activity*", route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody("[]")));
```

| Method | Effect |
|--------|--------|
| `route.fulfill(...)` | Respond with your own status, headers and body; nothing reaches the server |
| `route.abort("connectionrefused")` | Simulate a network failure |
| `route.resume()` | Send it to the server (optionally modified) |
| `route.fetch()` + `fulfill(setResponse(real).setBody(changed))` | Call the real server, then change the response |
| `route.fallback()` | Pass it to the next matching route handler |
| `page.unroute(pattern)` | Remove your routes |

Glob patterns: `*` matches anything except `/`, and `**` matches anything including `/`. The pattern
has to match the **whole URL**, including `?query`.

> **Docs gotcha:** the Mock APIs page shows `route.fulfill(RequestOptions.create().setData(...))`.
> That doesn't exist in Java. Use `new Route.FulfillOptions().setContentType("application/json").setBody(json)`.
> It also shows `Response response = route.fetch()`, but `route.fetch()` returns **`APIResponse`**.

### Waiting for and observing traffic

```java
Response response = page.waitForResponse("**/api/activity*", () -> page.reload());
assertEquals(200, response.status());

page.onRequest(request -> { if (request.url().contains("/api/")) calls.incrementAndGet(); });
```

### Holding a request to see a loading state

A route handler doesn't have to answer straight away. Store the `Route`, make your assertions while
the browser waits, then release it:

```java
List<Route> held = new CopyOnWriteArrayList<>();
page.route("**/api/activity*", held::add);
dashboard.refreshActivity();
assertThat(panel.getByRole(AriaRole.STATUS)).hasText("Loading activity…");
held.forEach(Route::resume);
```

> Keep route handlers **short**. Don't call page or locator methods (`textContent`, `click`…) inside
> a handler, because it runs on Playwright's dispatch thread and can deadlock. Calling `route.fetch()`
> is fine.
>
> This works for `fetch()`/XHR. It does **not** work for full-page form submissions, because locators
> wait for the navigation to finish (see lesson 09).

## Exercise

Complete the starter `workshop/lesson11/ActivityFeedTest.java` (test names only). Sign in during
`@BeforeEach`, and implement `activityPanel()`, `activityItems()` and `refreshActivity()` in your `DashboardPage`.

1. **Real data.** `waitForResponse` around `page.reload()` → `200`, and at least one activity item visible.
2. **Mocked list.** Fulfill with two entries (a deposit of `1234.5` and a withdrawal of `20`). Assert
   exactly 2 items, the first contains "Deposit · Mocked salary" and "+£1,234.50", and the second contains
   "-£20.00". (Look at `app.js` to see which JSON fields the panel uses.)
3. **Empty.** Fulfill `[]` → "No activity yet."
4. **Server error, then recovery.** Fulfill `500` → the alert "Could not load recent activity". Then
   `unroute` and click **Refresh** → real items appear.
5. **Network failure.** `abort("connectionrefused")` → the alert is visible.
6. **Patch the real response.** Use `route.fetch()`, replace every description with `PATCHED`, and fulfill.
7. **Count calls.** After the first load finishes, count `/api/activity` requests with `onRequest`.
   Click Refresh inside `waitForResponse`, and assert exactly **1** more call.
8. Create `workshop/lesson11/InFlightStateTest.java` yourself: hold the request, assert the "Loading activity…" status
   and zero items, release it, then assert that the status disappears and items appear.
   *Tip: wait for the first load to finish before you add the route, or you'll race with it.*

## Discussion

- Mocking makes tests fast and deterministic, but what does it **stop** you from testing?
- Where else in a real product would `abort` or `500` tests be valuable?

## Checkpoint

- [ ] 8 passing tests, none of them depending on what activity actually exists (except 1, 4 and 7)

**Reference:** `solutions/lesson11/`

Next: [12: Debugging, tracing and scaling the suite](12-debugging-and-scale.md)
