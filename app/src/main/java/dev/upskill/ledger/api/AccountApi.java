package dev.upskill.ledger.api;

import dev.upskill.ledger.api.ApiDtos.AccountDto;
import dev.upskill.ledger.api.ApiDtos.AccountUpdateRequest;
import dev.upskill.ledger.api.ApiDtos.AmountRequest;
import dev.upskill.ledger.api.ApiDtos.EntryDto;
import dev.upskill.ledger.api.ApiDtos.TransferRequest;
import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountForm;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.MoneyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountApi {

    private final AccountService accounts;
    private final MoneyService money;

    public AccountApi(AccountService accounts, MoneyService money) {
        this.accounts = accounts;
        this.money = money;
    }

    @GetMapping("/api/accounts")
    public List<AccountDto> list(@RequestParam(required = false) AccountStatus status,
                                 @RequestParam(required = false) AccountType type,
                                 @RequestParam(required = false) Long customerId) {
        return accounts.search(status, type, customerId).stream().map(AccountDto::of).toList();
    }

    @GetMapping("/api/accounts/{id}")
    public AccountDto get(@PathVariable Long id) {
        return AccountDto.of(accounts.get(id));
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountDto> open(@Valid @RequestBody AccountForm form) {
        AccountDto created = AccountDto.of(accounts.open(form));
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @PatchMapping("/api/accounts/{id}")
    public AccountDto update(@PathVariable Long id, @RequestBody AccountUpdateRequest request) {
        Account account = accounts.get(id);
        if (request.nickname() != null) {
            account = accounts.rename(id, request.nickname());
        }
        if (request.status() != null && request.status() != account.getStatus()) {
            account = accounts.changeStatus(id, request.status());
        }
        return AccountDto.of(account);
    }

    @DeleteMapping("/api/accounts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accounts.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/accounts/{id}/transactions")
    public List<EntryDto> history(@PathVariable Long id, @RequestParam(required = false) EntryType type) {
        return money.history(id, type).stream().map(EntryDto::of).toList();
    }

    @PostMapping("/api/accounts/{id}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto deposit(@PathVariable Long id, @RequestBody AmountRequest request) {
        return EntryDto.of(money.deposit(id, request.amount(), request.description()));
    }

    @PostMapping("/api/accounts/{id}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto withdraw(@PathVariable Long id, @RequestBody AmountRequest request) {
        return EntryDto.of(money.withdraw(id, request.amount(), request.description()));
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto transfer(@RequestBody TransferRequest request) {
        if (request.fromAccountId() == null || request.toAccountId() == null) {
            throw new BusinessRuleException("fromAccountId and toAccountId are required");
        }
        money.transfer(request.fromAccountId(), request.toAccountId(), request.amount(), request.description());
        return AccountDto.of(accounts.get(request.fromAccountId()));
    }

    @GetMapping("/api/activity")
    public List<EntryDto> recentActivity(@RequestParam(defaultValue = "8") int limit) {
        return money.recent(limit).stream().map(EntryDto::of).toList();
    }
}
