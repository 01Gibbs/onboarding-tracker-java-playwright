package dev.upskill.ledger.ledger;

import dev.upskill.ledger.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** One line in an account's history. A transfer produces two entries sharing a {@code transferId}. */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 80)
    private String description;

    @Column(length = 8)
    private String counterpartyAccountNumber;

    @Column(length = 36)
    private String transferId;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    LedgerEntry(Account account, EntryType type, BigDecimal amount, String description,
                String counterpartyAccountNumber, String transferId, Instant createdAt) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = account.getBalance();
        this.description = description;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.transferId = transferId;
        this.createdAt = createdAt;
    }

    /** Amount with its sign: positive for money in, negative for money out. */
    public BigDecimal getSignedAmount() {
        return type.isCredit() ? amount : amount.negate();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public EntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public String getTransferId() {
        return transferId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
