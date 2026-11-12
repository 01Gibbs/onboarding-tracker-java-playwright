package dev.upskill.ledger.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Drives the JSON API through the real interceptors and exception handlers, and creates fresh data so
 * tests never change the seeded customers and accounts.
 */
final class ApiTestClient {

    private final MockMvc mvc;

    ApiTestClient(WebApplicationContext context) {
        this.mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request);
    }

    /** Sends {@code request} as the seeded user {@code username} (password {@code <username>-pass}). */
    ResultActions performAs(String username, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(username)));
    }

    /** A new customer with a unique email; returns its id. */
    int newCustomer() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String body = performAs("teller", json(post("/api/customers"),
                "{\"firstName\":\"Api\",\"lastName\":\"Test\",\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** A new, empty CURRENT account for a new customer; returns its id. */
    int newAccount() throws Exception {
        String body = performAs("teller", json(post("/api/accounts"),
                "{\"customerId\":%d,\"type\":\"CURRENT\",\"nickname\":\"Api test\"}".formatted(newCustomer())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String token(String username) throws Exception {
        String body = mvc.perform(json(post("/api/auth/login"),
                        "{\"username\":\"%s\",\"password\":\"%s-pass\"}".formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
