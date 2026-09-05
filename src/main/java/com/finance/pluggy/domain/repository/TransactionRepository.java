package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByPluggyTransactionId(String pluggyTransactionId);
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByInternalCategory(InternalCategory internalCategory);
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.internalCategory = :category AND t.type = :type AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCategoryAndTypeAndDateBetween(
            @Param("category") InternalCategory category,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTypeAndDateBetween(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t.internalCategory as category, SUM(t.amount) as totalAmount, COUNT(t) as transactionCount " +
           "FROM Transaction t WHERE t.type = :type AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.internalCategory ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumAmountGroupedByCategoryAndDateBetween(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
