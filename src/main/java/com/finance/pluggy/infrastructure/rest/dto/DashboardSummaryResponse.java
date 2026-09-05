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
public class DashboardSummaryResponse {
    private BigDecimal totalConsolidatedBalance; // Saldo de contas bancárias de liquidez
    private BigDecimal totalBankBalance; // Contas Corrente / Poupança
    private BigDecimal totalCreditCardBalance; // Cartões de Crédito (Fatura)
    private BigDecimal totalInvestmentBalance; // Investimentos
    private BigDecimal netWorth; // Patrimônio Líquido
    private BigDecimal totalIncomeCurrentMonth;
    private BigDecimal totalExpensesCurrentMonth;
    private BigDecimal netSavingsCurrentMonth;
    private long activeItemsCount;
    private long activeAccountsCount;
}
