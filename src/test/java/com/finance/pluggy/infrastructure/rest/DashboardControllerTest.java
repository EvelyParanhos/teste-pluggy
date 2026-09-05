package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.service.DashboardService;
import com.finance.pluggy.infrastructure.rest.dto.AccountGroupSummaryResponse;
import com.finance.pluggy.infrastructure.rest.dto.CategoryExpenseReportResponse;
import com.finance.pluggy.infrastructure.rest.dto.DashboardSummaryResponse;
import com.finance.pluggy.infrastructure.rest.dto.MonthlyExpenseReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("Deve consultar o resumo do Dashboard via GET /api/v1/dashboard/summary")
    void shouldGetDashboardSummary() throws Exception {
        DashboardSummaryResponse summary = DashboardSummaryResponse.builder()
                .totalConsolidatedBalance(new BigDecimal("10000.00"))
                .totalBankBalance(new BigDecimal("10000.00"))
                .totalCreditCardBalance(new BigDecimal("2000.00"))
                .totalInvestmentBalance(new BigDecimal("15000.00"))
                .netWorth(new BigDecimal("23000.00"))
                .totalIncomeCurrentMonth(new BigDecimal("5000.00"))
                .totalExpensesCurrentMonth(new BigDecimal("2000.00"))
                .netSavingsCurrentMonth(new BigDecimal("3000.00"))
                .activeItemsCount(2)
                .activeAccountsCount(4)
                .build();

        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBankBalance").value(10000.00))
                .andExpect(jsonPath("$.totalCreditCardBalance").value(2000.00))
                .andExpect(jsonPath("$.netWorth").value(23000.00))
                .andExpect(jsonPath("$.activeAccountsCount").value(4));
    }

    @Test
    @DisplayName("Deve consultar a visão de overview via GET /api/v1/dashboard/account-overview")
    void shouldGetAccountOverview() throws Exception {
        AccountGroupSummaryResponse response = AccountGroupSummaryResponse.builder()
                .bankAccountsGroup(AccountGroupSummaryResponse.BankAccountsGroup.builder()
                        .totalBalance(new BigDecimal("1802.02"))
                        .build())
                .creditCardsGroup(AccountGroupSummaryResponse.CreditCardsGroup.builder()
                        .totalSpent(new BigDecimal("3032.00"))
                        .build())
                .investmentsGroup(AccountGroupSummaryResponse.InvestmentsGroup.builder()
                        .totalBalance(new BigDecimal("28021.24"))
                        .build())
                .build();

        when(dashboardService.getAccountOverview()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/account-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankAccountsGroup.totalBalance").value(1802.02))
                .andExpect(jsonPath("$.creditCardsGroup.totalSpent").value(3032.00))
                .andExpect(jsonPath("$.investmentsGroup.totalBalance").value(28021.24));
    }

    @Test
    @DisplayName("Deve consultar despesas por categoria via GET /api/v1/dashboard/expenses-by-category")
    void shouldGetExpensesByCategory() throws Exception {
        CategoryExpenseReportResponse report = CategoryExpenseReportResponse.builder()
                .category(InternalCategory.ALIMENTACAO)
                .categoryDescription("Alimentação e Restaurantes")
                .totalAmount(new BigDecimal("750.00"))
                .percentageOfTotal(new BigDecimal("50.00"))
                .transactionCount(5)
                .monthlyLimit(new BigDecimal("1000.00"))
                .build();

        when(dashboardService.getExpensesByCategory(null, null)).thenReturn(List.of(report));

        mockMvc.perform(get("/api/v1/dashboard/expenses-by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("ALIMENTACAO"))
                .andExpect(jsonPath("$[0].totalAmount").value(750.00))
                .andExpect(jsonPath("$[0].percentageOfTotal").value(50.00));
    }

    @Test
    @DisplayName("Deve consultar histórico mensal via GET /api/v1/dashboard/monthly-history")
    void shouldGetMonthlyHistory() throws Exception {
        MonthlyExpenseReportResponse history = MonthlyExpenseReportResponse.builder()
                .yearMonth("2026-09")
                .monthName("Setembro 2026")
                .totalIncome(new BigDecimal("4000.00"))
                .totalExpenses(new BigDecimal("1500.00"))
                .netResult(new BigDecimal("2500.00"))
                .build();

        when(dashboardService.getMonthlyHistory(6)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/dashboard/monthly-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].yearMonth").value("2026-09"))
                .andExpect(jsonPath("$[0].netResult").value(2500.00));
    }
}
