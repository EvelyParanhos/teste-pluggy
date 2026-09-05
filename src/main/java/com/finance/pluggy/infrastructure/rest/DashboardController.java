package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.service.DashboardService;
import com.finance.pluggy.infrastructure.rest.dto.CategoryExpenseReportResponse;
import com.finance.pluggy.infrastructure.rest.dto.DashboardSummaryResponse;
import com.finance.pluggy.infrastructure.rest.dto.MonthlyExpenseReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retorna o resumo consolidado do saldo e métricas do mês atual.
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    /**
     * Retorna a visão segregada do Overview de Contas Bancárias, Cartões de Crédito e Investimentos.
     */
    @GetMapping("/account-overview")
    public ResponseEntity<com.finance.pluggy.infrastructure.rest.dto.AccountGroupSummaryResponse> getAccountOverview() {
        return ResponseEntity.ok(dashboardService.getAccountOverview());
    }

    /**
     * Retorna a distribuição e comparativo de despesas por categoria interna.
     */
    @GetMapping("/expenses-by-category")
    public ResponseEntity<List<CategoryExpenseReportResponse>> getExpensesByCategory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(dashboardService.getExpensesByCategory(year, month));
    }

    /**
     * Retorna o histórico mensal de receitas, despesas e saldo líquido.
     */
    @GetMapping("/monthly-history")
    public ResponseEntity<List<MonthlyExpenseReportResponse>> getMonthlyHistory(
            @RequestParam(required = false, defaultValue = "6") Integer monthsCount) {
        return ResponseEntity.ok(dashboardService.getMonthlyHistory(monthsCount));
    }
}
