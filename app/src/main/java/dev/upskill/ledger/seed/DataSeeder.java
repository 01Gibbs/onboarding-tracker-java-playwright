package dev.upskill.ledger.seed;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountForm;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerForm;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.MoneyService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads a known set of fictional data on start-up and on POST /api/testing/reset.
 * Identity columns are restarted so references (CUS-0001) and account numbers (10000001) are predictable.
 * Runs as an ApplicationRunner so seeding finishes before the app reports ready (see HealthApi).
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final CustomerService customers;
    private final AccountService accounts;
    private final MoneyService money;
    private final TransactionTemplate transaction;

    public DataSeeder(JdbcTemplate jdbc, EntityManager entityManager, CustomerService customers,
                      AccountService accounts, MoneyService money, TransactionTemplate transaction) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.customers = customers;
        this.accounts = accounts;
        this.money = money;
        this.transaction = transaction;
    }

    @Override
    public void run(ApplicationArguments args) {
        reset();
    }

    public synchronized SeedSummary reset() {
        return transaction.execute(status -> seed());
    }

    private SeedSummary seed() {
        entityManager.flush();
        entityManager.clear();
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : new String[] {"ledger_entries", "accounts", "customers"}) {
            jdbc.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Customer alice = customer("Alice", "Archer", "alice.archer@example.test", "+44 7700 900001");
        Customer ben = customer("Ben", "Carter", "ben.carter@example.test", "+44 7700 900002");
        Customer chloe = customer("Chloe", "Davies", "chloe.davies@example.test", null);
        Customer dan = customer("Dan", "Evans", "dan.evans@example.test", "+44 7700 900004");
        customer("Erin", "Foster", "erin.foster@example.test", null);

        Account everyday = account(alice, AccountType.CURRENT, "Everyday");          // 10000001
        Account rainyDay = account(alice, AccountType.SAVINGS, "Rainy Day");         // 10000002
        Account main = account(ben, AccountType.CURRENT, "Main");                    // 10000003
        Account holiday = account(ben, AccountType.SAVINGS, "Holiday Fund");         // 10000004
        account(chloe, AccountType.CURRENT, "Bills");                                // 10000005
        Account old = account(dan, AccountType.CURRENT, "Old Account");              // 10000006

        money.deposit(everyday.getId(), amount("1500.00"), "Salary");
        money.withdraw(everyday.getId(), amount("250.00"), "Rent share");
        money.deposit(rainyDay.getId(), amount("4000.00"), "Opening balance");
        money.transfer(everyday.getId(), rainyDay.getId(), amount("1000.00"), "Monthly saving");
        money.deposit(everyday.getId(), amount("1000.00"), "Freelance invoice");
        money.deposit(main.getId(), amount("400.00"), "Salary");
        money.withdraw(main.getId(), amount("79.50"), "Groceries");
        money.deposit(holiday.getId(), amount("800.00"), "Opening balance");
        money.deposit(old.getId(), amount("50.00"), "Opening balance");
        money.withdraw(old.getId(), amount("50.00"), "Closing withdrawal");

        accounts.changeStatus(holiday.getId(), AccountStatus.FROZEN);
        accounts.changeStatus(old.getId(), AccountStatus.CLOSED);
        customers.changeStatus(dan.getId(), CustomerStatus.SUSPENDED);

        return new SeedSummary(5, 6);
    }

    public record SeedSummary(int customers, int accounts) {
    }

    private Customer customer(String first, String last, String email, String phone) {
        CustomerForm form = new CustomerForm();
        form.setFirstName(first);
        form.setLastName(last);
        form.setEmail(email);
        form.setPhone(phone);
        return customers.create(form);
    }

    private Account account(Customer owner, AccountType type, String nickname) {
        AccountForm form = new AccountForm();
        form.setCustomerId(owner.getId());
        form.setType(type);
        form.setNickname(nickname);
        return accounts.open(form);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
