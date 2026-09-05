package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.BudgetAlertLogRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetRuleService {

    private final CategoryBudgetRepository categoryBudgetRepository;
    private final BudgetAlertLogRepository alertLogRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Avalia as transações recebidas no evento pós-ingestão e verifica estouro ou proximidade de teto de orçamento.
     *
     * @param transactions Lista de transações salvas/atualizadas
     */
    @Transactional
    public void evaluateTransactions(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        // Obtém o conjunto de categorias internas afetadas pelas novas transações
        Set<InternalCategory> categories = transactions.stream()
                .map(Transaction::getInternalCategory)
                .filter(cat -> cat != null)
                .collect(Collectors.toSet());

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        String currentYearMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        for (InternalCategory category : categories) {
            Optional<CategoryBudget> budgetOpt = categoryBudgetRepository.findByCategory(category);
            if (budgetOpt.isEmpty()) {
                continue;
            }

            CategoryBudget budget = budgetOpt.get();
            BigDecimal totalSpent = transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(
                    category, TransactionType.DEBIT, startDate, endDate);

            if (totalSpent == null) {
                totalSpent = BigDecimal.ZERO;
            }

            if (budget.getMonthlyLimit() == null || budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal percentageUsed = totalSpent.multiply(new BigDecimal("100"))
                    .divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);

            BigDecimal threshold = budget.getAlertThresholdPercentage() != null
                    ? budget.getAlertThresholdPercentage()
                    : new BigDecimal("80.00");

            boolean isExceeded = totalSpent.compareTo(budget.getMonthlyLimit()) >= 0;
            boolean isThresholdReached = percentageUsed.compareTo(threshold) >= 0;

            if (isExceeded || isThresholdReached) {
                String alertType = isExceeded ? "ESTOURO DE TETO" : "ALERTA DE PROXIMIDADE";
                String message = String.format("[%s] Categoria '%s' (%s): Gasto mensal de R$ %.2f de um limite de R$ %.2f (%.1f%% utilizado).",
                        alertType, category.name(), category.getDescription(), totalSpent, budget.getMonthlyLimit(), percentageUsed);

                if (isExceeded) {
                    log.error("🚨 ALERTA CRÍTICO DE ORÇAMENTO: {}", message);
                } else {
                    log.warn("⚠️ ALERTA DE ORÇAMENTO: {}", message);
                }

                // Registra ou atualiza o log de alerta do mês vigente para essa categoria
                BudgetAlertLog alertLog = alertLogRepository.findByCategoryAndYearMonth(category, currentYearMonth)
                        .orElseGet(() -> BudgetAlertLog.builder()
                                .category(category)
                                .yearMonth(currentYearMonth)
                                .build());

                alertLog.setMonthlyLimit(budget.getMonthlyLimit());
                alertLog.setTotalSpent(totalSpent);
                alertLog.setPercentageUsed(percentageUsed);
                alertLog.setAlertMessage(message);

                alertLogRepository.save(alertLog);
            }
        }
    }
}
