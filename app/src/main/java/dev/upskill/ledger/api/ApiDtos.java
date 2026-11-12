package dev.upskill.ledger.api;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

/** JSON shapes returned by the API. Entities are never serialised directly. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record CustomerDto(Long id, String reference, String firstName, String lastName, String email,
                              String phone, CustomerStatus status, int accountCount, Instant createdAt) {
        static CustomerDto of(Customer c) {
            return new CustomerDto(c.getId(), c.getReference(), c.getFirstName(), c.getLastName(), c.getEmail(),
                    c.getPhone(), c.getStatus(), c.getAccounts().size(), c.getCreatedAt());
        }
    }

    public record AccountDto(Long id, String accountNumber, Long customerId, String customerName,
                             AccountType type, String nickname, BigDecimal balance, AccountStatus status,
                             Instant openedAt) {
        static AccountDto of(Account a) {
            return new AccountDto(a.getId(), a.getAccountNumber(), a.getCustomer().getId(),
                    a.getCustomer().getFullName(), a.getType(), a.getNickname(), a.getBalance(), a.getStatus(),
                    a.getOpenedAt());
        }
    }

    public record EntryDto(Long id, Long accountId, String accountNumber, String accountNickname, EntryType type,
                           BigDecimal amount, BigDecimal balanceAfter, String description,
                           String counterpartyAccountNumber, String transferId, Instant createdAt) {
        static EntryDto of(LedgerEntry e) {
            Account a = e.getAccount();
            return new EntryDto(e.getId(), a.getId(), a.getAccountNumber(), a.getNickname(), e.getType(),
                    e.getAmount(), e.getBalanceAfter(), e.getDescription(), e.getCounterpartyAccountNumber(),
                    e.getTransferId(), e.getCreatedAt());
        }
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, String username, String displayName, String role) {
    }

    public record AmountRequest(BigDecimal amount, String description) {
    }

    public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {
    }

    public record AccountUpdateRequest(String nickname, AccountStatus status) {
    }

    public record StatusRequest(CustomerStatus status) {
    }
}
