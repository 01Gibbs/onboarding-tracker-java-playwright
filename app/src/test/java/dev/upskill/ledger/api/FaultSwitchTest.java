package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Mission B0's planted faults: switched on for one account through test support, so learners can prove
 * their ledger checks catch a broken invariant. Faults are shared app state, so each test clears them.
 */
@SpringBootTest(properties = "ledgerline.test-support-enabled=true")
class FaultSwitchTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @AfterEach
    void clearFaults() throws Exception {
        api.perform(delete("/api/testing/faults")).andExpect(status().isNoContent());
    }

    @Test
    void aFaultOnlyAffectsTheAccountItIsArmedFor() throws Exception {
        int faulty = api.newAccount();
        int healthy = api.newAccount();
        deposit(faulty, "50");
        deposit(healthy, "50");

        api.perform(json(put("/api/testing/faults/skip-withdrawal-entry"), "{\"accountId\":%d}".formatted(faulty)))
                .andExpect(status().isOk());
        withdraw(faulty, "10");
        withdraw(healthy, "10");

        api.performAs("teller", get("/api/accounts/" + faulty + "/transactions"))
                .andExpect(jsonPath("$", hasSize(1)));
        api.performAs("teller", get("/api/accounts/" + healthy + "/transactions"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void resetClearsFaultsBecauseAccountIdsStartAgain() throws Exception {
        // The seed creates 6 accounts, so after a reset the next new account is always id 7.
        api.perform(json(put("/api/testing/faults/skip-withdrawal-entry"), "{\"accountId\":7}"))
                .andExpect(status().isOk());

        api.perform(post("/api/testing/reset")).andExpect(status().isOk());

        int account = api.newAccount();
        deposit(account, "10");
        withdraw(account, "5");
        api.performAs("teller", get("/api/accounts/" + account + "/transactions"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void disarmingAFaultThatIsNotArmedIsHarmless() throws Exception {
        api.perform(delete("/api/testing/faults/overdraft?accountId=0"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownFaultsAreA404() throws Exception {
        api.perform(json(put("/api/testing/faults/no-such-fault"), "{\"accountId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fault no-such-fault was not found"));
    }

    private void deposit(int account, String amount) throws Exception {
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }

    private void withdraw(int account, String amount) throws Exception {
        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }
}
