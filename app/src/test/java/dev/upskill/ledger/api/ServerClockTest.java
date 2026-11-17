package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests can pin the server's clock, so time-based rules (cut-offs, timeouts, statements) are testable.
 * The clock is shared by the whole app, so this class releases it after every test.
 */
@SpringBootTest(properties = "ledgerline.test-support-enabled=true")
class ServerClockTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @AfterEach
    void releaseClock() throws Exception {
        api.perform(delete("/api/testing/clock")).andExpect(status().isNoContent());
    }

    @Test
    void entriesAreStampedWithThePinnedTime() throws Exception {
        int account = api.newAccount();

        api.perform(json(put("/api/testing/clock"), "{\"instant\":\"2030-01-31T16:59:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instant").value("2030-01-31T16:59:00Z"));

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").value("2030-01-31T16:59:00Z"));
    }

    @Test
    void pinningWithoutAnInstantIsA400AndLeavesTheClockAlone() throws Exception {
        api.perform(json(put("/api/testing/clock"), "{\"instant\":\"2030-01-31T16:59:00Z\"}"));

        api.perform(json(put("/api/testing/clock"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.instant").value("instant is required"));

        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1}"))
                .andExpect(jsonPath("$.createdAt").value("2030-01-31T16:59:00Z"));
    }

    @Test
    void releasingTheClockGoesBackToRealTime() throws Exception {
        api.perform(json(put("/api/testing/clock"), "{\"instant\":\"2030-01-31T16:59:00Z\"}"));
        api.perform(delete("/api/testing/clock")).andExpect(status().isNoContent());

        int account = api.newAccount();
        String body = api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1}"))
                .andReturn().getResponse().getContentAsString();
        Instant createdAt = Instant.parse(JsonPath.read(body, "$.createdAt"));
        assertThat(Duration.between(createdAt, Instant.now()).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void resetReseedsTheKnownData() throws Exception {
        api.perform(post("/api/testing/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(5))
                .andExpect(jsonPath("$.accounts").value(6));
        api.performAs("teller", get("/api/accounts/1"))
                .andExpect(jsonPath("$.accountNumber").value("10000001"))
                .andExpect(jsonPath("$.balance").value(1250.00));
    }
}
