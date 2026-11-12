package dev.upskill.e2e.workshop.support;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

/** Lesson 04: shared Playwright configuration. Point every test class at it with @UsePlaywright(LedgerlineOptions.class). */
public class LedgerlineOptions implements OptionsFactory {

    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "true"));

    @Override
    public Options getOptions() {
        // TODO lesson 04: set the base URL, headless mode and the API request base URL.
        //  Hint: new Options().setBaseUrl(...).setHeadless(...)
        //        .setApiRequestOptions(new APIRequest.NewContextOptions().setBaseURL(...))
        // TODO lesson 12: keep a trace for failing tests with .setTrace(Options.Trace.RETAIN_ON_FAILURE)
        return new Options();
    }
}
