# Lesson 06: CRUD through the UI, plus validation and dialogs

**Goal:** cover the full customer life cycle (create, read, update, delete), including form
validation and browser confirmation dialogs.

**Time:** about 2 hours

**Docs:** [Dialogs](https://playwright.dev/java/docs/dialogs) · [Assertions](https://playwright.dev/java/docs/test-assertions) · [Actions](https://playwright.dev/java/docs/input)

## Concepts

### Dialogs

`alert`, `confirm` and `prompt` are native browser dialogs. **By default Playwright dismisses them
automatically**, so a confirm-protected button silently does nothing. Register a handler *before* the
action that triggers the dialog:

```java
page.onceDialog(dialog -> {
    assertEquals("confirm", dialog.type());
    assertEquals("Delete customer Kofi Tester-1a2b3c4d? This cannot be undone.", dialog.message());
    dialog.accept();      // or dialog.dismiss()
});
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete customer")).click();
```

`onceDialog` handles only the next dialog. `onDialog` handles every dialog until you remove it.

### Own your test data

Every test that changes data should **create the data it changes**, with unique values (from
`TestData`). Never edit or delete the seeded customers. Other tests, and other people, rely on them.

### Accessible validation is testable validation

The forms mark invalid inputs with `aria-invalid="true"` and link each one to its message with
`aria-describedby`. To a screen reader, the message is the field's **description**, so you can assert
it without any CSS:

```java
assertThat(page.getByLabel("First name")).hasAccessibleDescription("First name is required");
assertThat(page.getByLabel("First name")).hasAttribute("aria-invalid", "true");
```

### Grouping with `@Nested`

JUnit's `@Nested` classes group related tests (for example, "Validation") and inherit the outer
class's `@BeforeEach` and `@UsePlaywright`.

## Exercise

Open the starter `workshop/lesson06/CustomerCrudTest.java`. Fill in the `@BeforeEach` sign-in first,
then the private helper `createCustomer(Page page, String firstName)`. It fills in and saves the "New
customer" form with unique data, and returns the full name.

**Create and read**

1. Create a customer, search for their (unique) last name, and assert one row containing their name
   with an "Active" badge.

**Update**

2. Create a customer, click **Edit**, and assert that the form is pre-filled (`hasValue`). Change the
   email and add a phone number, then save. Assert the flash message and the new values on the page.
3. Create a customer and click **Suspend customer**. Assert that the badge says "Suspended" and that "Open
   account" has disappeared. Then reactivate them.

**Delete**

4. Create a customer, delete them and **accept** the dialog. Also assert the dialog's type and exact
   message. Assert that you're on `/customers`, that the flash says "Customer … deleted", and that no link
   with their name exists.
5. Create a customer, click delete and **dismiss** the dialog. Assert that nothing happened.
6. Try to delete **Alice Archer** (accept the dialog). Assert the error: "Customer cannot be deleted
   while they have accounts". (This is safe: the delete is rejected.)

**Validation** (in a `@Nested class Validation`)

7. Submit an empty form. Assert the error summary, the accessible descriptions of First name and Last
   name, and `aria-invalid="true"` on First name.
8. Submit an invalid email. Assert the message, and that the other fields kept their values.
9. Use `ALICE.ARCHER@example.test`. Assert "A customer with this email already exists".

## Discussion

- What happens in exercise 4 if you forget `onceDialog`? Try it.
- Exercises 1–5 all start by creating a customer through the UI. That's slow, and it's not what
  they're testing. How could we make it faster? (Lesson 09 answers this.)

## Checkpoint

- [ ] 9 passing tests that you can rerun as often as you like
- [ ] No seeded data was modified

**Reference:** `solutions/lesson06/CustomerCrudTest.java`

Next: [07: Page Object Model](07-page-objects.md)
