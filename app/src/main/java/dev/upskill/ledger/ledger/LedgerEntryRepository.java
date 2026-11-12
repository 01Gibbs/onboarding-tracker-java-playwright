package dev.upskill.ledger.ledger;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByAccountId(Long accountId);

    @Query("""
            select e from LedgerEntry e
            where e.account.id = :accountId and (:type is null or e.type = :type)
            order by e.id desc
            """)
    List<LedgerEntry> history(@Param("accountId") Long accountId, @Param("type") EntryType type);

    @Query("select e from LedgerEntry e join fetch e.account order by e.id desc")
    List<LedgerEntry> recent(Limit limit);
}
