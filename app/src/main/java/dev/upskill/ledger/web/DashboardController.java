package dev.upskill.ledger.web;

import dev.upskill.ledger.account.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AccountService accounts;

    public DashboardController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("summary", accounts.summary());
        return "dashboard";
    }
}
