# Lesson 06: CRUD through the UI, plus validation and dialogs

**Goal:** cover the full customer life cycle (create, read, update, delete), including form validation and
browser confirmation dialogs.

**Time:** about 2 hours

**Docs:** [Dialogs](https://playwright.dev/docs/dialogs) · [Assertions](https://playwright.dev/docs/test-assertions) · [Annotations and describe](https://playwright.dev/docs/test-annotations)

## Concepts

### Dialogs

`alert`, `confirm` and `prompt` are native browser dialogs. **By default Playwright dismisses them**, so a
confirm-protected button silently does nothing. Handle the dialog by registering a listener *before* the action
that opens it:

```ts
page.once('dialog', dialog => dialog.accept())     // or dialog.dismiss()
await page.getByRole('button', { name: 'Delete customer' }).click()
```

To also check *which* dialog appeared, wait for it as an event, then act on it:

```ts
const dialogShown = page.waitForEvent('dialog')
const clicked = page.getByRole('button', { name: 'Delete customer' }).click()
const dialog = await dialogShown
expect(dialog.type()).toBe('confirm')
expect(dialog.message()).toBe('Delete customer Kofi Tester-1a2b3c4d? This cannot be undone.')
await dialog.accept()
await clicked
```

> Why not assert inside the `page.once` listener? If an `expect` in a listener fails, the listener stops before
> `accept()`, the dialog stays open, and the test hangs until it times out instead of reporting the real problem.

### Own your test data

Every test that changes data should **create the data it changes**, with unique values (`test-data.ts`). Never
edit or delete the seeded customers. Other tests, and other people, rely on them.

### Accessible validation is testable validation

The forms mark invalid inputs with `aria-invalid="true"` and link each one to its message with
`aria-describedby`. To a screen reader, the message is the field's **description**, so you can assert it
without any CSS:

```ts
await expect(page.getByLabel('First name')).toHaveAccessibleDescription('First name is required')
await expect(page.getByLabel('First name')).toHaveAttribute('aria-invalid', 'true')
```

### Grouping with `test.describe`

`test.describe('validation', () => { ... })` groups related tests. A `test.beforeEach` inside the group runs
only for its tests, after the file's own `beforeEach`.

## Exercise

Open the starter `e2e-ts/react/lesson06/customer-crud.spec.ts`. Fill in the `beforeEach` sign-in first, then the
helper `createCustomer(page, firstName)`. It fills in and saves the "New customer" form with unique data, and
returns the full name.

**Create and read**

1. Create a customer, search for their (unique) last name, and expect one row containing their name with an
   "Active" badge.

**Update**

2. Create a customer, click **Edit**, and expect the form to be pre-filled (`toHaveValue`). Change the email and
   add a phone number, then save. Expect the flash message and the new values on the page.
3. Create a customer and click **Suspend customer**. Expect the badge to say "Suspended" and "Open account" to
   have gone. Then reactivate them.

**Delete**

4. Create a customer, delete them and **accept** the dialog. Also check the dialog's type and exact message.
   Expect to be on `/customers`, the flash to say "Customer … deleted", and no link with their name.
5. Create a customer, click delete and **dismiss** the dialog. Expect nothing to have happened.
6. Try to delete **Alice Archer** (accept the dialog). Expect the error "Customer cannot be deleted while they
   have accounts". (This is safe: the delete is refused.)

**Validation** (in the `validation` group)

7. Submit an empty form. Expect the error summary, the accessible descriptions of First name and Last name, and
   `aria-invalid="true"` on First name.
8. Submit an invalid email. Expect the message, and that the other fields kept their values.
9. Use `ALICE.ARCHER@example.test`. Expect "A customer with this email already exists".

## Discussion

- What happens in exercise 4 if you forget to handle the dialog? Try it.
- Exercises 1–5 all start by creating a customer through the UI. That's slow, and it's not what they're
  testing. How could we make it faster? (Lesson 09 answers this.)

## Checkpoint

- [ ] 9 passing tests that you can rerun as often as you like
- [ ] No seeded data was changed

**Reference:** `lesson06/customer-crud.spec.ts`

Next: [07: Page Object Model](07-page-objects.md)
