package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

/**
 * The JSON API as a client sees it: status codes and the error shape promised in docs/ledgerline-reference.md.
 * Runs through the real interceptors and exception handlers.
 */
@SpringBootTest
class ApiContractTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void missingCredentialsGiveA401InTheErrorShape() throws Exception {
        api.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void auditorsCannotWriteAndGetA403InTheErrorShape() throws Exception {
        api.performAs("auditor", post("/api/customers"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void unknownApiRoutesGiveA404InTheErrorShape() throws Exception {
        api.performAs("teller", get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void customerStatusChangeWithoutAStatusIsA400() throws Exception {
        int customer = api.newCustomer();

        api.performAs("teller", json(patch("/api/customers/" + customer + "/status"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("Status is required"));
    }

    @Test
    void transferWithoutAccountIdsIsA400() throws Exception {
        api.performAs("teller", json(post("/api/transfers"), "{\"amount\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").exists());
    }

    @Test
    void aBlankNicknameIsABrokenRuleNotBadInput() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"nickname\":\" \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.nickname").value("Nickname must be between 1 and 40 characters"));
    }

    @Test
    void activityLimitIsClampedRatherThanRejected() throws Exception {
        api.performAs("teller", get("/api/activity?limit=99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(50))));
        api.performAs("teller", get("/api/activity?limit=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(1))));
    }

    @Test
    void depositsAreStoredAndEchoedInPence() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1E+2}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"amount\":100.00")));
    }

    @Test
    void aBodyThatIsNotJsonGetsA415InTheErrorShape() throws Exception {
        api.performAs("teller", post("/api/customers").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void aMalformedBodyIsA400() throws Exception {
        api.performAs("teller", json(post("/api/customers"), "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void anUnknownTokenIsA401() throws Exception {
        api.perform(get("/api/customers").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditorsCanRead() throws Exception {
        api.performAs("auditor", get("/api/customers")).andExpect(status().isOk());
    }

    @Test
    void lockedUsersCannotSignIn() throws Exception {
        api.perform(json(post("/api/auth/login"), "{\"username\":\"locked\",\"password\":\"locked-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("This user is locked. Contact an administrator."));
    }

    @Test
    void aWrongPasswordIsA401WithoutSayingWhichPartWasWrong() throws Exception {
        api.perform(json(post("/api/auth/login"), "{\"username\":\"teller\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        api.perform(json(post("/api/auth/login"), "{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signingInReturnsWhoYouAre() throws Exception {
        api.perform(json(post("/api/auth/login"), "{\"username\":\"auditor\",\"password\":\"auditor-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("auditor"))
                .andExpect(jsonPath("$.displayName").value("Alex Auditor"))
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void testSupportIsOffUnlessTheDevProfileTurnsItOn() throws Exception {
        api.perform(post("/api/testing/reset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fieldErrors").isMap());
        api.perform(json(put("/api/testing/faults/overdraft"), "{\"accountId\":1}"))
                .andExpect(status().isNotFound());
        api.perform(delete("/api/testing/faults/overdraft?accountId=1")).andExpect(status().isNotFound());
        api.perform(delete("/api/testing/faults")).andExpect(status().isNotFound());
        api.perform(json(put("/api/testing/clock"), "{\"instant\":\"2030-01-31T16:59:00Z\"}"))
                .andExpect(status().isNotFound());
        api.perform(delete("/api/testing/clock"))
                .andExpect(status().isNotFound());
        api.perform(json(put("/api/testing/clock"), "{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSignedInBrowserSessionCanCallTheApi() throws Exception {
        MockHttpSession teller = api.signIn("teller");

        api.perform(get("/api/customers").session(teller)).andExpect(status().isOk());
        api.perform(json(post("/api/accounts/" + api.newAccount() + "/deposits"), "{\"amount\":1}").session(teller))
                .andExpect(status().isCreated());
    }

    @Test
    void anAuditorsBrowserSessionCanOnlyRead() throws Exception {
        MockHttpSession auditor = api.signIn("auditor");

        api.perform(get("/api/dashboard").session(auditor)).andExpect(status().isOk());
        api.perform(json(post("/api/accounts/" + api.newAccount() + "/deposits"), "{\"amount\":1}").session(auditor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to do that"));
    }

    @Test
    void moveMoneyIsSlowOnPurposeLikeThePage() throws Exception {
        int account = api.newAccount();

        long started = System.nanoTime();
        api.performAs("teller", json(post("/api/accounts/" + account + "/movements"), "{\"type\":\"DEPOSIT\",\"amount\":1}"))
                .andExpect(status().isCreated());
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(750));
    }
}
