package dev.upskill.e2e.workshop.support;

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
import java.util.Map;

/**
 * Lesson 08: a small typed client for the JSON API. loginAs, createCustomer and json are done for you -
 * follow the same pattern for the rest. See docs/ledgerline-reference.md for endpoints and status codes.
 */
public class LedgerApi implements AutoCloseable {

    private final APIRequestContext request;

    private LedgerApi(APIRequestContext request) {
        this.request = request;
    }

    /** Logs in through POST /api/auth/login and returns a client that sends the bearer token. */
    public static LedgerApi loginAs(Playwright playwright, User user) {
        APIRequestContext anonymous = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(LedgerlineOptions.BASE_URL));
        String token;
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

    /** The underlying context, for requests this class doesn't wrap. */
    public APIRequestContext request() {
        return request;
    }

    public JsonObject createCustomer(String firstName, String lastName, String email) {
        APIResponse response = request.post("/api/customers", RequestOptions.create().setData(Map.of(
                "firstName", firstName, "lastName", lastName, "email", email)));
        if (response.status() != 201) {
            throw new AssertionError("Expected 201 Created but got " + response.status() + ": " + response.text());
        }
        return json(response);
    }

    /** A customer with unique, obviously-fake details. */
    public JsonObject createCustomer() {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonObject openAccount(long customerId, String type, String nickname) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    /** A new customer with one CURRENT account holding openingBalance. */
    public JsonObject accountWithBalance(String openingBalance) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonObject deposit(long accountId, String amount) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonObject withdraw(long accountId, String amount, String description) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonObject setAccountStatus(long accountId, String status) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonObject getAccount(long accountId) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public BigDecimal balanceOf(long accountId) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    public JsonArray transactions(long accountId) {
        throw new UnsupportedOperationException("TODO lesson 08");
    }

    // Mission B0 (lessons/B0-ledger-invariants.md) adds these.

    /** POST /api/transfers. Returns the source account. */
    public JsonObject transfer(long fromAccountId, long toAccountId, String amount) {
        throw new UnsupportedOperationException("TODO mission B0");
    }

    /** GET /api/accounts with no filter, so closed accounts are included too. */
    public JsonArray allAccounts() {
        throw new UnsupportedOperationException("TODO mission B0");
    }

    /** True when the app exposes /api/testing/* (the dev profile, which spring-boot:run turns on). */
    public boolean testSupportEnabled() {
        throw new UnsupportedOperationException("TODO mission B0");
    }

    /** PUT /api/testing/faults/{fault} with {"accountId": ...}. */
    public void plantFault(String fault, long accountId) {
        throw new UnsupportedOperationException("TODO mission B0");
    }

    /** DELETE /api/testing/faults/{fault}?accountId=... */
    public void clearFault(String fault, long accountId) {
        throw new UnsupportedOperationException("TODO mission B0");
    }

    @Override
    public void close() {
        request.dispose();
    }

    public static JsonObject json(APIResponse response) {
        return JsonParser.parseString(response.text()).getAsJsonObject();
    }
}
