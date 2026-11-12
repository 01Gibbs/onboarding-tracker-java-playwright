package dev.upskill.e2e.solutions.support;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A thin, typed client for the Ledgerline JSON API (introduced in lesson 08).
 * Used to arrange test data quickly instead of clicking through the UI.
 */
public class LedgerApi implements AutoCloseable {

    private final APIRequestContext request;

    private LedgerApi(APIRequestContext request) {
        this.request = request;
    }

    /** Logs in through POST /api/auth/login and returns a client that sends the bearer token. */
    public static LedgerApi loginAs(Playwright playwright, User user) {
        String token;
        APIRequestContext anonymous = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(LedgerlineOptions.BASE_URL));
        try {
            APIResponse response = anonymous.post("/api/auth/login", RequestOptions.create()
                    .setData(Map.of("username", user.username(), "password", user.password())));
            assertThat(response).isOK();
            token = json(response).get("token").getAsString();
        } finally {
            anonymous.dispose();
        }
        return new LedgerApi(playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(LedgerlineOptions.BASE_URL)
                .setExtraHTTPHeaders(Map.of("Authorization", "Bearer " + token))));
    }

    public APIRequestContext request() {
        return request;
    }

    public JsonObject createCustomer(String firstName, String lastName, String email) {
        return created(request.post("/api/customers", RequestOptions.create().setData(Map.of(
                "firstName", firstName, "lastName", lastName, "email", email))));
    }

    /** Creates a customer with unique, obviously-fake details. */
    public JsonObject createCustomer() {
        return createCustomer("Test", TestData.uniqueLastName(), TestData.uniqueEmail("customer"));
    }

    public JsonObject openAccount(long customerId, String type, String nickname) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerId", customerId);
        body.put("type", type);
        body.put("nickname", nickname);
        return created(request.post("/api/accounts", RequestOptions.create().setData(body)));
    }

    /** Creates a fresh customer with one CURRENT account holding {@code openingBalance}. */
    public JsonObject accountWithBalance(String openingBalance) {
        JsonObject customer = createCustomer();
        JsonObject account = openAccount(customer.get("id").getAsLong(), "CURRENT", "Test " + TestData.unique());
        if (new BigDecimal(openingBalance).signum() > 0) {
            deposit(account.get("id").getAsLong(), openingBalance);
        }
        return getAccount(account.get("id").getAsLong());
    }

    public JsonObject deposit(long accountId, String amount) {
        return created(request.post("/api/accounts/" + accountId + "/deposits",
                RequestOptions.create().setData(Map.of("amount", new BigDecimal(amount), "description", "API deposit"))));
    }

    public JsonObject withdraw(long accountId, String amount, String description) {
        return created(request.post("/api/accounts/" + accountId + "/withdrawals",
                RequestOptions.create().setData(Map.of("amount", new BigDecimal(amount), "description", description))));
    }

    public JsonObject setAccountStatus(long accountId, String status) {
        APIResponse response = request.patch("/api/accounts/" + accountId,
                RequestOptions.create().setData(Map.of("status", status)));
        assertThat(response).isOK();
        return json(response);
    }

    public JsonObject getAccount(long accountId) {
        APIResponse response = request.get("/api/accounts/" + accountId);
        assertThat(response).isOK();
        return json(response);
    }

    public BigDecimal balanceOf(long accountId) {
        return getAccount(accountId).get("balance").getAsBigDecimal();
    }

    public JsonArray transactions(long accountId) {
        APIResponse response = request.get("/api/accounts/" + accountId + "/transactions");
        assertThat(response).isOK();
        return JsonParser.parseString(response.text()).getAsJsonArray();
    }

    /** Moves money between two accounts (mission B0). Returns the source account. */
    public JsonObject transfer(long fromAccountId, long toAccountId, String amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromAccountId", fromAccountId);
        body.put("toAccountId", toAccountId);
        body.put("amount", new BigDecimal(amount));
        return created(request.post("/api/transfers", RequestOptions.create().setData(body)));
    }

    /** Every account, including closed ones (mission B0 uses it to find the other leg of a transfer). */
    public JsonArray allAccounts() {
        APIResponse response = request.get("/api/accounts");
        assertThat(response).isOK();
        return JsonParser.parseString(response.text()).getAsJsonArray();
    }

    /** True when the app exposes /api/testing/*, which `./mvnw -pl app spring-boot:run` turns on. */
    public boolean testSupportEnabled() {
        return request.delete("/api/testing/faults/overdraft?accountId=0").status() == 204;
    }

    /** Arms one of the app's planted faults (mission B0) for a single account. Needs test support. */
    public void plantFault(String fault, long accountId) {
        APIResponse response = request.put("/api/testing/faults/" + fault,
                RequestOptions.create().setData(Map.of("accountId", accountId)));
        assertThat(response).isOK();
    }

    public void clearFault(String fault, long accountId) {
        APIResponse response = request.delete("/api/testing/faults/" + fault + "?accountId=" + accountId);
        if (response.status() != 204) {
            throw new AssertionError("Could not clear fault " + fault + ": " + response.status() + " " + response.text());
        }
    }

    @Override
    public void close() {
        request.dispose();
    }

    public static JsonObject json(APIResponse response) {
        return JsonParser.parseString(response.text()).getAsJsonObject();
    }

    private static JsonObject created(APIResponse response) {
        if (response.status() != 201) {
            throw new AssertionError("Expected 201 Created but got " + response.status() + ": " + response.text());
        }
        return json(response);
    }
}
