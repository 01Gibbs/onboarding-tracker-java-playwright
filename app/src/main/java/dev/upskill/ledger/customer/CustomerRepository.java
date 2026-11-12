package dev.upskill.ledger.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select c from Customer c
            where (:status is null or c.status = :status)
              and (:q is null
                   or lower(concat(c.firstName, ' ', c.lastName)) like lower(concat('%', :q, '%'))
                   or lower(c.email) like lower(concat('%', :q, '%')))
            order by c.lastName, c.firstName
            """)
    List<Customer> search(@Param("q") String q, @Param("status") CustomerStatus status);
}
