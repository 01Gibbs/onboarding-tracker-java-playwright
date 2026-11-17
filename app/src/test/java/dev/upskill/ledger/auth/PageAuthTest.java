package dev.upskill.ledger.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Who may see and change the HTML pages: the server-side half of "auditors cannot change anything". */
@SpringBootTest
class PageAuthTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void anonymousVisitorsAreSentToSignIn() throws Exception {
        mvc.perform(get("/customers")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void aSignedInUserSeesPagesAsThemselves() throws Exception {
        mvc.perform(get("/customers").session(signIn("auditor")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentUser", new CurrentUser("auditor", "Alex Auditor", Role.AUDITOR)));
    }

    @Test
    void auditorsGetA403WhenPostingAPageFormDirectly() throws Exception {
        // An unknown customer, so a broken check could never change real data: the interceptor refuses first.
        mvc.perform(post("/customers/999999/status").param("status", "SUSPENDED").session(signIn("auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorsCanStillSignOut() throws Exception {
        mvc.perform(post("/logout").session(signIn("auditor"))).andExpect(redirectedUrl("/login"));
    }

    @Test
    void aLockedUserStaysOnTheSignInPage() throws Exception {
        mvc.perform(post("/login").param("username", "locked").param("password", "locked-pass"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "This user is locked. Contact an administrator."));
    }

    private MockHttpSession signIn(String username) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").param("username", username).param("password", username + "-pass"))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
    }
}
