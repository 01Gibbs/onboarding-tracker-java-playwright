package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/**
 * What a front end needs to rebuild Ledgerline's pages from the JSON API alone, with the same text and
 * rules as the Thymeleaf pages (ADR-0001).
 */
@SpringBootTest(properties = "ledgerline.processing-delay-ms=0")
class FrontEndApiTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void aMovementReturnsTheConfirmationThePageShows() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/movements"),
                        "{\"type\":\"DEPOSIT\",\"amount\":25.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Deposit of £25.50 completed"))
                .andExpect(jsonPath("$.account.balance").value(25.50));
    }

    @Test
    void aMovementWithoutAnAmountIsRejectedLikeTheForm() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/movements"), "{\"type\":\"DEPOSIT\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.amount").value("Amount is required"));
    }

    @Test
    void aMovementMustSayWhichKindItIs() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/movements"), "{\"amount\":10}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.type").value("Choose a type"));
    }

    @Test
    void aMovementOnAnUnknownAccountIsA404WhateverTheAmount() throws Exception {
        api.performAs("teller", json(post("/api/accounts/999999/movements"), "{\"type\":\"DEPOSIT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anOverlongDescriptionIsABrokenRuleNotAServerError() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"),
                        "{\"amount\":5,\"description\":\"%s\"}".formatted("x".repeat(81))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.description").value("Description must be 80 characters or fewer"));
    }

    @Test
    void anAccountCarriesTheTextItsPageShows() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", get("/api/accounts/" + account))
                .andExpect(jsonPath("$.displayName").value(matchesPattern("Api test \\(\\d{8}\\)")))
                .andExpect(jsonPath("$.balanceDisplay").value("£0.00"))
                .andExpect(jsonPath("$.typeLabel").value("Current"))
                .andExpect(jsonPath("$.statusLabel").value("Active"))
                .andExpect(jsonPath("$.canDelete").value(true));
    }

    @Test
    void anAccountWithHistoryCannotBeDeleted() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1250}"));

        api.performAs("teller", get("/api/accounts/" + account))
                .andExpect(jsonPath("$.balanceDisplay").value("£1,250.00"))
                .andExpect(jsonPath("$.canDelete").value(false));
    }

    @Test
    void entriesCarryTheTextTheTransactionsTableShows() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":25.5}"));
        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":5}"));

        api.performAs("teller", get("/api/accounts/" + account + "/transactions"))
                .andExpect(jsonPath("$[0].typeLabel").value("Withdrawal"))
                .andExpect(jsonPath("$[0].amountDisplay").value("-£5.00"))
                .andExpect(jsonPath("$[0].balanceAfterDisplay").value("£20.50"))
                .andExpect(jsonPath("$[1].amountDisplay").value("+£25.50"))
                .andExpect(jsonPath("$[1].createdAtDisplay").value(matchesPattern("\\d{1,2} [A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}")));
    }

    @Test
    void aCustomerCarriesTheTextItsPageShows() throws Exception {
        int customer = api.newCustomer();

        api.performAs("teller", get("/api/customers/" + customer))
                .andExpect(jsonPath("$.fullName").value("Api Test"))
                .andExpect(jsonPath("$.statusLabel").value("Active"));
    }

    @Test
    void theDashboardSummaryCountsActiveAccountsAndBalances() throws Exception {
        String before = dashboard();
        int account = api.newAccount();
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":10}"));
        String after = dashboard();

        assertThat((Integer) JsonPath.read(after, "$.customerCount"))
                .isEqualTo((Integer) JsonPath.read(before, "$.customerCount") + 1);
        assertThat((Integer) JsonPath.read(after, "$.activeAccountCount"))
                .isEqualTo((Integer) JsonPath.read(before, "$.activeAccountCount") + 1);
        assertThat(new BigDecimal(JsonPath.read(after, "$.totalBalance").toString()))
                .isEqualByComparingTo(new BigDecimal(JsonPath.read(before, "$.totalBalance").toString()).add(BigDecimal.TEN));
        assertThat((String) JsonPath.read(after, "$.totalBalanceDisplay")).matches("£[\\d,]+\\.\\d{2}");
    }

    @Test
    void transferTargetsAreTheOtherActiveAccounts() throws Exception {
        int from = api.newAccount();
        int to = api.newAccount();
        int frozen = api.newAccount();
        api.performAs("teller", json(patch("/api/accounts/" + frozen), "{\"status\":\"FROZEN\"}"));

        String targets = api.performAs("teller", get("/api/accounts/" + from + "/transfer-targets"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(targets, "$[*].id");
        assertThat(ids).contains(to).doesNotContain(from, frozen);
    }

    @Test
    void optionsListEveryChoiceWithItsLabelInPageOrder() throws Exception {
        api.performAs("teller", get("/api/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementTypes[2].value").value("TRANSFER"))
                .andExpect(jsonPath("$.movementTypes[2].label").value("Transfer"))
                .andExpect(jsonPath("$.entryTypes[2].label").value("Transfer in"))
                .andExpect(jsonPath("$.accountStatuses[1].label").value("Frozen"))
                .andExpect(jsonPath("$.accountTypes[1].label").value("Savings"))
                .andExpect(jsonPath("$.customerStatuses[1].label").value("Suspended"));
    }

    private String dashboard() throws Exception {
        return api.performAs("teller", get("/api/dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
