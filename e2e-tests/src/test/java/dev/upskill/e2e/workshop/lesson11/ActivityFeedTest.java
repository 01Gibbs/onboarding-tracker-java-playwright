package dev.upskill.e2e.workshop.lesson11;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 11 starter: route() before the request happens. */
@UsePlaywright(LedgerlineOptions.class)
class ActivityFeedTest {

    @Test
    void realActivityIsShownByDefault() {
        abort("TODO lesson 11, exercise 1");
    }

    @Test
    void showsMockedEntries() {
        abort("TODO lesson 11, exercise 2");
    }

    @Test
    void showsAnEmptyState() {
        abort("TODO lesson 11, exercise 3");
    }

    @Test
    void recoversFromAServerErrorAfterRefresh() {
        abort("TODO lesson 11, exercise 4");
    }

    @Test
    void handlesANetworkFailure() {
        abort("TODO lesson 11, exercise 5");
    }

    @Test
    void modifiesTheRealResponse() {
        abort("TODO lesson 11, exercise 6");
    }

    @Test
    void refreshCallsTheApiAgain() {
        abort("TODO lesson 11, exercise 7");
    }
}
