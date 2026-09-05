package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.model.BudgetAlertLog;
import com.finance.pluggy.domain.model.CategoryBudget;
import com.finance.pluggy.domain.model.TransactionType;
import com.finance.pluggy.domain.repository.BudgetAlertLogRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.rest.dto.CategoryBudgetRequest;
import com.finance.pluggy.infrastructure.rest.dto.CategoryBudgetStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final CategoryBudgetRepository categoryBudgetRepository;
    private final BudgetAlertLogRepository alertLogRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Cadastra ou atualiza o limite de orçamento de uma categoria.
     */
    @PostMapping
    public ResponseEntity<CategoryBudget> saveBudget(@Valid @RequestBody CategoryBudgetRequest request) {
        CategoryBudget budget = categoryBudgetRepository.findByCategory(request.getCategory())
                .orElseGet(() -> CategoryBudget.builder().category(request.getCategory()).build());

        budget.setMonthlyLimit(request.getMonthlyLimit());
        if (request.getAlertThresholdPercentage() != null) {
            budget.setAlertThresholdPercentage(request.getAlertThresholdPercentage());
        }

        return ResponseEntity.ok(categoryBudgetRepository.save(budget));
    }

    /**
     * Lista todos os orçamentos cadastrados com os gastos acumulados do mês corrente e percentual de uso.
     */
    @GetMapping
    public ResponseEntity<List<CategoryBudgetStatusResponse>> getBudgetsStatus() {
        List<CategoryBudget> budgets = categoryBudgetRepository.findAll();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        List<CategoryBudgetStatusResponse> response = new ArrayList<>();

        for (CategoryBudget budget : budgets) {
            BigDecimal totalSpent = transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(
                    budget.getCategory(), TransactionType.DEBIT, startDate, endDate);

            if (totalSpent == null) {
                totalSpent = BigDecimal.ZERO;
            }

            BigDecimal percentageUsed = BigDecimal.ZERO;
            if (budget.getMonthlyLimit() != null && budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = totalSpent.multiply(new BigDecimal("100"))
                        .divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);
            }

            String status = "NORMAL";
            BigDecimal threshold = budget.getAlertThresholdPercentage() != null
                    ? budget.getAlertThresholdPercentage()
                    : new BigDecimal("80.00");

            if (totalSpent.compareTo(budget.getMonthlyLimit()) >= 0) {
                status = "EXCEEDED";
            } else if (percentageUsed.compareTo(threshold) >= 0) {
                status = "WARN";
            }

            response.add(CategoryBudgetStatusResponse.builder()
                    .id(budget.getId())
                    .category(budget.getCategory())
                    .categoryDescription(budget.getCategory().getDescription())
                    .monthlyLimit(budget.getMonthlyLimit())
                    .currentSpent(totalSpent)
                    .percentageUsed(percentageUsed)
                    .status(status)
                    .build());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Lista os alertas de orçamento gerados no mês corrente.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<BudgetAlertLog>> getAlerts() {
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return ResponseEntity.ok(alertLogRepository.findByYearMonth(currentYearMonth));
    }
}
