package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

/** /api/customers: the customer rules from docs/ledgerline-reference.md, as a client meets them. */
@SpringBootTest
class CustomerApiTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void aNewCustomerIsTrimmedGivenAReferenceAndLocated() throws Exception {
        String email = UUID.randomUUID() + "@example.test";

        String body = api.performAs("teller", json(post("/api/customers"),
                        "{\"firstName\":\" Ada \",\"lastName\":\" Lovelace \",\"email\":\"%s\",\"phone\":\"\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountCount").value(0))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.createdAtDisplay").value(matchesPattern("\\d{1,2} [A-Z][a-z]{2} \\d{4}")))
                .andReturn().getResponse().getContentAsString();

        int id = JsonPath.read(body, "$.id");
        api.performAs("teller", get("/api/customers/" + id))
                .andExpect(jsonPath("$.reference").value("CUS-%04d".formatted(id)));
    }

    @Test
    void aNewCustomerIsFoundAtItsLocation() throws Exception {
        api.performAs("teller", json(post("/api/customers"), customer("")))
                .andExpect(header().string("Location", matchesPattern("/api/customers/\\d+")));
    }

    @Test
    void anEmailCanOnlyBelongToOneCustomerWhateverItsCase() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        api.performAs("teller", json(post("/api/customers"), customer("Api", email))).andExpect(status().isCreated());

        api.performAs("teller", json(post("/api/customers"), customer("Other", email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").value("A customer with this email already exists"));
    }

    @Test
    void missingAndInvalidFieldsAreAllReported() throws Exception {
        api.performAs("teller", json(post("/api/customers"), "{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.fieldErrors.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address"));
    }

    @Test
    void anUpdateReplacesTheDetailsAndMayKeepTheSameEmail() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        int id = create(customer("Api", email));

        api.performAs("teller", json(put("/api/customers/" + id),
                        "{\"firstName\":\"New\",\"lastName\":\" Name \",\"email\":\"%s\",\"phone\":\"0123 456 789\"}"
                                .formatted(email.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.email").value(email.toUpperCase()))
                .andExpect(jsonPath("$.phone").value("0123 456 789"));
        api.performAs("teller", get("/api/customers/" + id))
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void anUpdateCannotTakeAnotherCustomersEmail() throws Exception {
        String taken = UUID.randomUUID() + "@example.test";
        create(customer("Api", taken));
        int id = api.newCustomer();

        api.performAs("teller", json(put("/api/customers/" + id), customer("Api", taken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").value("A customer with this email already exists"));
    }

    @Test
    void anUnknownCustomerIsA404() throws Exception {
        api.performAs("teller", get("/api/customers/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer 999999 was not found"));
        api.performAs("teller", json(put("/api/customers/999999"), customer("")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSuspendedCustomerIsListedUnderSuspendedAndCannotOpenAccounts() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        int id = create(customer("Api", email));

        api.performAs("teller", json(patch("/api/customers/" + id + "/status"), "{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.statusLabel").value("Suspended"));

        api.performAs("teller", get("/api/customers").param("q", email).param("status", "SUSPENDED"))
                .andExpect(jsonPath("$[*].id").value(contains(id)));
        api.performAs("teller", get("/api/customers").param("q", email).param("status", "ACTIVE"))
                .andExpect(jsonPath("$", hasSize(0)));
        api.performAs("teller", json(post("/api/accounts"),
                        "{\"customerId\":%d,\"type\":\"SAVINGS\",\"nickname\":\"Nope\"}".formatted(id)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.customerId").value("Accounts cannot be opened for a suspended customer"));
    }

    @Test
    void searchMatchesFullNameOrEmailIgnoringCase() throws Exception {
        String email = UUID.randomUUID() + "@Example.test";
        int id = create("{\"firstName\":\"Zebedee\",\"lastName\":\"Quixote\",\"email\":\"%s\"}".formatted(email));

        api.performAs("teller", get("/api/customers").param("q", "  zebedee quix "))
                .andExpect(jsonPath("$[*].id").value(contains(id)));
        api.performAs("teller", get("/api/customers").param("q", email.toLowerCase()))
                .andExpect(jsonPath("$[*].id").value(contains(id)));
    }

    @Test
    void aCustomerWithoutAccountsCanBeDeleted() throws Exception {
        int id = api.newCustomer();

        api.performAs("teller", delete("/api/customers/" + id)).andExpect(status().isNoContent());
        api.performAs("teller", get("/api/customers/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void aCustomerWithAccountsCannotBeDeleted() throws Exception {
        int id = api.read("/api/accounts/" + api.newAccount(), "$.customerId");

        api.performAs("teller", delete("/api/customers/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer cannot be deleted while they have accounts"));
        api.performAs("teller", get("/api/customers/" + id))
                .andExpect(jsonPath("$.accountCount").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+12345678901234567890", "+44 7700 900 123 456 789 01"})
    void aPhoneWithUpToTwentyDigitsIsKept(String phone) throws Exception {
        api.performAs("teller", json(post("/api/customers"), customer(phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1      ", "+123456", "123456789012345678901", "12-34-56-78"})
    void aPhoneWithoutSevenToTwentyDigitsIsRejected(String phone) throws Exception {
        api.performAs("teller", json(post("/api/customers"), customer(phone)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Phone must contain 7-20 digits and may start with +"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"_", "%"})
    void searchTreatsPercentAndUnderscoreAsPlainText(String wildcard) throws Exception {
        String unique = UUID.randomUUID().toString();
        int literal = create(customer("Api", unique + "a" + wildcard + "b@example.test"));
        create(customer("Api", unique + "axb@example.test"));

        api.performAs("teller", get("/api/customers").param("q", unique + "a" + wildcard + "b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(literal)));
    }

    private int create(String customer) throws Exception {
        String body = api.performAs("teller", json(post("/api/customers"), customer))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String customer(String phone) {
        return "{\"firstName\":\"Api\",\"lastName\":\"Test\",\"email\":\"%s@example.test\",\"phone\":\"%s\"}"
                .formatted(UUID.randomUUID(), phone);
    }

    private static String customer(String firstName, String email) {
        return "{\"firstName\":\"%s\",\"lastName\":\"Test\",\"email\":\"%s\"}".formatted(firstName, email);
    }
}
