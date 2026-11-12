package dev.upskill.e2e.workshop.lesson10;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 10 starter: sign in once in @BeforeAll, save storage state, start each test from it. */
@UsePlaywright(LedgerlineOptions.class)
class AuditorRoleTest {

    @Test
    void auditorLandsOnTheDashboardWithoutLoggingIn() {
        abort("TODO lesson 10, exercise 1");
    }

    @Test
    void auditorDoesNotSeeEditingControls() {
        abort("TODO lesson 10, exercise 2");
    }

    @Test
    void auditorIsBlockedFromChangesEvenWithoutTheButtons() {
        abort("TODO lesson 10, exercise 3");
    }
}
