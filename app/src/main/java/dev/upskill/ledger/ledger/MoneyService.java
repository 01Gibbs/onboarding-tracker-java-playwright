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
    private final Faults faults;

    public MoneyService(AccountService accounts, LedgerEntryRepository entries, Money money, Clock clock,
                        Faults faults) {
        this.accounts = accounts;
        this.entries = entries;
        this.money = money;
        this.clock = clock;
        this.faults = faults;
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
        return entries.save(post(account, EntryType.DEPOSIT, value, describe(description, "Deposit"), null, null,
                clock.instant()));
    }

    public LedgerEntry withdraw(Long accountId, BigDecimal amount, String description) {
        Account account = accounts.get(accountId);
        BigDecimal value = Amount.of(amount).value();
        requireActive(account, null);
        requireFunds(account, value);
        LedgerEntry entry = post(account, EntryType.WITHDRAWAL, value, describe(description, "Withdrawal"), null, null,
                clock.instant());
        if (faults.isArmed(Fault.SKIP_WITHDRAWAL_ENTRY, account.getId())) {
            return entry; // planted fault (mission B0): the money moves but nothing is recorded
        }
        return entries.save(entry);
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
        Instant now = clock.instant();
        entries.save(post(from, EntryType.TRANSFER_OUT, value, text, to.getAccountNumber(), transferId, now));

        BigDecimal received = value;
        String receivedTransferId = transferId;
        if (faults.isArmed(Fault.TRANSFER_IN_SHORT, to.getId())) {
            received = value.subtract(new BigDecimal("0.01")); // planted fault (mission B0): a penny goes missing
        }
        if (faults.isArmed(Fault.UNPAIRED_TRANSFER, to.getId())) {
            receivedTransferId = UUID.randomUUID().toString(); // planted fault (mission B0): legs lose each other
        }
        entries.save(post(to, EntryType.TRANSFER_IN, received, text, from.getAccountNumber(), receivedTransferId, now));
        return to;
    }

    private void requireActive(Account account, String field) {
        if (!account.isActive()) {
            throw new BusinessRuleException("Account " + account.getAccountNumber() + " is "
                    + account.getStatus().getLabel().toLowerCase() + " and cannot be used", field);
        }
    }

    /** Moves the money on the account and returns the entry that records it (not yet saved). */
    private LedgerEntry post(Account account, EntryType type, BigDecimal value, String text,
                             String counterparty, String transferId, Instant now) {
        if (faults.isArmed(Fault.STALE_BALANCE_AFTER, account.getId())) {
            // planted fault (mission B0): the entry is written before the balance changes
            LedgerEntry stale = new LedgerEntry(account, type, value, text, counterparty, transferId, now);
            move(account, type, value);
            return stale;
        }
        move(account, type, value);
        return new LedgerEntry(account, type, value, text, counterparty, transferId, now);
    }

    private static void move(Account account, EntryType type, BigDecimal value) {
        if (type.isCredit()) {
            account.credit(value);
        } else {
            account.debit(value);
        }
    }

    private void requireFunds(Account account, BigDecimal amount) {
        if (faults.isArmed(Fault.OVERDRAFT, account.getId())) {
            return; // planted fault (mission B0): no funds check, so the balance can go negative
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds. Available balance is "
                    + money.format(account.getBalance()), "amount");
        }
    }

    private static String describe(String description, String fallback) {
        return StringUtils.hasText(description) ? description.trim() : fallback;
    }
}
