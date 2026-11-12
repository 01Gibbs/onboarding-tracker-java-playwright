package dev.upskill.ledger.account;

import dev.upskill.ledger.customer.Customer;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    /** Account numbers are derived from the id so they are predictable after a data reset. */
    private static final long NUMBER_BASE = 10_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 8)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 40)
    private String nickname;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private Instant openedAt;

    @Version
    private long version;

    protected Account() {
    }

    public Account(Customer customer, AccountType type, String nickname, Instant openedAt) {
        this.customer = customer;
        this.type = type;
        this.nickname = nickname;
        this.openedAt = openedAt;
    }

    void assignAccountNumber() {
        this.accountNumber = String.valueOf(NUMBER_BASE + id);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /** "Everyday (10000001)" - used in dropdowns and activity feeds. */
    public String getDisplayName() {
        return nickname + " (" + accountNumber + ")";
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public AccountType getType() {
        return type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
