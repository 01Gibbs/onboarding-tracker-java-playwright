package dev.upskill.ledger.account;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("""
            select a from Account a join fetch a.customer c
            where (:status is null or a.status = :status)
              and (:type is null or a.type = :type)
              and (:customerId is null or c.id = :customerId)
            order by a.accountNumber
            """)
    List<Account> search(@Param("status") AccountStatus status,
                         @Param("type") AccountType type,
                         @Param("customerId") Long customerId);

    List<Account> findByStatusAndIdNotOrderByAccountNumber(AccountStatus status, Long id);
}
