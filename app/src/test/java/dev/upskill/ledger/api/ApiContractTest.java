package dev.upskill.ledger.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The JSON API as a client sees it: status codes and the error shape promised in docs/ledgerline-reference.md.
 * Runs through the real interceptors and exception handlers.
 */
@SpringBootTest
class ApiContractTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void missingCredentialsGiveA401InTheErrorShape() throws Exception {
        mvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void auditorsCannotWriteAndGetA403InTheErrorShape() throws Exception {
        mvc.perform(as("auditor", post("/api/customers")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void unknownApiRoutesGiveA404InTheErrorShape() throws Exception {
        mvc.perform(as("teller", get("/api/nope")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void customerStatusChangeWithoutAStatusIsA400() throws Exception {
        mvc.perform(as("teller", json(patch("/api/customers/5/status"), "{}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("Status is required"));
    }

    @Test
    void transferWithoutAccountIdsIsA400() throws Exception {
        mvc.perform(as("teller", json(post("/api/transfers"), "{\"amount\":5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fromAccountId").exists())
                .andExpect(jsonPath("$.fieldErrors.toAccountId").exists());
    }

    @Test
    void depositsAreStoredAndEchoedInPence() throws Exception {
        String account = mvc.perform(as("teller", json(post("/api/accounts"),
                        "{\"customerId\":1,\"type\":\"CURRENT\",\"nickname\":\"Contract\"}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(account, "$.id");

        mvc.perform(as("teller", json(post("/api/accounts/" + id + "/deposits"), "{\"amount\":1E+2}")))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"amount\":100.00")));
    }

    @Test
    void testSupportIsOffUnlessTheDevProfileTurnsItOn() throws Exception {
        mvc.perform(post("/api/testing/reset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder as(String username, MockHttpServletRequestBuilder request) throws Exception {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(username));
    }

    private String token(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s-pass\"}".formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
