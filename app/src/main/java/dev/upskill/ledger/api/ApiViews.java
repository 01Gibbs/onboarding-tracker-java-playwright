package dev.upskill.ledger.api;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.api.ApiDtos.AccountDto;
import dev.upskill.ledger.api.ApiDtos.CustomerDto;
import dev.upskill.ledger.api.ApiDtos.DashboardDto;
import dev.upskill.ledger.api.ApiDtos.EntryDto;
import dev.upskill.ledger.api.ApiDtos.ReceiptDto;
import dev.upskill.ledger.common.Dates;
import dev.upskill.ledger.common.Money;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.ledger.LedgerEntry;
import dev.upskill.ledger.ledger.Movements.Receipt;
import org.springframework.stereotype.Component;

/**
 * Turns entities into the JSON the API returns, formatted with the same {@link Money} and {@link Dates} the
 * Thymeleaf pages use, so every front end shows identical text.
 */
@Component
class ApiViews {

    private final AccountService accounts;
    private final Money money;
    private final Dates dates;

    ApiViews(AccountService accounts, Money money, Dates dates) {
        this.accounts = accounts;
        this.money = money;
        this.dates = dates;
    }

    CustomerDto customer(Customer c) {
        return new CustomerDto(c.getId(), c.getReference(), c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getPhone(), c.getStatus(), c.getAccounts().size(), c.getCreatedAt(), c.getFullName(),
                c.getStatus().getLabel(), dates.date(c.getCreatedAt()));
    }

    AccountDto account(Account a) {
        return new AccountDto(a.getId(), a.getAccountNumber(), a.getCustomer().getId(),
                a.getCustomer().getFullName(), a.getType(), a.getNickname(), a.getBalance(), a.getStatus(),
                a.getOpenedAt(), a.getDisplayName(), money.format(a.getBalance()), a.getType().getLabel(),
                a.getStatus().getLabel(), accounts.canDelete(a));
    }

    EntryDto entry(LedgerEntry e) {
        Account a = e.getAccount();
        return new EntryDto(e.getId(), a.getId(), a.getAccountNumber(), a.getNickname(), e.getType(),
                e.getAmount(), e.getBalanceAfter(), e.getDescription(), e.getCounterpartyAccountNumber(),
                e.getTransferId(), e.getCreatedAt(), e.getType().getLabel(), money.formatChange(e.getSignedAmount()),
                money.format(e.getBalanceAfter()), dates.dateTime(e.getCreatedAt()));
    }

    DashboardDto dashboard(AccountService.Summary s) {
        return new DashboardDto(s.customerCount(), s.activeAccountCount(), s.totalBalance(),
                money.format(s.totalBalance()));
    }

    ReceiptDto receipt(Receipt r) {
        return new ReceiptDto(r.message(), account(r.account()));
    }
}
