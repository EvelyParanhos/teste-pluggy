package com.finance.pluggy.infrastructure.rest.dto;

import com.finance.pluggy.domain.model.InternalCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBudgetRequest {

    @NotNull(message = "A categoria interna é obrigatória")
    private InternalCategory category;

    @NotNull(message = "O limite mensal é obrigatório")
    @Positive(message = "O limite mensal deve ser um valor positivo")
    private BigDecimal monthlyLimit;

    private BigDecimal alertThresholdPercentage;
}
