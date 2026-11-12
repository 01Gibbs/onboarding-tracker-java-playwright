package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    void testSupportIsOffUnlessTheDevProfileTurnsItOn() throws Exception {
        api.perform(post("/api/testing/reset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fieldErrors").isMap());
        api.perform(json(put("/api/testing/faults/overdraft"), "{\"accountId\":1}"))
                .andExpect(status().isNotFound());
    }
}
