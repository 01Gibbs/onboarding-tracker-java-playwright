package dev.upskill.e2e.solutions.lesson08;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.RequestOptions;
import dev.upskill.e2e.solutions.support.LedgerlineOptions;
import dev.upskill.e2e.solutions.support.TestData;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lesson 08: testing the JSON API directly with the injected APIRequestContext.
 * No browser is launched for these tests - they are fast and precise.
 */
@UsePlaywright(LedgerlineOptions.class)
class CustomerApiTest {

    private Map<String, String> auth;

    @BeforeEach
    void logIn(APIRequestContext request) {
        APIResponse response = request.post("/api/auth/login", RequestOptions.create()
                .setData(Map.of("username", "teller", "password", "teller-pass")));
        assertThat(response).isOK();
        String token = JsonParser.parseString(response.text()).getAsJsonObject().get("token").getAsString();
        auth = Map.of("Authorization", "Bearer " + token);
    }

    private RequestOptions withAuth() {
        RequestOptions options = RequestOptions.create();
        auth.forEach(options::setHeader);
        return options;
    }

    @Test
    void requestsWithoutATokenAreRejected(APIRequestContext request) {
        APIResponse response = request.get("/api/customers");

        assertEquals(401, response.status());
    }

    @Test
    void listCustomersAndFilterByStatus(APIRequestContext request) {
        APIResponse response = request.get("/api/customers", withAuth().setQueryParam("status", "SUSPENDED"));

        assertThat(response).isOK();
        JsonArray customers = JsonParser.parseString(response.text()).getAsJsonArray();
        assertTrue(customers.size() >= 1);
        customers.forEach(c -> assertEquals("SUSPENDED", c.getAsJsonObject().get("status").getAsString()));
    }

    @Test
    void createReadUpdateDeleteACustomer(APIRequestContext request) {
        String email = TestData.uniqueEmail("api");

        // Create
        APIResponse created = request.post("/api/customers", withAuth().setData(Map.of(
                "firstName", "Api", "lastName", "Created", "email", email)));
        assertEquals(201, created.status());
        JsonObject customer = JsonParser.parseString(created.text()).getAsJsonObject();
        long id = customer.get("id").getAsLong();
        assertEquals(email, customer.get("email").getAsString());
        assertTrue(customer.get("reference").getAsString().startsWith("CUS-"));
        assertEquals("/api/customers/" + id, created.headers().get("location"));

        // Read
        APIResponse fetched = request.get("/api/customers/" + id, withAuth());
        assertThat(fetched).isOK();

        // Update
        APIResponse updated = request.put("/api/customers/" + id, withAuth().setData(Map.of(
                "firstName", "Api", "lastName", "Renamed", "email", email)));
        assertThat(updated).isOK();
        assertEquals("Renamed", JsonParser.parseString(updated.text()).getAsJsonObject()
                .get("lastName").getAsString());

        // Delete
        assertEquals(204, request.delete("/api/customers/" + id, withAuth()).status());
        assertEquals(404, request.get("/api/customers/" + id, withAuth()).status());
    }

    @Test
    void validationErrorsListEachInvalidField(APIRequestContext request) {
        APIResponse response = request.post("/api/customers", withAuth().setData(Map.of(
                "firstName", "", "lastName", "Valid", "email", "nope")));

        assertEquals(400, response.status());
        JsonObject fieldErrors = JsonParser.parseString(response.text()).getAsJsonObject()
                .getAsJsonObject("fieldErrors");
        assertEquals("First name is required", fieldErrors.get("firstName").getAsString());
        assertEquals("Enter a valid email address", fieldErrors.get("email").getAsString());
    }

    @Test
    void duplicateEmailIsAConflict(APIRequestContext request) {
        APIResponse response = request.post("/api/customers", withAuth().setData(Map.of(
                "firstName", "Alice", "lastName", "Clone", "email", "alice.archer@example.test")));

        assertEquals(409, response.status());
    }

    @Test
    void auditorsCanReadButNotWrite(APIRequestContext request) {
        APIResponse login = request.post("/api/auth/login", RequestOptions.create()
                .setData(Map.of("username", "auditor", "password", "auditor-pass")));
        String token = JsonParser.parseString(login.text()).getAsJsonObject().get("token").getAsString();
        RequestOptions asAuditor = RequestOptions.create().setHeader("Authorization", "Bearer " + token);

        assertThat(request.get("/api/customers", asAuditor)).isOK();
        APIResponse write = request.post("/api/customers", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + token)
                .setData(Map.of("firstName", "No", "lastName", "Way", "email", TestData.uniqueEmail("no"))));
        assertEquals(403, write.status());
    }
}
