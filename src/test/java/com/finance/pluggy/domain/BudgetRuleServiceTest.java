package com.finance.pluggy.domain;

import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.BudgetAlertLogRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.BudgetRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetRuleServiceTest {

    @Mock
    private CategoryBudgetRepository categoryBudgetRepository;

    @Mock
    private BudgetAlertLogRepository alertLogRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetRuleService budgetRuleService;

    @Test
    @DisplayName("Deve gerar alerta quando o total gasto ultrapassar o limite configurado para a categoria")
    void shouldGenerateAlertWhenMonthlyLimitExceeded() {
        InternalCategory category = InternalCategory.ALIMENTACAO;

        CategoryBudget budget = CategoryBudget.builder()
                .id(1L)
                .category(category)
                .monthlyLimit(new BigDecimal("1000.00"))
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        Transaction tx = Transaction.builder()
                .id(100L)
                .description("Supermercado")
                .amount(new BigDecimal("1050.00"))
                .type(TransactionType.DEBIT)
                .internalCategory(category)
                .date(LocalDate.now())
                .build();

        when(categoryBudgetRepository.findByCategory(category)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(eq(category), eq(TransactionType.DEBIT), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1050.00"));
        when(alertLogRepository.findByCategoryAndYearMonth(eq(category), anyString())).thenReturn(Optional.empty());

        budgetRuleService.evaluateTransactions(List.of(tx));

        ArgumentCaptor<BudgetAlertLog> alertCaptor = ArgumentCaptor.forClass(BudgetAlertLog.class);
        verify(alertLogRepository).save(alertCaptor.capture());

        BudgetAlertLog alert = alertCaptor.getValue();
        assertThat(alert.getCategory()).isEqualTo(category);
        assertThat(alert.getTotalSpent()).isEqualByComparingTo("1050.00");
        assertThat(alert.getMonthlyLimit()).isEqualByComparingTo("1000.00");
        assertThat(alert.getAlertMessage()).contains("ESTOURO DE TETO");
    }

    @Test
    @DisplayName("Não deve gerar alerta se o gasto estiver dentro do limite normal")
    void shouldNotGenerateAlertWhenWithinNormalLimit() {
        InternalCategory category = InternalCategory.TRANSPORTE;

        CategoryBudget budget = CategoryBudget.builder()
                .id(2L)
                .category(category)
                .monthlyLimit(new BigDecimal("500.00"))
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        Transaction tx = Transaction.builder()
                .id(200L)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.DEBIT)
                .internalCategory(category)
                .date(LocalDate.now())
                .build();

        when(categoryBudgetRepository.findByCategory(category)).thenReturn(Optional.of(budget));
        when(transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(eq(category), eq(TransactionType.DEBIT), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("100.00"));

        budgetRuleService.evaluateTransactions(List.of(tx));

        verify(alertLogRepository, never()).save(any(BudgetAlertLog.class));
    }
}
