package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.CategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryMappingRepository extends JpaRepository<CategoryMapping, Long> {
    Optional<CategoryMapping> findByPluggyCategoryIgnoreCase(String pluggyCategory);
}
