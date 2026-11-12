package dev.upskill.e2e.workshop;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;

/**
 * Lesson 00: proves your machine is ready. If this passes, Java, Maven, Playwright and the app all work.
 * Your own tests go in this "workshop" package - see lessons/01-first-test.md.
 */
@UsePlaywright
class SetupCheckTest {

    private static final String HEALTH = System.getProperty("baseUrl", "http://localhost:8080") + "/api/health";

    @Test
    void appIsRunning(APIRequestContext request) {
        APIResponse response;
        try {
            response = request.get(HEALTH);
        } catch (PlaywrightException e) {
            throw new AssertionError("Ledgerline is not running at " + HEALTH + ". Start it in another terminal "
                    + "with: ./mvnw -pl app spring-boot:run  (and leave it running)", e);
        }
        assertThat(response).isOK();
    }
}
