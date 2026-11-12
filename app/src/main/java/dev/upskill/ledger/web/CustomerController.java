package dev.upskill.ledger.web;

import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.customer.Customer;
import dev.upskill.ledger.customer.CustomerForm;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.customer.CustomerStatus;
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
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) CustomerStatus status, Model model) {
        model.addAttribute("customers", customers.search(q, status));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", CustomerStatus.values());
        return "customers/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CustomerForm());
        return "customers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CustomerForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "customers/form";
        }
        try {
            Customer customer = customers.create(form);
            redirect.addFlashAttribute("success", "Customer " + customer.getFullName() + " created");
            return "redirect:/customers/" + customer.getId();
        } catch (BusinessRuleException e) {
            Forms.reject(result, e);
            return "customers/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customers.get(id));
        return "customers/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer = customers.get(id);
        model.addAttribute("customer", customer);
        model.addAttribute("form", CustomerForm.from(customer));
        return "customers/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CustomerForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        model.addAttribute("customer", customers.get(id));
        if (result.hasErrors()) {
            return "customers/form";
        }
        try {
            Customer customer = customers.update(id, form);
            redirect.addFlashAttribute("success", "Customer " + customer.getFullName() + " updated");
            return "redirect:/customers/" + id;
        } catch (BusinessRuleException e) {
            Forms.reject(result, e);
            return "customers/form";
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam CustomerStatus status,
                               RedirectAttributes redirect) {
        Customer customer = customers.changeStatus(id, status);
        redirect.addFlashAttribute("success",
                "Customer " + customer.getFullName() + " is now " + status.getLabel().toLowerCase());
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        String name = customers.get(id).getFullName();
        try {
            customers.delete(id);
            redirect.addFlashAttribute("success", "Customer " + name + " deleted");
            return "redirect:/customers";
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/customers/" + id;
        }
    }
}
