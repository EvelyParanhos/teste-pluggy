package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.BudgetAlertLog;
import com.finance.pluggy.domain.model.InternalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetAlertLogRepository extends JpaRepository<BudgetAlertLog, Long> {
    List<BudgetAlertLog> findByYearMonth(String yearMonth);
    Optional<BudgetAlertLog> findByCategoryAndYearMonth(InternalCategory category, String yearMonth);
}
