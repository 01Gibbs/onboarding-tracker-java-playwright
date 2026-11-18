package dev.upskill.ledger.api;

import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.EntryType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JSON shapes returned by the API. Entities are never serialised directly. The {@code *Display}, {@code *Label}
 * and {@code displayName}/{@code fullName} fields are the exact text the pages show, so a front end never formats.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record CustomerDto(Long id, String reference, String firstName, String lastName, String email,
                              String phone, CustomerStatus status, int accountCount, Instant createdAt,
                              String fullName, String statusLabel, String createdAtDisplay) {
    }

    public record AccountDto(Long id, String accountNumber, Long customerId, String customerName,
                             AccountType type, String nickname, BigDecimal balance, AccountStatus status,
                             Instant openedAt, String displayName, String balanceDisplay, String typeLabel,
                             String statusLabel, boolean canDelete) {
    }

    public record EntryDto(Long id, Long accountId, String accountNumber, String accountNickname, EntryType type,
                           BigDecimal amount, BigDecimal balanceAfter, String description,
                           String counterpartyAccountNumber, String transferId, Instant createdAt,
                           String typeLabel, String amountDisplay, String balanceAfterDisplay,
                           String createdAtDisplay) {
    }

    public record DashboardDto(int customerCount, long activeAccountCount, BigDecimal totalBalance,
                               String totalBalanceDisplay) {
    }

    public record ReceiptDto(String message, AccountDto account) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, String username, String displayName, String role) {
    }

    /** Who is signed in, with what the pages show and whether edit controls appear. */
    public record SessionUserDto(String username, String displayName, String role, String roleLabel,
                                 boolean canEdit) {
    }

    public record AmountRequest(BigDecimal amount, String description) {
    }

    public record TransferRequest(@NotNull(message = "fromAccountId is required") Long fromAccountId,
                                  @NotNull(message = "toAccountId is required") Long toAccountId,
                                  BigDecimal amount, String description) {
    }

    public record AccountUpdateRequest(String nickname, AccountStatus status) {
    }

    public record StatusRequest(@NotNull(message = "Status is required") CustomerStatus status) {
    }
}
