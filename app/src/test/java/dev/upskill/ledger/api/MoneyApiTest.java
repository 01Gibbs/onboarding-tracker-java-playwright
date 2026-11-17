package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/** Deposits, withdrawals and transfers: the money rules from docs/ledgerline-reference.md, over HTTP. */
@SpringBootTest(properties = "ledgerline.processing-delay-ms=0")
class MoneyApiTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void aDepositIsRecordedWithItsBalanceAfter() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"),
                        "{\"amount\":12.5,\"description\":\"  Birthday  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.balanceAfter").value(12.50))
                .andExpect(jsonPath("$.description").value("Birthday"))
                .andExpect(jsonPath("$.accountId").value(account));
    }

    @Test
    void aMissingDescriptionIsNamedAfterTheMovement() throws Exception {
        int account = api.newAccount();
        deposit(account, "10");

        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":1,\"description\":\" \"}"))
                .andExpect(jsonPath("$.description").value("Withdrawal"));
        api.performAs("teller", get("/api/accounts/" + account + "/transactions"))
                .andExpect(jsonPath("$[1].description").value("Deposit"));
    }

    @ParameterizedTest
    @CsvSource({
            "0,          Amount must be greater than zero",
            "-1,         Amount must be greater than zero",
            "1000000.01, 'Amount must not exceed 1,000,000.00'",
            "1.234,      Amount can have at most 2 decimal places",
    })
    void everyMoneyEndpointAppliesTheAmountRules(String amount, String message) throws Exception {
        int account = api.newAccount();
        deposit(account, "10");

        for (String endpoint : new String[] {"deposits", "withdrawals"}) {
            api.performAs("teller", json(post("/api/accounts/" + account + "/" + endpoint), "{\"amount\":" + amount + "}"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.fieldErrors.amount").value(message));
        }
        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":%s}".formatted(account, api.newAccount(), amount)))
                .andExpect(jsonPath("$.fieldErrors.amount").value(message));
    }

    @Test
    void aDescriptionOfExactlyEightyCharactersIsKept() throws Exception {
        int account = api.newAccount();

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"),
                        "{\"amount\":1,\"description\":\"%s\"}".formatted("x".repeat(80))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("x".repeat(80)));
    }

    @Test
    void aWithdrawalCanEmptyTheAccountButNotOverdrawIt() throws Exception {
        int account = api.newAccount();
        deposit(account, "10");

        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":10.01}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Unprocessable Content"))
                .andExpect(jsonPath("$.fieldErrors.amount").value("Insufficient funds. Available balance is £10.00"));
        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(0));
    }

    @Test
    void aTransferMovesMoneyAndLinksBothSides() throws Exception {
        int from = api.newAccount();
        int to = api.newAccount();
        deposit(from, "100");

        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":40}".formatted(from, to)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(from))
                .andExpect(jsonPath("$.balance").value(60.00));

        String toNumber = accountNumber(to);
        String fromNumber = accountNumber(from);
        api.performAs("teller", get("/api/accounts/" + to + "/transactions"))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_IN"))
                .andExpect(jsonPath("$[0].amountDisplay").value("+£40.00"))
                .andExpect(jsonPath("$[0].counterpartyAccountNumber").value(fromNumber))
                .andExpect(jsonPath("$[0].description").value("Transfer"));
        api.performAs("teller", get("/api/accounts/" + from + "/transactions?type=TRANSFER_OUT"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].counterpartyAccountNumber").value(toNumber))
                .andExpect(jsonPath("$[0].transferId").value(matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$[0].transferId").value(transferId(to)));
    }

    @Test
    void aTransferBeyondTheBalanceMovesNothing() throws Exception {
        int from = api.newAccount();
        int to = api.newAccount();
        deposit(from, "5");

        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":5.01}".formatted(from, to)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.amount").value("Insufficient funds. Available balance is £5.00"));

        api.performAs("teller", get("/api/accounts/" + to)).andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void aTransferToTheSameAccountIsRefused() throws Exception {
        int account = api.newAccount();
        deposit(account, "5");

        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1}".formatted(account, account)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").value("Cannot transfer to the same account"));
    }

    @Test
    void aTransferMovementMustSayWhereTo() throws Exception {
        int account = api.newAccount();
        deposit(account, "5");

        api.performAs("teller", json(post("/api/accounts/" + account + "/movements"), "{\"type\":\"TRANSFER\",\"amount\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").value("Choose an account to transfer to"));
    }

    @Test
    void aFrozenAccountCanNeitherSendNorReceive() throws Exception {
        int active = api.newAccount();
        int frozen = api.newAccount();
        deposit(active, "5");
        deposit(frozen, "5");
        api.performAs("teller", json(patch("/api/accounts/" + frozen), "{\"status\":\"FROZEN\"}"));
        String frozenNumber = accountNumber(frozen);

        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1}".formatted(active, frozen)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.toAccountId")
                        .value("Account " + frozenNumber + " is frozen and cannot be used"));
        api.performAs("teller", json(post("/api/accounts/" + frozen + "/deposits"), "{\"amount\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Account " + frozenNumber + " is frozen and cannot be used"));
        api.performAs("teller", json(post("/api/accounts/" + frozen + "/withdrawals"), "{\"amount\":1}"))
                .andExpect(status().isUnprocessableContent());
        api.performAs("teller", json(post("/api/transfers"),
                        "{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1}".formatted(frozen, active)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Account " + frozenNumber + " is frozen and cannot be used"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void aClosedAccountCannotTakeDeposits() throws Exception {
        int account = api.newAccount();
        api.performAs("teller", json(patch("/api/accounts/" + account), "{\"status\":\"CLOSED\"}"));

        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Account " + accountNumber(account) + " is closed and cannot be used"));
    }

    @Test
    void eachMovementReturnsTheConfirmationThePageShows() throws Exception {
        int from = api.newAccount();
        int to = api.newAccount();
        deposit(from, "100");
        String toName = "Api test (" + accountNumber(to) + ")";

        api.performAs("teller", json(post("/api/accounts/" + from + "/movements"), "{\"type\":\"WITHDRAWAL\",\"amount\":1234.5}"))
                .andExpect(status().isUnprocessableContent());
        api.performAs("teller", json(post("/api/accounts/" + from + "/movements"), "{\"type\":\"WITHDRAWAL\",\"amount\":10,\"description\":\"Cash\"}"))
                .andExpect(jsonPath("$.message").value("Withdrawal of £10.00 completed"))
                .andExpect(jsonPath("$.account.balance").value(90.00));
        api.performAs("teller", json(post("/api/accounts/" + from + "/movements"),
                        "{\"type\":\"TRANSFER\",\"amount\":20,\"toAccountId\":%d}".formatted(to)))
                .andExpect(jsonPath("$.message").value("Transfer of £20.00 to " + toName + " completed"))
                .andExpect(jsonPath("$.account.id").value(from))
                .andExpect(jsonPath("$.account.balance").value(70.00));
        api.performAs("teller", get("/api/accounts/" + from + "/transactions"))
                .andExpect(jsonPath("$[1].description").value("Cash"));
    }

    @Test
    void historyIsNewestFirstAndCanBeFilteredByType() throws Exception {
        int account = api.newAccount();
        deposit(account, "1");
        deposit(account, "2");
        api.performAs("teller", json(post("/api/accounts/" + account + "/withdrawals"), "{\"amount\":3}"));

        api.performAs("teller", get("/api/accounts/" + account + "/transactions"))
                .andExpect(jsonPath("$[*].type").value(contains("WITHDRAWAL", "DEPOSIT", "DEPOSIT")))
                .andExpect(jsonPath("$[*].balanceAfter").value(contains(0.0, 3.0, 1.0)))
                .andExpect(jsonPath("$[0].id").value(greaterThan(0)));
        api.performAs("teller", get("/api/accounts/" + account + "/transactions?type=DEPOSIT"))
                .andExpect(jsonPath("$", hasSize(2)));
        api.performAs("teller", get("/api/accounts/999999/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account 999999 was not found"));
    }

    @Test
    void recentActivityIsNewestFirstAcrossAccounts() throws Exception {
        int first = api.newAccount();
        int second = api.newAccount();
        for (int i = 0; i < 8; i++) {
            deposit(first, "1");
        }
        deposit(second, "2");

        api.performAs("teller", get("/api/activity?limit=2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountId").value(second))
                .andExpect(jsonPath("$[1].accountId").value(first));
        api.performAs("teller", get("/api/activity"))
                .andExpect(jsonPath("$", hasSize(8)));
    }

    private void deposit(int account, String amount) throws Exception {
        api.performAs("teller", json(post("/api/accounts/" + account + "/deposits"), "{\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }

    private String transferId(int account) throws Exception {
        return api.read("/api/accounts/" + account + "/transactions?type=TRANSFER_IN", "$[0].transferId");
    }

    private String accountNumber(int account) throws Exception {
        return api.read("/api/accounts/" + account, "$.accountNumber");
    }
}
