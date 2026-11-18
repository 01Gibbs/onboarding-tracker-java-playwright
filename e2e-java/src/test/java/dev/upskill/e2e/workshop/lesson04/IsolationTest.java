package dev.upskill.e2e.workshop.lesson04;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Lesson 04 starter: prove that every test gets a fresh BrowserContext. */
@UsePlaywright(LedgerlineOptions.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IsolationTest {

    @Test
    @Order(1)
    void firstTestSignsIn(Page page, BrowserContext context) {
        abort("TODO lesson 04, exercise 3: sign in, assert a JSESSIONID cookie exists");
    }

    @Test
    @Order(2)
    void secondTestStartsSignedOut(Page page, BrowserContext context) {
        abort("TODO lesson 04, exercise 3: assert no cookies, and /customers redirects to /login");
    }
}
