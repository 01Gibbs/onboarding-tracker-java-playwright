package dev.upskill.ledger.account;

import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.common.NotFoundException;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.LedgerEntryRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;
    private final CustomerService customers;
    private final Clock clock;

    public AccountService(AccountRepository accounts, LedgerEntryRepository entries, CustomerService customers,
                          Clock clock) {
        this.accounts = accounts;
        this.entries = entries;
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Account> search(AccountStatus status, AccountType type, Long customerId) {
        return accounts.search(status, type, customerId);
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
    }

    /** Other active accounts that money can be transferred to from {@code accountId}. */
    @Transactional(readOnly = true)
    public List<Account> transferTargets(Long accountId) {
        return accounts.findByStatusAndIdNotOrderByAccountNumber(AccountStatus.ACTIVE, accountId);
    }

    @Transactional(readOnly = true)
    public boolean hasHistory(Long accountId) {
        return entries.existsByAccountId(accountId);
    }

    public Account open(AccountForm form) {
        Customer customer = customers.get(form.getCustomerId());
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Accounts cannot be opened for a suspended customer", "customerId");
        }
        Account account = accounts.save(
                new Account(customer, form.getType(), form.getNickname().trim(), clock.instant()));
        account.assignAccountNumber();
        customer.getAccounts().add(account);
        return account;
    }

    public Account rename(Long id, String nickname) {
        Account account = get(id);
        if (nickname == null || nickname.isBlank() || nickname.trim().length() > 40) {
            throw new BusinessRuleException("Nickname must be between 1 and 40 characters", "nickname");
        }
        account.setNickname(nickname.trim());
        return account;
    }

    public Account changeStatus(Long id, AccountStatus target) {
        Account account = get(id);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw BusinessRuleException.conflict("Closed accounts cannot be changed", "status");
        }
        if (target == AccountStatus.CLOSED && account.getBalance().signum() != 0) {
            throw BusinessRuleException.conflict(
                    "Account balance must be zero before it can be closed", "status");
        }
        account.setStatus(target);
        return account;
    }

    public void delete(Long id) {
        Account account = get(id);
        if (entries.existsByAccountId(id) || account.getBalance().signum() != 0) {
            throw BusinessRuleException.conflict(
                    "Account has transaction history and cannot be deleted. Close it instead.", null);
        }
        account.getCustomer().getAccounts().remove(account);
        accounts.delete(account);
    }
}
