package dev.upskill.ledger.web;

import dev.upskill.ledger.account.Account;
import dev.upskill.ledger.account.AccountForm;
import dev.upskill.ledger.account.AccountService;
import dev.upskill.ledger.account.AccountStatus;
import dev.upskill.ledger.account.AccountType;
import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.customer.CustomerStatus;
import dev.upskill.ledger.ledger.EntryType;
import dev.upskill.ledger.ledger.MoneyService;
import dev.upskill.ledger.ledger.MovementForm;
import dev.upskill.ledger.ledger.MovementType;
import dev.upskill.ledger.ledger.Movements;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accounts;
    private final CustomerService customers;
    private final MoneyService money;
    private final Movements movements;

    public AccountController(AccountService accounts, CustomerService customers, MoneyService money,
                             Movements movements) {
        this.accounts = accounts;
        this.customers = customers;
        this.money = money;
        this.movements = movements;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AccountStatus status,
                       @RequestParam(required = false) AccountType type, Model model) {
        model.addAttribute("accounts", accounts.search(status, type, null));
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("statuses", AccountStatus.values());
        model.addAttribute("types", AccountType.values());
        return "accounts/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long customerId, Model model) {
        AccountForm form = new AccountForm();
        form.setCustomerId(customerId);
        model.addAttribute("form", form);
        return openForm(model);
    }

    @PostMapping
    public String open(@Valid @ModelAttribute("form") AccountForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return openForm(model);
        }
        try {
            Account account = accounts.open(form);
            redirect.addFlashAttribute("success", "Account " + account.getAccountNumber() + " opened");
            return "redirect:/accounts/" + account.getId();
        } catch (BusinessRuleException e) {
            Forms.reject(result, e);
            return openForm(model);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) EntryType entryType,
                         Model model) {
        if (!model.containsAttribute("movement")) {
            MovementForm movement = new MovementForm();
            movement.setType(MovementType.DEPOSIT);
            model.addAttribute("movement", movement);
        }
        return detailPage(id, entryType, model);
    }

    @PostMapping("/{id}/movements")
    public String move(@PathVariable Long id, @Valid @ModelAttribute("movement") MovementForm movement,
                       BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return detailPage(id, null, model);
        }
        try {
            redirect.addFlashAttribute("success", movements.move(id, movement).message());
            return "redirect:/accounts/" + id;
        } catch (BusinessRuleException e) {
            Forms.reject(result, e);
            return detailPage(id, null, model);
        }
    }

    @PostMapping("/{id}/nickname")
    public String rename(@PathVariable Long id, @RequestParam String nickname, RedirectAttributes redirect) {
        try {
            accounts.rename(id, nickname);
            redirect.addFlashAttribute("success", "Nickname updated");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam AccountStatus status,
                               RedirectAttributes redirect) {
        try {
            Account account = accounts.changeStatus(id, status);
            redirect.addFlashAttribute("success",
                    "Account " + account.getAccountNumber() + " is now " + status.getLabel().toLowerCase());
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        Account account = accounts.get(id);
        Long customerId = account.getCustomer().getId();
        String number = account.getAccountNumber();
        try {
            accounts.delete(id);
            redirect.addFlashAttribute("success", "Account " + number + " deleted");
            return "redirect:/customers/" + customerId;
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/" + id;
        }
    }

    private String openForm(Model model) {
        model.addAttribute("customers", customers.search(null, CustomerStatus.ACTIVE));
        model.addAttribute("types", AccountType.values());
        return "accounts/form";
    }

    private String detailPage(Long id, EntryType entryType, Model model) {
        Account account = accounts.get(id);
        model.addAttribute("account", account);
        model.addAttribute("entries", money.history(id, entryType));
        model.addAttribute("entryType", entryType);
        model.addAttribute("entryTypes", EntryType.values());
        model.addAttribute("movementTypes", MovementType.values());
        model.addAttribute("transferTargets", accounts.transferTargets(id));
        model.addAttribute("canDelete", accounts.canDelete(account));
        return "accounts/detail";
    }
}
