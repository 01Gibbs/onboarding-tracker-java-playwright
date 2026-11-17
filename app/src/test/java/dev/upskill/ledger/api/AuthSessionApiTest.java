package dev.upskill.ledger.api;

import static dev.upskill.ledger.api.ApiTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

/**
 * Signing in with JSON for the React and Angular front ends (docs/ui-contract.md): the same server session the Thymeleaf
 * sign-in form creates, so cookies, storageState and page.request work the same in every track.
 */
@SpringBootTest
class AuthSessionApiTest {

    @Autowired
    WebApplicationContext context;

    ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(context);
    }

    @Test
    void signingInStartsASessionThatTheApiAccepts() throws Exception {
        MockHttpSession session = signIn("teller", "teller-pass");

        api.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teller"))
                .andExpect(jsonPath("$.displayName").value("Terry Teller"))
                .andExpect(jsonPath("$.role").value("TELLER"))
                .andExpect(jsonPath("$.roleLabel").value("Teller"))
                .andExpect(jsonPath("$.canEdit").value(true));
        api.perform(json(post("/api/accounts/" + api.newAccount() + "/deposits"), "{\"amount\":1}").session(session))
                .andExpect(status().isCreated());
    }

    @Test
    void theSessionAlsoOpensTheThymeleafPages() throws Exception {
        api.perform(get("/customers").session(signIn("auditor", "auditor-pass"))).andExpect(status().isOk());
    }

    @Test
    void anAuditorsSessionIsReadOnly() throws Exception {
        MockHttpSession auditor = signIn("auditor", "auditor-pass");

        api.perform(get("/api/auth/me").session(auditor))
                .andExpect(jsonPath("$.canEdit").value(false));
        api.perform(json(post("/api/accounts/1/deposits"), "{\"amount\":1}").session(auditor))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongOrLockedCredentialsAreA401WithTheSignInPagesMessage() throws Exception {
        api.perform(json(post("/api/auth/session"), "{\"username\":\"teller\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        api.perform(json(post("/api/auth/session"), "{\"username\":\"locked\",\"password\":\"locked-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("This user is locked. Contact an administrator."));
        api.perform(json(post("/api/auth/session"), "{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usernamesAreTrimmedLikeTheSignInForm() throws Exception {
        api.perform(json(post("/api/auth/session"), "{\"username\":\" teller \",\"password\":\"teller-pass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nobodySignedInIsA401() throws Exception {
        api.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void signingInAgainStartsAFreshSession() throws Exception {
        MockHttpSession first = signIn("teller", "teller-pass");

        MockHttpSession second = (MockHttpSession) api.perform(json(post("/api/auth/session"),
                        "{\"username\":\"auditor\",\"password\":\"auditor-pass\"}").session(first))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        assertThat(first.isInvalid()).isTrue();
        api.perform(get("/api/auth/me").session(second)).andExpect(jsonPath("$.username").value("auditor"));
    }

    @Test
    void anyoneSignedInCanSignOut() throws Exception {
        MockHttpSession auditor = signIn("auditor", "auditor-pass");

        api.perform(delete("/api/auth/session").session(auditor)).andExpect(status().isNoContent());

        assertThat(auditor.isInvalid()).isTrue();
        api.perform(delete("/api/auth/session")).andExpect(status().isNoContent());
    }

    private MockHttpSession signIn(String username, String password) throws Exception {
        return (MockHttpSession) api.perform(json(post("/api/auth/session"),
                        "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn().getRequest().getSession(false);
    }
}
