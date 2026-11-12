package dev.upskill.ledger.ledger;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.common.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MoneyService {

    private final AccountService accounts;
    private final LedgerEntryRepository entries;
    private final Money money;
    private final Clock clock;

    public MoneyService(AccountService accounts, LedgerEntryRepository entries, Money money, Clock clock) {
        this.accounts = accounts;
        this.entries = entries;
        this.money = money;
        this.clock = clock;
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

    public LedgerEntry deposit(Long accountId, BigDecimal amount, String description) {
        Account account = accounts.get(accountId);
        BigDecimal value = Amount.of(amount).value();
        requireActive(account, null);
        account.credit(value);
        return entries.save(new LedgerEntry(account, EntryType.DEPOSIT, value,
                describe(description, "Deposit"), null, null, clock.instant()));
    }

    public LedgerEntry withdraw(Long accountId, BigDecimal amount, String description) {
        Account account = accounts.get(accountId);
        BigDecimal value = Amount.of(amount).value();
        requireActive(account, null);
        requireFunds(account, value);
        account.debit(value);
        return entries.save(new LedgerEntry(account, EntryType.WITHDRAWAL, value,
                describe(description, "Withdrawal"), null, null, clock.instant()));
    }

    /** Moves money between two accounts atomically. Returns the destination account. */
    public Account transfer(Long fromId, Long toId, BigDecimal amount, String description) {
        if (toId == null) {
            throw new BusinessRuleException("Choose an account to transfer to", "toAccountId");
        }
        if (toId.equals(fromId)) {
            throw new BusinessRuleException("Cannot transfer to the same account", "toAccountId");
        }
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        BigDecimal value = Amount.of(amount).value();
        requireActive(from, null);
        requireActive(to, "toAccountId");
        requireFunds(from, value);

        String transferId = UUID.randomUUID().toString();
        String text = describe(description, "Transfer");
        from.debit(value);
        Instant now = clock.instant();
        entries.save(new LedgerEntry(from, EntryType.TRANSFER_OUT, value, text, to.getAccountNumber(), transferId, now));
        to.credit(value);
        entries.save(new LedgerEntry(to, EntryType.TRANSFER_IN, value, text, from.getAccountNumber(), transferId, now));
        return to;
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
