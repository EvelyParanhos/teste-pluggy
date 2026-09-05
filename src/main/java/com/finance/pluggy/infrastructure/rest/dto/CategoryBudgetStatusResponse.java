package com.finance.pluggy.infrastructure.rest.dto;

import com.finance.pluggy.domain.model.InternalCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBudgetStatusResponse {
    private Long id;
    private InternalCategory category;
    private String categoryDescription;
    private BigDecimal monthlyLimit;
    private BigDecimal currentSpent;
    private BigDecimal percentageUsed;
    private String status;
}
