package com.finance.pluggy.domain;

import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.DashboardService;
import com.finance.pluggy.domain.service.InvoiceService;
import com.finance.pluggy.infrastructure.rest.dto.AccountGroupSummaryResponse;
import com.finance.pluggy.infrastructure.rest.dto.CategoryExpenseReportResponse;
import com.finance.pluggy.infrastructure.rest.dto.DashboardSummaryResponse;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import com.finance.pluggy.infrastructure.rest.dto.MonthlyExpenseReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryBudgetRepository categoryBudgetRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Deve calcular resumo do Dashboard segregando contas bancárias, cartões e investimentos")
    void shouldCalculateDashboardSummary() {
        when(invoiceService.getInvoices()).thenReturn(List.of());
        when(accountRepository.sumBankAccountsBalance()).thenReturn(new BigDecimal("5000.00"));
        when(accountRepository.sumCreditCardBalance()).thenReturn(new BigDecimal("1200.00"));
        when(accountRepository.sumInvestmentBalance()).thenReturn(new BigDecimal("10000.00"));

        when(transactionRepository.sumAmountByTypeAndDateBetween(eq(TransactionType.DEBIT), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1200.00"));
        when(transactionRepository.sumAmountByTypeAndDateBetween(eq(TransactionType.CREDIT), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("3000.00"));

        when(itemRepository.count()).thenReturn(2L);
        when(accountRepository.count()).thenReturn(3L);

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();

        assertThat(summary.getTotalBankBalance()).isEqualByComparingTo("5000.00");
        assertThat(summary.getTotalCreditCardBalance()).isEqualByComparingTo("1200.00");
        assertThat(summary.getTotalInvestmentBalance()).isEqualByComparingTo("10000.00");
        assertThat(summary.getNetWorth()).isEqualByComparingTo("13800.00"); // (5000 + 10000) - 1200
        assertThat(summary.getTotalExpensesCurrentMonth()).isEqualByComparingTo("1200.00");
        assertThat(summary.getTotalIncomeCurrentMonth()).isEqualByComparingTo("3000.00");
        assertThat(summary.getNetSavingsCurrentMonth()).isEqualByComparingTo("1800.00");
    }

    @Test
    @DisplayName("Deve agrupar contas por tipo no AccountOverview")
    void shouldGetAccountOverview() {
        Account bankAcc = Account.builder()
                .id(1L)
                .name("Nubank")
                .type(AccountType.BANK)
                .subtype(AccountSubtype.CHECKING_ACCOUNT)
                .balance(new BigDecimal("1000.00"))
                .build();

        Account creditAcc = Account.builder()
                .id(2L)
                .name("Itaú Cartão")
                .number("7425")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balance(new BigDecimal("500.00"))
                .build();

        Account investAcc = Account.builder()
                .id(3L)
                .name("Renda Fixa CDB")
                .marketingName("Renda Fixa")
                .type(AccountType.INVESTMENT)
                .subtype(AccountSubtype.INVESTMENT_ACCOUNT)
                .balance(new BigDecimal("2000.00"))
                .build();

        when(invoiceService.getInvoices()).thenReturn(List.of());
        when(accountRepository.findAll()).thenReturn(List.of(bankAcc, creditAcc, investAcc));

        AccountGroupSummaryResponse overview = dashboardService.getAccountOverview();

        assertThat(overview.getBankAccountsGroup().getTotalBalance()).isEqualByComparingTo("1000.00");
        assertThat(overview.getBankAccountsGroup().getItems()).hasSize(1);
        assertThat(overview.getBankAccountsGroup().getItems().get(0).getName()).isEqualTo("Nubank");

        assertThat(overview.getCreditCardsGroup().getTotalSpent()).isEqualByComparingTo("500.00");
        assertThat(overview.getCreditCardsGroup().getItems()).hasSize(1);
        assertThat(overview.getCreditCardsGroup().getItems().get(0).getMaskedNumber()).isEqualTo("xxxx 7425");

        assertThat(overview.getInvestmentsGroup().getTotalBalance()).isEqualByComparingTo("2000.00");
        assertThat(overview.getInvestmentsGroup().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Deve filtrar apenas faturas atuais (isCurrent = true) ao calcular resumo e overview no Dashboard")
    void shouldOnlySumCurrentInvoicesInDashboardSummary() {
        InvoiceResponse pastInvoice = InvoiceResponse.builder()
                .accountId(2L)
                .currentBalance(new BigDecimal("500.00"))
                .isCurrent(false)
                .status("PAID")
                .build();

        InvoiceResponse currentInvoice = InvoiceResponse.builder()
                .accountId(2L)
                .currentBalance(new BigDecimal("1200.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .isCurrent(true)
                .status("OPEN")
                .build();

        InvoiceResponse futureInvoice = InvoiceResponse.builder()
                .accountId(2L)
                .currentBalance(new BigDecimal("800.00"))
                .isCurrent(false)
                .status("OPEN")
                .build();

        Account creditAcc = Account.builder()
                .id(2L)
                .name("Itaú Cartão")
                .number("7425")
                .type(AccountType.CREDIT)
                .subtype(AccountSubtype.CREDIT_CARD)
                .balance(new BigDecimal("1200.00"))
                .build();

        when(invoiceService.getInvoices()).thenReturn(List.of(pastInvoice, currentInvoice, futureInvoice));
        when(accountRepository.sumBankAccountsBalance()).thenReturn(new BigDecimal("5000.00"));
        when(accountRepository.sumInvestmentBalance()).thenReturn(new BigDecimal("0.00"));
        when(accountRepository.findAll()).thenReturn(List.of(creditAcc));

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        AccountGroupSummaryResponse overview = dashboardService.getAccountOverview();

        // Saldo de cartão deve ser exatamente R$ 1200.00 (somente a fatura atual, desconsiderando R$ 500 de passada e R$ 800 de futura)
        assertThat(summary.getTotalCreditCardBalance()).isEqualByComparingTo("1200.00");
        assertThat(overview.getCreditCardsGroup().getTotalSpent()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("Deve agrupar despesas por categoria com percentuais corretos")
    void shouldGroupExpensesByCategory() {
        Object[] cat1 = new Object[]{InternalCategory.ALIMENTACAO, new BigDecimal("600.00"), 4L};
        Object[] cat2 = new Object[]{InternalCategory.TRANSPORTE, new BigDecimal("400.00"), 2L};

        when(transactionRepository.sumAmountGroupedByCategoryAndDateBetween(eq(TransactionType.DEBIT), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(cat1, cat2));

        List<CategoryExpenseReportResponse> reports = dashboardService.getExpensesByCategory(2026, 9);

        assertThat(reports).hasSize(2);

        CategoryExpenseReportResponse report1 = reports.get(0);
        assertThat(report1.getCategory()).isEqualTo(InternalCategory.ALIMENTACAO);
        assertThat(report1.getTotalAmount()).isEqualByComparingTo("600.00");
        assertThat(report1.getPercentageOfTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Deve retornar o histórico mensal dos últimos N meses")
    void shouldGetMonthlyHistory() {
        when(transactionRepository.sumAmountByTypeAndDateBetween(any(), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        List<MonthlyExpenseReportResponse> history = dashboardService.getMonthlyHistory(3);

        assertThat(history).hasSize(3);
        assertThat(history.get(2).getYearMonth()).isEqualTo(LocalDate.now().toString().substring(0, 7));
    }
}
