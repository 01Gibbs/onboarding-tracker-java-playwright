package dev.upskill.ledger.api;

import dev.upskill.ledger.api.ApiDtos.AccountDto;
import dev.upskill.ledger.api.ApiDtos.AccountUpdateRequest;
import dev.upskill.ledger.api.ApiDtos.AmountRequest;
import dev.upskill.ledger.api.ApiDtos.DashboardDto;
import dev.upskill.ledger.api.ApiDtos.EntryDto;
import dev.upskill.ledger.api.ApiDtos.ReceiptDto;
import dev.upskill.ledger.api.ApiDtos.TransferRequest;
import dev.upskill.ledger.account.AccountForm;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.MoneyService;
import dev.upskill.ledger.ledger.MovementForm;
import dev.upskill.ledger.ledger.Movements;
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
    private final Movements movements;
    private final ApiViews views;

    public AccountApi(AccountService accounts, MoneyService money, Movements movements, ApiViews views) {
        this.accounts = accounts;
        this.money = money;
        this.movements = movements;
        this.views = views;
    }

    @GetMapping("/api/accounts")
    public List<AccountDto> list(@RequestParam(required = false) AccountStatus status,
                                 @RequestParam(required = false) AccountType type,
                                 @RequestParam(required = false) Long customerId) {
        return accounts.search(status, type, customerId).stream().map(views::account).toList();
    }

    @GetMapping("/api/accounts/{id}")
    public AccountDto get(@PathVariable Long id) {
        return views.account(accounts.get(id));
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountDto> open(@Valid @RequestBody AccountForm form) {
        AccountDto created = views.account(accounts.open(form));
        return ResponseEntity.created(URI.create("/api/accounts/" + created.id())).body(created);
    }

    @PatchMapping("/api/accounts/{id}")
    public AccountDto update(@PathVariable Long id, @RequestBody AccountUpdateRequest request) {
        return views.account(accounts.update(id, request.nickname(), request.status()));
    }

    @DeleteMapping("/api/accounts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accounts.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/accounts/{id}/transfer-targets")
    public List<AccountDto> transferTargets(@PathVariable Long id) {
        accounts.get(id);
        return accounts.transferTargets(id).stream().map(views::account).toList();
    }

    @GetMapping("/api/accounts/{id}/transactions")
    public List<EntryDto> history(@PathVariable Long id, @RequestParam(required = false) EntryType type) {
        return money.history(id, type).stream().map(views::entry).toList();
    }

    @PostMapping("/api/accounts/{id}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto deposit(@PathVariable Long id, @RequestBody AmountRequest request) {
        return views.entry(money.deposit(id, request.amount(), request.description()));
    }

    @PostMapping("/api/accounts/{id}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto withdraw(@PathVariable Long id, @RequestBody AmountRequest request) {
        return views.entry(money.withdraw(id, request.amount(), request.description()));
    }

    /** The "Move money" form: slow on purpose, and returns the confirmation to show. */
    @PostMapping("/api/accounts/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptDto move(@PathVariable Long id, @RequestBody MovementForm movement) {
        return views.receipt(movements.move(id, movement));
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto transfer(@Valid @RequestBody TransferRequest request) {
        money.transfer(request.fromAccountId(), request.toAccountId(), request.amount(), request.description());
        return views.account(accounts.get(request.fromAccountId()));
    }

    @GetMapping("/api/dashboard")
    public DashboardDto dashboard() {
        return views.dashboard(accounts.summary());
    }

    @GetMapping("/api/activity")
    public List<EntryDto> recentActivity(@RequestParam(defaultValue = "8") int limit) {
        return money.recent(limit).stream().map(views::entry).toList();
    }
}
