package dev.upskill.ledger.api;

import dev.upskill.ledger.api.ApiDtos.CustomerDto;
import dev.upskill.ledger.api.ApiDtos.StatusRequest;
import dev.upskill.ledger.customer.CustomerForm;
import dev.upskill.ledger.customer.CustomerService;
import dev.upskill.ledger.customer.CustomerStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerApi {

    private final CustomerService customers;

    public CustomerApi(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    public List<CustomerDto> list(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) CustomerStatus status) {
        return customers.search(q, status).stream().map(CustomerDto::of).toList();
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Long id) {
        return CustomerDto.of(customers.get(id));
    }

    @PostMapping
    public ResponseEntity<CustomerDto> create(@Valid @RequestBody CustomerForm form) {
        CustomerDto created = CustomerDto.of(customers.create(form));
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerForm form) {
        return CustomerDto.of(customers.update(id, form));
    }

    @PatchMapping("/{id}/status")
    public CustomerDto changeStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return CustomerDto.of(customers.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customers.delete(id);
        return ResponseEntity.noContent().build();
    }
}
