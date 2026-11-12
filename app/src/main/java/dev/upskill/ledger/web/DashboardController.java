package dev.upskill.ledger.web;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.customer.CustomerService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final CustomerService customers;
    private final AccountService accounts;

    public DashboardController(CustomerService customers, AccountService accounts) {
        this.customers = customers;
        this.accounts = accounts;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Account> all = accounts.search(null, null, null);
        model.addAttribute("customerCount", customers.search(null, null).size());
        model.addAttribute("activeAccountCount", all.stream().filter(Account::isActive).count());
        model.addAttribute("totalBalance", all.stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "dashboard";
    }
}
