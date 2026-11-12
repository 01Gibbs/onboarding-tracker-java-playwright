package dev.upskill.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountForm;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerForm;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.ledger.MoneyService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Sanity checks for the core money rules, so the app under test behaves predictably. */
@SpringBootTest
class MoneyServiceTest {

    @Autowired
    CustomerService customers;
    @Autowired
    AccountService accounts;
    @Autowired
    MoneyService money;

    private Account newAccount() {
        CustomerForm customerForm = new CustomerForm();
        customerForm.setFirstName("Unit");
        customerForm.setLastName("Test");
        customerForm.setEmail(UUID.randomUUID() + "@example.test");
        Customer customer = customers.create(customerForm);
        AccountForm accountForm = new AccountForm();
        accountForm.setCustomerId(customer.getId());
        accountForm.setType(AccountType.CURRENT);
        accountForm.setNickname("Unit");
        return accounts.open(accountForm);
    }

    @Test
    void transferMovesMoneyAtomically() {
        Account from = newAccount();
        Account to = newAccount();
        money.deposit(from.getId(), new BigDecimal("100.00"), null);

        money.transfer(from.getId(), to.getId(), new BigDecimal("40.00"), "Test");

        assertThat(accounts.get(from.getId()).getBalance()).isEqualByComparingTo("60.00");
        assertThat(accounts.get(to.getId()).getBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void cannotOverdraw() {
        Account account = newAccount();
        money.deposit(account.getId(), new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> money.withdraw(account.getId(), new BigDecimal("10.01"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Insufficient funds. Available balance is £10.00");
        assertThat(accounts.get(account.getId()).getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void frozenAccountsCannotReceiveTransfers() {
        Account from = newAccount();
        Account to = newAccount();
        money.deposit(from.getId(), new BigDecimal("10.00"), null);
        accounts.changeStatus(to.getId(), AccountStatus.FROZEN);

        assertThatThrownBy(() -> money.transfer(from.getId(), to.getId(), BigDecimal.ONE, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(accounts.get(from.getId()).getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void closingRequiresZeroBalance() {
        Account account = newAccount();
        money.deposit(account.getId(), BigDecimal.ONE, null);

        assertThatThrownBy(() -> accounts.changeStatus(account.getId(), AccountStatus.CLOSED))
                .hasMessage("Account balance must be zero before it can be closed");
    }
}
