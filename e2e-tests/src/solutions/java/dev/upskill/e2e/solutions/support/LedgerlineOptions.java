package dev.upskill.e2e.solutions.support;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

/**
 * Shared Playwright configuration (introduced in lesson 04, extended in lesson 12).
 * Values come from system properties so the same tests can target any environment:
 * <pre>./mvnw -pl e2e-tests test -DbaseUrl=http://localhost:8080 -Dheadless=false</pre>
 */
public class LedgerlineOptions implements OptionsFactory {

    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "true"));

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl(BASE_URL)                       // page.navigate("/customers") works
                .setHeadless(HEADLESS)
                .setApiRequestOptions(new APIRequest.NewContextOptions().setBaseURL(BASE_URL))
                .setTrace(Options.Trace.RETAIN_ON_FAILURE); // lesson 12: test-results/<test>/trace.zip
    }
}
