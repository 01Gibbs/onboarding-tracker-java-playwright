package dev.upskill.ledger.customer;

import dev.upskill.ledger.common.BusinessRuleException;
import dev.upskill.ledger.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customers;
    private final Clock clock;

    public CustomerService(CustomerRepository customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Customer> search(String query, CustomerStatus status) {
        String q = StringUtils.hasText(query) ? query.trim() : null;
        return customers.search(q, status);
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    public Customer create(CustomerForm form) {
        String email = form.getEmail().trim();
        if (customers.existsByEmailIgnoreCase(email)) {
            throw BusinessRuleException.conflict("A customer with this email already exists", "email");
        }
        return customers.save(new Customer(form.getFirstName().trim(), form.getLastName().trim(), email,
                blankToNull(form.getPhone()), clock.instant()));
    }

    public Customer update(Long id, CustomerForm form) {
        Customer customer = get(id);
        String email = form.getEmail().trim();
        if (customers.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw BusinessRuleException.conflict("A customer with this email already exists", "email");
        }
        customer.setFirstName(form.getFirstName().trim());
        customer.setLastName(form.getLastName().trim());
        customer.setEmail(email);
        customer.setPhone(blankToNull(form.getPhone()));
        return customer;
    }

    public Customer changeStatus(Long id, CustomerStatus status) {
        Customer customer = get(id);
        customer.setStatus(status);
        return customer;
    }

    public void delete(Long id) {
        Customer customer = get(id);
        if (!customer.getAccounts().isEmpty()) {
            throw BusinessRuleException.conflict(
                    "Customer cannot be deleted while they have accounts", null);
        }
        customers.delete(customer);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
