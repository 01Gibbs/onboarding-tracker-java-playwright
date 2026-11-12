package dev.upskill.e2e.workshop.lesson08;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 08 starter, part 1: raw requests with the injected APIRequestContext. Get a token in a @BeforeEach. */
@UsePlaywright(LedgerlineOptions.class)
class CustomerApiTest {

    @Test
    void requestsWithoutATokenAreRejected(APIRequestContext request) {
        abort("TODO lesson 08, exercise 1");
    }

    @Test
    void listCustomersAndFilterByStatus(APIRequestContext request) {
        abort("TODO lesson 08, exercise 2");
    }

    @Test
    void createReadUpdateDeleteACustomer(APIRequestContext request) {
        abort("TODO lesson 08, exercise 3");
    }

    @Test
    void validationErrorsListEachInvalidField(APIRequestContext request) {
        abort("TODO lesson 08, exercise 4");
    }

    @Test
    void duplicateEmailIsAConflict(APIRequestContext request) {
        abort("TODO lesson 08, exercise 5");
    }

    @Test
    void auditorsCanReadButNotWrite(APIRequestContext request) {
        abort("TODO lesson 08, exercise 6");
    }
}
