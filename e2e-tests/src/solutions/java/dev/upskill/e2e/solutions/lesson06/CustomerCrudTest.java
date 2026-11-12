package dev.upskill.e2e.solutions.lesson06;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.TestData;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lesson 06: create, read, update and delete customers through the UI, including validation and dialogs. */
@UsePlaywright(LedgerlineOptions.class)
class CustomerCrudTest {

    @BeforeEach
    void signIn(Page page) {
        page.navigate("/login");
        page.getByLabel("Username").fill("teller");
        page.getByLabel("Password").fill("teller-pass");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
    }

    /** Creates a customer through the form and leaves the browser on their detail page. */
    private String createCustomer(Page page, String firstName) {
        String lastName = TestData.uniqueLastName();
        page.navigate("/customers/new");
        page.getByLabel("First name").fill(firstName);
        page.getByLabel("Last name").fill(lastName);
        page.getByLabel("Email").fill(TestData.uniqueEmail(firstName.toLowerCase()));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save customer")).click();
        assertThat(page.getByTestId("flash-message")).containsText("created");
        return firstName + " " + lastName;
    }

    private void clickButton(Page page, String name) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name)).click();
    }

    @Test
    void createdCustomerCanBeFoundBySearch(Page page) {
        String name = createCustomer(page, "Hana");

        page.navigate("/customers");
        page.getByRole(AriaRole.SEARCHBOX, new Page.GetByRoleOptions().setName("Search customers"))
                .fill(name.split(" ")[1]);
        clickButton(page, "Search");

        assertThat(page.getByTestId("customer-row")).hasCount(1);
        assertThat(page.getByTestId("customer-row")).containsText(name);
        assertThat(page.getByTestId("customer-row").getByTestId("status-badge")).hasText("Active");
    }

    @Test
    void editingACustomerUpdatesTheirDetails(Page page) {
        createCustomer(page, "Ivan");
        String newEmail = TestData.uniqueEmail("ivan.updated");

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit")).click();
        assertThat(page.getByLabel("First name")).hasValue("Ivan");   // form is pre-filled
        page.getByLabel("Email").fill(newEmail);
        page.getByLabel("Phone").fill("+44 7700 900123");
        clickButton(page, "Save customer");

        assertThat(page.getByTestId("flash-message")).containsText("updated");
        assertThat(page.getByText(newEmail)).isVisible();
        assertThat(page.getByText("+44 7700 900123")).isVisible();
    }

    @Test
    void suspendingAndReactivatingACustomer(Page page) {
        createCustomer(page, "Jade");
        var badge = page.getByRole(AriaRole.HEADING).getByTestId("status-badge");

        clickButton(page, "Suspend customer");
        assertThat(badge).hasText("Suspended");
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Open account"))).isHidden();

        clickButton(page, "Reactivate customer");
        assertThat(badge).hasText("Active");
    }

    @Test
    void deletingACustomerAfterConfirming(Page page) {
        String name = createCustomer(page, "Kofi");

        // Handlers run on the test's thread in Playwright for Java, so a failing assertion here fails the test
        page.onceDialog(dialog -> {
            assertEquals("confirm", dialog.type());
            assertEquals("Delete customer " + name + "? This cannot be undone.", dialog.message());
            dialog.accept();
        });
        clickButton(page, "Delete customer");

        assertThat(page).hasURL(Pattern.compile(".*/customers$"));
        assertThat(page.getByTestId("flash-message")).hasText("Customer " + name + " deleted");
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name))).hasCount(0);
    }

    @Test
    void dismissingTheConfirmationKeepsTheCustomer(Page page) {
        String name = createCustomer(page, "Lena");

        page.onceDialog(dialog -> dialog.dismiss());
        clickButton(page, "Delete customer");

        // Nothing was submitted: still on the same page, still showing the "created" message
        assertThat(page).hasURL(Pattern.compile(".*/customers/\\d+$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).containsText(name);
        assertThat(page.getByTestId("flash-message")).containsText("created");
    }

    @Test
    void customersWithAccountsCannotBeDeleted(Page page) {
        page.navigate("/customers");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Alice Archer")).click();

        page.onceDialog(dialog -> dialog.accept());
        clickButton(page, "Delete customer");

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("Customer cannot be deleted while they have accounts");
    }

    @Nested
    class Validation {

        @Test
        void requiredFieldsAreReported(Page page) {
            page.navigate("/customers/new");

            clickButton(page, "Save customer");

            assertThat(page.getByTestId("error-summary")).isVisible();
            // Each message is linked to its input with aria-describedby: assert it as the field's description
            assertThat(page.getByLabel("First name")).hasAccessibleDescription("First name is required");
            assertThat(page.getByLabel("Last name")).hasAccessibleDescription("Last name is required");
            assertThat(page.getByLabel("First name")).hasAttribute("aria-invalid", "true");
        }

        @Test
        void invalidEmailIsRejected(Page page) {
            page.navigate("/customers/new");
            page.getByLabel("First name").fill("Max");
            page.getByLabel("Last name").fill("Mustermann");
            page.getByLabel("Email").fill("not-an-email");

            clickButton(page, "Save customer");

            assertThat(page.getByLabel("Email")).hasAccessibleDescription("Enter a valid email address");
            // What the user typed is preserved
            assertThat(page.getByLabel("First name")).hasValue("Max");
        }

        @Test
        void duplicateEmailIsRejected(Page page) {
            page.navigate("/customers/new");
            page.getByLabel("First name").fill("Alice");
            page.getByLabel("Last name").fill("Again");
            page.getByLabel("Email").fill("ALICE.ARCHER@example.test");

            clickButton(page, "Save customer");

            assertThat(page.getByLabel("Email")).hasAccessibleDescription("A customer with this email already exists");
        }
    }
}
