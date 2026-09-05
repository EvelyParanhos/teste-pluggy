package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.CategoryBudget;
import com.finance.pluggy.domain.model.InternalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, Long> {
    Optional<CategoryBudget> findByCategory(InternalCategory category);
}
