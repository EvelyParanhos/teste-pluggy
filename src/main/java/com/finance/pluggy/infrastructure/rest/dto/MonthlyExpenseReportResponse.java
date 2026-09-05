package com.finance.pluggy.infrastructure.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyExpenseReportResponse {
    private String yearMonth;
    private String monthName;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netResult;
}
