package dev.upskill.ledger.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService.Summary;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.MovementType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Thymeleaf front end's page flows, as the Java track's tests see them: where each form goes, what it says on
 * success, and where a refused change shows its message. Each test makes its own customer, so seeded data is never
 * changed.
 */
@SpringBootTest
class PageFlowTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;
    MockHttpSession teller;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        teller = (MockHttpSession) mvc.perform(post("/login").param("username", "teller").param("password", "teller-pass"))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
    }

    @Nested
    class EveryPage {

        @Test
        void theDashboardShowsTheBankSummary() throws Exception {
            MvcResult result = mvc.perform(get("/").session(teller)).andExpect(view().name("dashboard")).andReturn();
            Summary summary = (Summary) result.getModelAndView().getModel().get("summary");
            assertThat(summary.customerCount()).isGreaterThanOrEqualTo(5);   // the seeded five, plus any made by tests
            assertThat(summary.activeAccountCount()).isPositive();
        }

        @Test
        void everyPageKnowsWhereItIsAndTheCurrency() throws Exception {
            // currentPath marks the nav link for this page (aria-current), currencySymbol labels amounts
            mvc.perform(get("/customers").session(teller))
                    .andExpect(model().attribute("currentPath", "/customers"))
                    .andExpect(model().attribute("currencySymbol", "£"))
                    .andExpect(content().string(containsString("aria-current=\"page\"")));
        }
    }

    @Nested
    class SigningInAndOut {

        @Test
        void spacesAroundTheUsernameDoNotStopASignIn() throws Exception {
            mvc.perform(post("/login").param("username", " teller ").param("password", "teller-pass"))
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        void aSignedInUserIsSentFromTheSignInPageToTheDashboard() throws Exception {
            mvc.perform(get("/login").session(teller)).andExpect(redirectedUrl("/"));
        }

        @Test
        void aWrongPasswordKeepsTheUsernameAndSaysWhy() throws Exception {
            mvc.perform(post("/login").param("username", " teller ").param("password", "nope"))
                    .andExpect(view().name("login"))
                    .andExpect(model().attribute("error", "Invalid username or password"))
                    .andExpect(model().attribute("username", " teller "));
        }

        @Test
        void signingOutSaysSo() throws Exception {
            mvc.perform(post("/logout").session(teller))
                    .andExpect(redirectedUrl("/login"))
                    .andExpect(flash().attribute("info", "You have been signed out"));
            mvc.perform(get("/").session(teller)).andExpect(redirectedUrl("/login"));
        }
    }

    @Nested
    class Customers {

        @Test
        void theNewCustomerFormStartsEmpty() throws Exception {
            mvc.perform(get("/customers/new").session(teller))
                    .andExpect(view().name("customers/form"))
                    .andExpect(model().attribute("form", hasProperty("email", nullValue())));
        }

        @Test
        void creatingACustomerOpensTheirPageWithAMessage() throws Exception {
            String last = uniqueName();
            mvc.perform(customerForm(post("/customers"), "Grace", last, uniqueEmail()))
                    .andExpect(redirectedUrlPattern("/customers/{id}"))
                    .andExpect(flash().attribute("success", "Customer Grace " + last + " created"));
        }

        @Test
        void aMissingFieldShowsTheFormAgainWithTheError() throws Exception {
            MvcResult result = mvc.perform(customerForm(post("/customers"), "", uniqueName(), "not-an-email"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customers/form"))
                    .andReturn();
            assertThat(fieldError(result, "form", "firstName")).isEqualTo("First name is required");
            assertThat(fieldError(result, "form", "email")).isEqualTo("Enter a valid email address");
        }

        @Test
        void aTakenEmailIsShownNextToTheEmailField() throws Exception {
            String email = uniqueEmail();
            createCustomer(email);
            MvcResult result = mvc.perform(customerForm(post("/customers"), "Other", uniqueName(), email))
                    .andExpect(view().name("customers/form"))
                    .andReturn();
            assertThat(fieldError(result, "form", "email")).isEqualTo("A customer with this email already exists");
        }

        @Test
        void editingSavesAndSaysSo() throws Exception {
            long id = createCustomer(uniqueEmail());
            String last = uniqueName();
            mvc.perform(customerForm(post("/customers/" + id), "Ada", last, uniqueEmail()))
                    .andExpect(redirectedUrl("/customers/" + id))
                    .andExpect(flash().attribute("success", "Customer Ada " + last + " updated"));
        }

        @Test
        void anInvalidEditKeepsTheCustomerOnTheForm() throws Exception {
            long id = createCustomer(uniqueEmail());
            mvc.perform(get("/customers/" + id + "/edit").session(teller))
                    .andExpect(view().name("customers/form"))
                    .andExpect(model().attributeExists("form"));
            MvcResult result = mvc.perform(customerForm(post("/customers/" + id), "Ada", "", uniqueEmail()))
                    .andExpect(view().name("customers/form"))
                    .andExpect(model().attributeExists("customer"))
                    .andReturn();
            assertThat(fieldError(result, "form", "lastName")).isEqualTo("Last name is required");
        }

        @Test
        void editingToAnotherCustomersEmailIsRefusedOnTheEmailField() throws Exception {
            String taken = uniqueEmail();
            createCustomer(taken);
            long id = createCustomer(uniqueEmail());
            MvcResult result = mvc.perform(customerForm(post("/customers/" + id), "Ada", uniqueName(), taken))
                    .andExpect(view().name("customers/form"))
                    .andReturn();
            assertThat(fieldError(result, "form", "email")).isEqualTo("A customer with this email already exists");
        }

        @Test
        void suspendingSaysTheNewStatus() throws Exception {
            long id = createCustomer(uniqueEmail());
            mvc.perform(post("/customers/" + id + "/status").param("status", "SUSPENDED").session(teller))
                    .andExpect(redirectedUrl("/customers/" + id))
                    .andExpect(flash().attribute("success", "Customer Grace " + customer(id).getLastName() + " is now suspended"));
        }

        @Test
        void deletingACustomerWithoutAccountsReturnsToTheList() throws Exception {
            long id = createCustomer(uniqueEmail());
            String name = customer(id).getFullName();
            mvc.perform(post("/customers/" + id + "/delete").session(teller))
                    .andExpect(redirectedUrl("/customers"))
                    .andExpect(flash().attribute("success", "Customer " + name + " deleted"));
            mvc.perform(get("/customers/" + id).session(teller)).andExpect(status().isNotFound());
        }

        @Test
        void aCustomerWithAccountsCannotBeDeleted() throws Exception {
            long id = createCustomer(uniqueEmail());
            openAccount(id, "Spending");
            mvc.perform(post("/customers/" + id + "/delete").session(teller))
                    .andExpect(redirectedUrl("/customers/" + id))
                    .andExpect(flash().attribute("error", "Customer cannot be deleted while they have accounts"));
        }

        @Test
        void searchingFiltersTheList() throws Exception {
            long id = createCustomer(uniqueEmail());
            String last = customer(id).getLastName();
            MvcResult result = mvc.perform(get("/customers").param("q", last).session(teller))
                    .andExpect(view().name("customers/list"))
                    .andExpect(model().attribute("q", last))
                    .andReturn();
            assertThat(PageFlowTest.<Customer>listOf(result, "customers")).extracting(Customer::getId).containsExactly(id);
        }

        @Test
        void theListFiltersByStatus() throws Exception {
            long id = createCustomer(uniqueEmail());
            suspend(id);
            MvcResult result = mvc.perform(get("/customers").param("status", "SUSPENDED").session(teller))
                    .andExpect(model().attribute("status", CustomerStatus.SUSPENDED))
                    .andReturn();
            List<Customer> customers = listOf(result, "customers");
            assertThat(customers).extracting(Customer::getId).contains(id);
            assertThat(customers).allSatisfy(c -> assertThat(c.getStatus()).isEqualTo(CustomerStatus.SUSPENDED));
        }
    }

    @Nested
    class Accounts {

        @Test
        void openingAnAccountShowsItWithItsNumber() throws Exception {
            long customerId = createCustomer(uniqueEmail());
            mvc.perform(get("/accounts/new").param("customerId", String.valueOf(customerId)).session(teller))
                    .andExpect(view().name("accounts/form"))
                    .andExpect(model().attribute("form", hasProperty("customerId", is(customerId))))
                    .andExpect(model().attributeExists("customers", "types"));
            MvcResult opened = mvc.perform(post("/accounts").param("customerId", String.valueOf(customerId))
                            .param("type", "SAVINGS").param("nickname", "Goals").session(teller))
                    .andExpect(redirectedUrlPattern("/accounts/{id}"))
                    .andReturn();
            Account account = account(idFrom(opened));
            assertThat(opened.getFlashMap().get("success")).isEqualTo("Account " + account.getAccountNumber() + " opened");
            assertThat(account.getNickname()).isEqualTo("Goals");
            assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        }

        @Test
        void anIncompleteAccountFormKeepsTheChoicesOnScreen() throws Exception {
            MvcResult result = mvc.perform(post("/accounts").param("type", "SAVINGS").param("nickname", "Goals").session(teller))
                    .andExpect(view().name("accounts/form"))
                    .andExpect(model().attributeExists("customers", "types"))
                    .andReturn();
            assertThat(fieldError(result, "form", "customerId")).isEqualTo("Choose an account holder");
        }

        @Test
        void aSuspendedCustomerCannotOpenAnAccount() throws Exception {
            long customerId = createCustomer(uniqueEmail());
            suspend(customerId);
            MvcResult result = mvc.perform(post("/accounts").param("customerId", String.valueOf(customerId))
                            .param("type", "CURRENT").param("nickname", "Spending").session(teller))
                    .andExpect(view().name("accounts/form"))
                    .andExpect(model().attributeExists("customers"))
                    .andReturn();
            assertThat(fieldError(result, "form", "customerId"))
                    .isEqualTo("Accounts cannot be opened for a suspended customer");
        }

        @Test
        void aDepositSaysWhatHappenedAndChangesTheBalance() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            mvc.perform(move(id, "DEPOSIT", "25.50"))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("success", "Deposit of £25.50 completed"));
            assertThat(account(id).getBalance()).isEqualByComparingTo("25.50");
        }

        @Test
        void aRefusedWithdrawalStaysOnTheAccountWithTheReason() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            MvcResult result = mvc.perform(move(id, "WITHDRAWAL", "10.00"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("accounts/detail"))
                    .andExpect(model().attributeExists("account", "entries", "transferTargets"))
                    .andReturn();
            assertThat(fieldError(result, "movement", "amount")).startsWith("Insufficient funds");
        }

        @Test
        void anInvalidAmountIsShownOnTheAmountField() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            MvcResult result = mvc.perform(move(id, "DEPOSIT", "12.345"))
                    .andExpect(view().name("accounts/detail"))
                    .andReturn();
            assertThat(fieldError(result, "movement", "amount")).isEqualTo("Amount can have at most 2 decimal places");
        }

        @Test
        void aMovementWithNoTypeIsRefusedBeforeAnythingMoves() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            // A browser always sends a type (Deposit is preselected), but a hand-made request mustn't crash the page
            MvcResult result = mvc.perform(post("/accounts/" + id + "/movements").param("amount", "5").session(teller))
                    .andExpect(status().isOk())
                    .andExpect(view().name("accounts/detail"))
                    .andExpect(content().string(containsString("Choose a type")))
                    .andReturn();
            assertThat(fieldError(result, "movement", "type")).isEqualTo("Choose a type");
            assertThat(account(id).getBalance()).isEqualByComparingTo("0");
        }

        @Test
        void theDetailPageStartsWithADepositAndCanFilterHistory() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            deposit(id, "5.00");
            MvcResult result = mvc.perform(get("/accounts/" + id).param("entryType", "WITHDRAWAL").session(teller))
                    .andExpect(model().attribute("entryType", EntryType.WITHDRAWAL))
                    .andExpect(model().attribute("canDelete", false))
                    .andExpect(model().attribute("movement", hasProperty("type", is(MovementType.DEPOSIT))))
                    .andReturn();
            assertThat(listOf(result, "entries")).isEmpty();
        }

        @Test
        void renamingSaysSoAndATooLongNicknameIsRefused() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            mvc.perform(post("/accounts/" + id + "/nickname").param("nickname", "Bills").session(teller))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("success", "Nickname updated"));
            mvc.perform(post("/accounts/" + id + "/nickname").param("nickname", "x".repeat(41)).session(teller))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("error", "Nickname must be between 1 and 40 characters"));
            assertThat(account(id).getNickname()).isEqualTo("Bills");
        }

        @Test
        void freezingSaysTheNewStatus() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            mvc.perform(post("/accounts/" + id + "/status").param("status", "FROZEN").session(teller))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("success", "Account " + account(id).getAccountNumber() + " is now frozen"));
        }

        @Test
        void anAccountWithMoneyCannotBeClosed() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            deposit(id, "1.00");
            mvc.perform(post("/accounts/" + id + "/status").param("status", "CLOSED").session(teller))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("error", "Account balance must be zero before it can be closed"));
            assertThat(account(id).getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void deletingAnUnusedAccountReturnsToItsCustomer() throws Exception {
            long customerId = createCustomer(uniqueEmail());
            long id = openAccount(customerId, "Spending");
            String number = account(id).getAccountNumber();
            mvc.perform(post("/accounts/" + id + "/delete").session(teller))
                    .andExpect(redirectedUrl("/customers/" + customerId))
                    .andExpect(flash().attribute("success", "Account " + number + " deleted"));
        }

        @Test
        void anAccountWithHistoryCannotBeDeleted() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            deposit(id, "1.00");
            mvc.perform(post("/accounts/" + id + "/delete").session(teller))
                    .andExpect(redirectedUrl("/accounts/" + id))
                    .andExpect(flash().attribute("error",
                            "Account has transaction history and cannot be deleted. Close it instead."));
        }

        @Test
        void theListFiltersByStatusAndType() throws Exception {
            long id = openAccount(createCustomer(uniqueEmail()), "Spending");
            mvc.perform(post("/accounts/" + id + "/status").param("status", "FROZEN").session(teller))
                    .andExpect(flash().attributeExists("success"));
            MvcResult result = mvc.perform(get("/accounts").param("status", "FROZEN").param("type", "CURRENT").session(teller))
                    .andExpect(view().name("accounts/list"))
                    .andExpect(model().attribute("status", AccountStatus.FROZEN))
                    .andReturn();
            List<Account> accounts = listOf(result, "accounts");
            assertThat(accounts).extracting(Account::getId).contains(id);
            assertThat(accounts).allSatisfy(a -> {
                assertThat(a.getStatus()).isEqualTo(AccountStatus.FROZEN);
                assertThat(a.getType()).isEqualTo(AccountType.CURRENT);
            });
        }
    }

    // --- helpers: arrange through the pages, read back through the pages ---

    private long createCustomer(String email) throws Exception {
        return idFrom(mvc.perform(customerForm(post("/customers"), "Grace", uniqueName(), email))
                .andExpect(redirectedUrlPattern("/customers/{id}"))
                .andReturn());
    }

    private long openAccount(long customerId, String nickname) throws Exception {
        return idFrom(mvc.perform(post("/accounts").param("customerId", String.valueOf(customerId))
                        .param("type", "CURRENT").param("nickname", nickname).session(teller))
                .andExpect(redirectedUrlPattern("/accounts/{id}"))
                .andReturn());
    }

    private void suspend(long customerId) throws Exception {
        mvc.perform(post("/customers/" + customerId + "/status").param("status", "SUSPENDED").session(teller))
                .andExpect(flash().attributeExists("success"));
    }

    private void deposit(long accountId, String amount) throws Exception {
        mvc.perform(move(accountId, "DEPOSIT", amount)).andExpect(flash().attributeExists("success"));
    }

    private static long idFrom(MvcResult redirect) {
        String location = redirect.getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private Customer customer(long id) throws Exception {
        return (Customer) mvc.perform(get("/customers/" + id).session(teller))
                .andExpect(view().name("customers/detail"))
                .andReturn().getModelAndView().getModel().get("customer");
    }

    private Account account(long id) throws Exception {
        return (Account) mvc.perform(get("/accounts/" + id).session(teller))
                .andReturn().getModelAndView().getModel().get("account");
    }

    private MockHttpServletRequestBuilder customerForm(
            MockHttpServletRequestBuilder request,
            String first, String last, String email) {
        return request.param("firstName", first).param("lastName", last).param("email", email).session(teller);
    }

    private MockHttpServletRequestBuilder move(long id, String type, String amount) {
        return post("/accounts/" + id + "/movements").param("type", type).param("amount", amount).session(teller);
    }

    private static String fieldError(MvcResult result, String form, String field) {
        BindingResult errors = (BindingResult) result.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + form);
        assertThat(errors.getFieldError(field)).as("an error on " + field).isNotNull();
        return errors.getFieldError(field).getDefaultMessage();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> listOf(MvcResult result, String name) {
        return (List<T>) result.getModelAndView().getModel().get(name);
    }

    /** Letters only, and all the same length, so no name is part of another (searches match substrings). */
    private static String uniqueName() {
        return "Page" + UUID.randomUUID().toString().replaceAll("[^a-f]", "").substring(0, 6);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.test";
    }
}
