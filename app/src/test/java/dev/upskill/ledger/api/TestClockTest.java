package dev.upskill.ledger.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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

/** Tests can pin the server's clock, so time-based rules (cut-offs, timeouts, statements) are testable. */
@SpringBootTest(properties = "ledgerline.test-support-enabled=true")
class TestClockTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String login = mvc.perform(json(post("/api/auth/login"), "{\"username\":\"teller\",\"password\":\"teller-pass\"}"))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(login, "$.token");
    }

    @AfterEach
    void releaseClock() throws Exception {
        mvc.perform(delete("/api/testing/clock")).andExpect(status().isNoContent());
    }

    @Test
    void entriesAreStampedWithThePinnedTime() throws Exception {
        mvc.perform(json(put("/api/testing/clock"), "{\"instant\":\"2030-01-31T16:59:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instant").value("2030-01-31T16:59:00Z"));

        mvc.perform(teller(json(post("/api/accounts/1/deposits"), "{\"amount\":1}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").value("2030-01-31T16:59:00Z"));
    }

    private MockHttpServletRequestBuilder teller(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
