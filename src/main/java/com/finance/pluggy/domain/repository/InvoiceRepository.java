package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByPluggyBillId(String pluggyBillId);

    List<Invoice> findByAccountId(Long accountId);

    List<Invoice> findByAccountIdOrderByDueDateDesc(Long accountId);

    List<Invoice> findByAccountIdOrderByDueDateAsc(Long accountId);
}
