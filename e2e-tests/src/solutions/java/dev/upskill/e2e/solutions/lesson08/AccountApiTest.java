package dev.upskill.e2e.solutions.lesson08;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.RequestOptions;
import dev.upskill.e2e.solutions.support.LedgerApi;
import dev.upskill.e2e.solutions.support.User;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 08 (part 2): the same ideas wrapped in a small typed client, LedgerApi,
 * which later lessons reuse to arrange data.
 */
@UsePlaywright
class AccountApiTest {

    private LedgerApi api;

    @BeforeEach
    void logIn(Playwright playwright) {
        api = LedgerApi.loginAs(playwright, User.TELLER);
    }

    @AfterEach
    void dispose() {
        api.close();
    }

    @Test
    void newAccountsStartActiveWithZeroBalance() {
        JsonObject customer = api.createCustomer();

        JsonObject account = api.openAccount(customer.get("id").getAsLong(), "SAVINGS", "Rainy Day");

        assertEquals("ACTIVE", account.get("status").getAsString());
        assertEquals(0, account.get("balance").getAsBigDecimal().compareTo(BigDecimal.ZERO));
        assertEquals(8, account.get("accountNumber").getAsString().length());
    }

    @Test
    void depositsAndWithdrawalsUpdateTheBalance() {
        long id = api.accountWithBalance("100.00").get("id").getAsLong();

        APIResponse withdrawal = api.request().post("/api/accounts/" + id + "/withdrawals",
                RequestOptions.create().setData(Map.of("amount", 40.25)));

        assertEquals(201, withdrawal.status());
        assertEquals(new BigDecimal("59.75"), api.balanceOf(id));
        JsonArray history = api.transactions(id);
        assertEquals(2, history.size());
        assertEquals("WITHDRAWAL", history.get(0).getAsJsonObject().get("type").getAsString()); // newest first
    }

    @Test
    void cannotWithdrawMoreThanTheBalance() {
        long id = api.accountWithBalance("10.00").get("id").getAsLong();

        APIResponse response = api.request().post("/api/accounts/" + id + "/withdrawals",
                RequestOptions.create().setData(Map.of("amount", 10.01)));

        assertEquals(422, response.status());
        assertEquals("Insufficient funds. Available balance is £10.00",
                LedgerApi.json(response).get("message").getAsString());
        assertEquals(new BigDecimal("10.00"), api.balanceOf(id));
    }

    @Test
    void transfersMoveMoneyBetweenAccounts() {
        long from = api.accountWithBalance("250.00").get("id").getAsLong();
        long to = api.accountWithBalance("0").get("id").getAsLong();

        APIResponse response = api.request().post("/api/transfers", RequestOptions.create().setData(Map.of(
                "fromAccountId", from, "toAccountId", to, "amount", 75, "description", "Rent")));

        assertThat(response).isOK();
        assertEquals(new BigDecimal("175.00"), api.balanceOf(from));
        assertEquals(new BigDecimal("75.00"), api.balanceOf(to));
        String outgoing = api.transactions(from).get(0).getAsJsonObject().get("transferId").getAsString();
        String incoming = api.transactions(to).get(0).getAsJsonObject().get("transferId").getAsString();
        assertEquals(outgoing, incoming);
    }

    @Test
    void frozenAccountsRejectDeposits() {
        long id = api.accountWithBalance("5.00").get("id").getAsLong();
        api.setAccountStatus(id, "FROZEN");

        APIResponse response = api.request().post("/api/accounts/" + id + "/deposits",
                RequestOptions.create().setData(Map.of("amount", 1)));

        assertEquals(422, response.status());
    }

    @Test
    void accountsWithMoneyCannotBeClosed() {
        long id = api.accountWithBalance("1.00").get("id").getAsLong();

        APIResponse response = api.request().patch("/api/accounts/" + id,
                RequestOptions.create().setData(Map.of("status", "CLOSED")));

        assertEquals(409, response.status());
    }
}
