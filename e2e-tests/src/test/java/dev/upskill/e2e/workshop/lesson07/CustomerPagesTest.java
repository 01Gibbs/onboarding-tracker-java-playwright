package dev.upskill.e2e.workshop.lesson07;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 07 starter: no locators in this file - only page objects from the pages package. */
@UsePlaywright(LedgerlineOptions.class)
class CustomerPagesTest {

    @Test
    void createSearchAndOpenACustomer(Page page) {
        abort("TODO lesson 07, exercise 1");
    }

    @Test
    void deleteACustomer(Page page) {
        abort("TODO lesson 07, exercise 2");
    }

    @Test
    void validationErrorsAreShownNextToFields(Page page) {
        abort("TODO lesson 07, exercise 3");
    }

    @Test
    void searchingForSomethingThatDoesNotExist(Page page) {
        abort("TODO lesson 07, exercise 4");
    }
}
