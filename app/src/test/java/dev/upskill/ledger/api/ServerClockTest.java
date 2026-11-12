package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
