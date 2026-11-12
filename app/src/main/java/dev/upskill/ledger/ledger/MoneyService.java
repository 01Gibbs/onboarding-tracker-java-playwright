package dev.upskill.ledger.ledger;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.common.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MoneyService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    private final AccountService accounts;
    private final LedgerEntryRepository entries;
    private final Money money;

    public MoneyService(AccountService accounts, LedgerEntryRepository entries, Money money) {
        this.accounts = accounts;
        this.entries = entries;
        this.money = money;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> history(Long accountId, EntryType type) {
        accounts.get(accountId);
        return entries.history(accountId, type);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> recent(int limit) {
        return entries.recent(Limit.of(Math.clamp(limit, 1, 50)));
    }

    /** Applies a validated movement form; returns a confirmation message for the user. */
    public String apply(Long accountId, MovementForm form) {
        BigDecimal amount = form.getAmount();
        return switch (form.getType()) {
            case DEPOSIT -> {
                deposit(accountId, amount, form.getDescription());
                yield "Deposit of " + money.format(amount) + " completed";
            }
            case WITHDRAWAL -> {
                withdraw(accountId, amount, form.getDescription());
                yield "Withdrawal of " + money.format(amount) + " completed";
            }
            case TRANSFER -> {
                if (form.getToAccountId() == null) {
                    throw new BusinessRuleException("Choose an account to transfer to", "toAccountId");
                }
                Account target = transfer(accountId, form.getToAccountId(), amount, form.getDescription());
                yield "Transfer of " + money.format(amount) + " to " + target.getDisplayName() + " completed";
            }
        };
    }

    public LedgerEntry deposit(Long accountId, BigDecimal amount, String description) {
        Account account = accounts.get(accountId);
        validate(amount);
        requireActive(account, null);
        account.credit(amount);
        return entries.save(new LedgerEntry(account, EntryType.DEPOSIT, amount,
                describe(description, "Deposit"), null, null));
    }

    public LedgerEntry withdraw(Long accountId, BigDecimal amount, String description) {
        Account account = accounts.get(accountId);
        validate(amount);
        requireActive(account, null);
        requireFunds(account, amount);
        account.debit(amount);
        return entries.save(new LedgerEntry(account, EntryType.WITHDRAWAL, amount,
                describe(description, "Withdrawal"), null, null));
    }

    /** Moves money between two accounts atomically. Returns the destination account. */
    public Account transfer(Long fromId, Long toId, BigDecimal amount, String description) {
        if (fromId.equals(toId)) {
            throw new BusinessRuleException("Cannot transfer to the same account", "toAccountId");
        }
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        validate(amount);
        requireActive(from, null);
        requireActive(to, "toAccountId");
        requireFunds(from, amount);

        String transferId = UUID.randomUUID().toString();
        String text = describe(description, "Transfer");
        from.debit(amount);
        entries.save(new LedgerEntry(from, EntryType.TRANSFER_OUT, amount, text, to.getAccountNumber(), transferId));
        to.credit(amount);
        entries.save(new LedgerEntry(to, EntryType.TRANSFER_IN, amount, text, from.getAccountNumber(), transferId));
        return to;
    }

    private void validate(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero", "amount");
        }
        if (amount.scale() > 2) {
            throw new BusinessRuleException("Amount can have at most 2 decimal places", "amount");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("Amount must not exceed 1,000,000.00", "amount");
        }
    }

    private void requireActive(Account account, String field) {
        if (!account.isActive()) {
            throw new BusinessRuleException("Account " + account.getAccountNumber() + " is "
                    + account.getStatus().getLabel().toLowerCase() + " and cannot be used", field);
        }
    }

    private void requireFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds. Available balance is "
                    + money.format(account.getBalance()), "amount");
        }
    }

    private static String describe(String description, String fallback) {
        return StringUtils.hasText(description) ? description.trim() : fallback;
    }
}
