package dev.upskill.e2e.broken;

import com.microsoft.playwright.junit.Options;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;

/**
 * The usual options, but actions give up after 5 seconds instead of 30, so broken tests fail fast.
 * Every failing test keeps a trace in test-results/ - that's where you start.
 */
public class BrokenSuiteOptions extends LedgerlineOptions {

    @Override
    public Options getOptions() {
        return super.getOptions().setDefaultTimeout(5_000.0);
    }
}
