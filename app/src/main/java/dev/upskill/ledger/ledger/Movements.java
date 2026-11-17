package dev.upskill.ledger.ledger;

import dev.upskill.ledger.LedgerlineProperties;
import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.common.Money;
import org.springframework.stereotype.Service;

/**
 * The "Move money" form, for every front end: applies a deposit, withdrawal or transfer after the
 * deliberately slow downstream delay, and returns the confirmation the user sees.
 */
@Service
public class Movements {

    private final MoneyService money;
    private final AccountService accounts;
    private final Money moneyFormat;
    private final LedgerlineProperties properties;

    public Movements(MoneyService money, AccountService accounts, Money moneyFormat,
                     LedgerlineProperties properties) {
        this.money = money;
        this.accounts = accounts;
        this.moneyFormat = moneyFormat;
        this.properties = properties;
    }

    public record Receipt(String message, Account account) {
    }

    public Receipt move(Long accountId, MovementForm movement) {
        simulateSlowDownstream();
        accounts.get(accountId);
        if (movement.getType() == null) {
            throw new BusinessRuleException("Choose a type", "type");
        }
        String amount = moneyFormat.format(Amount.of(movement.getAmount()).value());
        String message = switch (movement.getType()) {
            case DEPOSIT -> {
                money.deposit(accountId, movement.getAmount(), movement.getDescription());
                yield "Deposit of " + amount + " completed";
            }
            case WITHDRAWAL -> {
                money.withdraw(accountId, movement.getAmount(), movement.getDescription());
                yield "Withdrawal of " + amount + " completed";
            }
            case TRANSFER -> {
                Account target = money.transfer(accountId, movement.getToAccountId(), movement.getAmount(),
                        movement.getDescription());
                yield "Transfer of " + amount + " to " + target.getDisplayName() + " completed";
            }
        };
        return new Receipt(message, accounts.get(accountId));
    }

    // Outside any transaction, so the delay never holds a database connection.
    private void simulateSlowDownstream() {
        try {
            Thread.sleep(properties.processingDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while processing", e);
        }
    }
}
