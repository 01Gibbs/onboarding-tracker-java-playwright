package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/** /api/accounts: opening, renaming, freezing, closing and deleting, as a client meets them. */
@SpringBootTest
class AccountApiTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void aRejectedUpdateChangesNothing() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":5}"));

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"nickname\":\"Renamed\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict());

        api.performAs("teller", get("/api/accounts/" + account))
                .andExpect(jsonPath("$.nickname").value("Api test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void aClosedAccountCannotBeRenamed() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"nickname\":\"Renamed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Closed accounts cannot be changed"));
    }

    @Test
    void aNewAccountIsActiveEmptyNumberedAndLocated() throws Exception {
        int customer = api.newCustomer();

        api.performAs("teller", json(post("/api/accounts"),
                        "{\"customerId\":%d,\"type\":\"SAVINGS\",\"nickname\":\"  Pot  \"}".formatted(customer)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/accounts/\\d+")))
                .andExpect(jsonPath("$.accountNumber").value(matchesPattern("1\\d{7}")))
                .andExpect(jsonPath("$.customerId").value(customer))
                .andExpect(jsonPath("$.customerName").value("Api Test"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.nickname").value("Pot"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.openedAt").isString());
        api.performAs("teller", get("/api/customers/" + customer))
                .andExpect(jsonPath("$.accountCount").value(1));
    }

    @Test
    void anAccountNeedsAHolderATypeAndANickname() throws Exception {
        api.performAs("teller", json(post("/api/accounts"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerId").value("Choose an account holder"))
                .andExpect(jsonPath("$.fieldErrors.type").value("Choose an account type"))
                .andExpect(jsonPath("$.fieldErrors.nickname").value("Nickname is required"));
    }

    @Test
    void anAccountForAnUnknownCustomerIsA404() throws Exception {
        api.performAs("teller", json(post("/api/accounts"), "{\"customerId\":999999,\"type\":\"CURRENT\",\"nickname\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnknownTypeIsMalformed() throws Exception {
        api.performAs("teller", json(post("/api/accounts"), "{\"customerId\":1,\"type\":\"GOLD\",\"nickname\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void listsCanBeFilteredByStatusTypeAndHolder() throws Exception {
        int account = api.newAccount();
        int customer = customerOf(account);
        api.performAs("teller", json(post("/api/accounts"),
                "{\"customerId\":%d,\"type\":\"SAVINGS\",\"nickname\":\"Pot\"}".formatted(customer)));
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"FROZEN\"}"));

        api.performAs("teller", get("/api/accounts").param("customerId", String.valueOf(customer)))
                .andExpect(jsonPath("$", hasSize(2)));
        api.performAs("teller", get("/api/accounts").param("customerId", String.valueOf(customer)).param("type", "SAVINGS"))
                .andExpect(jsonPath("$[*].nickname").value(contains("Pot")));
        api.performAs("teller", get("/api/accounts").param("customerId", String.valueOf(customer)).param("status", "FROZEN"))
                .andExpect(jsonPath("$[*].id").value(contains(account)));
    }

    @Test
    void aRenameIsTrimmedAndUpToFortyCharacters() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"nickname\":\" %s \"}".formatted("n".repeat(40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("n".repeat(40)));
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"nickname\":\"%s\"}".formatted("n".repeat(41))))
                .andExpect(status().isUnprocessableContent());
        api.performAs("teller", get("/api/accounts/" + account))
                .andExpect(jsonPath("$.nickname").value("n".repeat(40)));
    }

    @Test
    void freezingAndUnfreezing() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.statusLabel").value("Frozen"));
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"ACTIVE\"}"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void closedIsFinal() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"CLOSED\"}"))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.status").value("Closed accounts cannot be changed"));
    }

    @Test
    void anAccountWithMoneyCannotBeClosed() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":0.01}"));

        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account balance must be zero before it can be closed"));
    }

    @Test
    void anUnusedAccountCanBeDeleted() throws Exception {
        int account = api.newAccount();
        int customer = customerOf(account);

        api.performAs("teller", delete("/api/accounts/" + account)).andExpect(status().isNoContent());
        api.performAs("teller", get("/api/accounts/" + account)).andExpect(status().isNotFound());
        api.performAs("teller", get("/api/customers/" + customer)).andExpect(jsonPath("$.accountCount").value(0));
        api.performAs("teller", delete("/api/customers/" + customer)).andExpect(status().isNoContent());
    }

    @Test
    void anAccountWithHistoryCannotBeDeletedEvenWhenEmpty() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1}"));
        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":1}"));

        api.performAs("teller", delete("/api/accounts/" + account))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account has transaction history and cannot be deleted. Close it instead."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void anUnknownAccountIsA404Everywhere() throws Exception {
        api.performAs("teller", get("/api/accounts/999999")).andExpect(status().isNotFound());
        api.performAs("teller", json(patch("/api/accounts/999999"), "{}")).andExpect(status().isNotFound());
        api.performAs("teller", delete("/api/accounts/999999")).andExpect(status().isNotFound());
        api.performAs("teller", get("/api/accounts/999999/transfer-targets")).andExpect(status().isNotFound());
    }

    private int customerOf(int account) throws Exception {
        return api.read("/api/accounts/" + account, "$.customerId");
    }
}
