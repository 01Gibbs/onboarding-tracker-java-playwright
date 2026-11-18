package dev.upskill.e2e.workshop.lesson05;

import static org.junit.jupiter.api.Assumptions.abort;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import dev.upskill.e2e.workshop.support.LedgerlineOptions;
import org.junit.jupiter.api.Test;

/** Lesson 05 starter: paste your codegen recording into the test, then clean it up (see the checklist). */
@UsePlaywright(LedgerlineOptions.class)
class CreateCustomerRecordedTest {

    @Test
    void tellerCreatesACustomer(Page page) {
        abort("TODO lesson 05: paste the recording here");
    }
}
