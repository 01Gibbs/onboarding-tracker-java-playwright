package dev.upskill.e2e.workshop.lesson06;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lesson 06 starter. Test names match the exercise list in lessons/06-crud-ui.md. */
@UsePlaywright(LedgerlineOptions.class)
class CustomerCrudTest {

    @BeforeEach
    void signIn(Page page) {
        // TODO: sign in as teller
    }

    /** Creates a customer through the form with unique data; returns "First Last". */
    private String createCustomer(Page page, String firstName) {
        throw new UnsupportedOperationException("TODO lesson 06");
    }

    @Test
    void createdCustomerCanBeFoundBySearch(Page page) {
        abort("TODO lesson 06, exercise 1");
    }

    @Test
    void editingACustomerUpdatesTheirDetails(Page page) {
        abort("TODO lesson 06, exercise 2");
    }

    @Test
    void suspendingAndReactivatingACustomer(Page page) {
        abort("TODO lesson 06, exercise 3");
    }

    @Test
    void deletingACustomerAfterConfirming(Page page) {
        abort("TODO lesson 06, exercise 4");
    }

    @Test
    void dismissingTheConfirmationKeepsTheCustomer(Page page) {
        abort("TODO lesson 06, exercise 5");
    }

    @Test
    void customersWithAccountsCannotBeDeleted(Page page) {
        abort("TODO lesson 06, exercise 6");
    }

    @Nested
    class Validation {

        @Test
        void requiredFieldsAreReported(Page page) {
            abort("TODO lesson 06, exercise 7");
        }

        @Test
        void invalidEmailIsRejected(Page page) {
            abort("TODO lesson 06, exercise 8");
        }

        @Test
        void duplicateEmailIsRejected(Page page) {
            abort("TODO lesson 06, exercise 9");
        }
    }
}
