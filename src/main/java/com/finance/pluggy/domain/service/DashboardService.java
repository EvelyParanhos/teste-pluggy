package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.rest.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final InvoiceService invoiceService;

    /**
     * Retorna o resumo consolidado do Dashboard segregando liquidez, cartões e investimentos.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        BigDecimal bankBalance = accountRepository.sumBankAccountsBalance();
        if (bankBalance == null) bankBalance = BigDecimal.ZERO;

        List<InvoiceResponse> invoices = invoiceService.getInvoices();
        BigDecimal creditBalance;
        if (invoices != null && !invoices.isEmpty()) {
            creditBalance = invoices.stream()
                    .map(inv -> inv.getCurrentBalance() != null ? inv.getCurrentBalance() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            creditBalance = accountRepository.sumCreditCardBalance();
            if (creditBalance == null) creditBalance = BigDecimal.ZERO;
        }

        BigDecimal investmentBalance = accountRepository.sumInvestmentBalance();
        if (investmentBalance == null) investmentBalance = BigDecimal.ZERO;

        BigDecimal netWorth = bankBalance.add(investmentBalance).subtract(creditBalance);

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalExpenses = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.DEBIT, startDate, endDate);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.CREDIT, startDate, endDate);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        return DashboardSummaryResponse.builder()
                .totalConsolidatedBalance(bankBalance)
                .totalBankBalance(bankBalance)
                .totalCreditCardBalance(creditBalance)
                .totalInvestmentBalance(investmentBalance)
                .netWorth(netWorth)
                .totalIncomeCurrentMonth(totalIncome)
                .totalExpensesCurrentMonth(totalExpenses)
                .netSavingsCurrentMonth(netSavings)
                .activeItemsCount(itemRepository.count())
                .activeAccountsCount(accountRepository.count())
                .build();
    }

    /**
     * Retorna os dados agrupados por Contas Bancárias, Cartões de Crédito e Investimentos para os cards de Overview.
     */
    @Transactional(readOnly = true)
    public AccountGroupSummaryResponse getAccountOverview() {
        List<Account> allAccounts = accountRepository.findAll();
        List<InvoiceResponse> invoices = invoiceService.getInvoices();
        Map<Long, InvoiceResponse> invoiceMap = new HashMap<>();
        for (InvoiceResponse inv : invoices) {
            invoiceMap.put(inv.getAccountId(), inv);
        }

        BigDecimal bankTotal = BigDecimal.ZERO;
        List<AccountGroupSummaryResponse.BankAccountItem> bankItems = new ArrayList<>();

        BigDecimal creditTotal = BigDecimal.ZERO;
        BigDecimal totalLimit = BigDecimal.ZERO;
        List<AccountGroupSummaryResponse.CreditCardItem> creditItems = new ArrayList<>();

        BigDecimal investmentTotal = BigDecimal.ZERO;
        List<AccountGroupSummaryResponse.InvestmentItem> investmentItems = new ArrayList<>();

        for (Account acc : allAccounts) {
            BigDecimal bal = acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO;

            if (isCreditCard(acc)) {
                InvoiceResponse inv = invoiceMap.get(acc.getId());
                BigDecimal cardBalance = inv != null && inv.getCurrentBalance() != null ? inv.getCurrentBalance() : bal;
                BigDecimal cardLimit = inv != null && inv.getCreditLimit() != null ? inv.getCreditLimit() : new BigDecimal("5000.00");

                creditTotal = creditTotal.add(cardBalance);
                totalLimit = totalLimit.add(cardLimit);

                String maskedNum = acc.getNumber() != null && acc.getNumber().length() >= 4
                        ? "xxxx " + acc.getNumber().substring(acc.getNumber().length() - 4)
                        : "xxxx 0000";

                creditItems.add(AccountGroupSummaryResponse.CreditCardItem.builder()
                        .id(acc.getId())
                        .name(acc.getName() != null ? acc.getName() : "Cartão de Crédito")
                        .maskedNumber(maskedNum)
                        .balance(cardBalance)
                        .limit(cardLimit)
                        .build());
            } else if (isInvestment(acc)) {
                investmentTotal = investmentTotal.add(bal);
                investmentItems.add(AccountGroupSummaryResponse.InvestmentItem.builder()
                        .id(acc.getId())
                        .name(acc.getName() != null ? acc.getName() : "Aplicação Financeira")
                        .assetClass(acc.getMarketingName() != null ? acc.getMarketingName() : "Renda Fixa")
                        .balance(bal)
                        .percentageShare(BigDecimal.ZERO)
                        .build());
            } else {
                bankTotal = bankTotal.add(bal);
                bankItems.add(AccountGroupSummaryResponse.BankAccountItem.builder()
                        .id(acc.getId())
                        .institutionName(acc.getItem() != null && acc.getItem().getConnectorName() != null 
                                ? acc.getItem().getConnectorName() : acc.getName())
                        .name(acc.getName())
                        .number(acc.getNumber() != null ? acc.getNumber() : "-")
                        .balance(bal)
                        .percentageShare(BigDecimal.ZERO)
                        .build());
            }
        }

        // Calcula percentuais de participação bancária
        if (bankTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (AccountGroupSummaryResponse.BankAccountItem item : bankItems) {
                BigDecimal share = item.getBalance().multiply(new BigDecimal("100"))
                        .divide(bankTotal, 1, RoundingMode.HALF_UP);
                item.setPercentageShare(share);
            }
        }

        // Calcula percentuais de investimentos
        if (investmentTotal.compareTo(BigDecimal.ZERO) > 0) {
            for (AccountGroupSummaryResponse.InvestmentItem item : investmentItems) {
                BigDecimal share = item.getBalance().multiply(new BigDecimal("100"))
                        .divide(investmentTotal, 1, RoundingMode.HALF_UP);
                item.setPercentageShare(share);
            }
        }

        // Calcula uso do limite de crédito
        BigDecimal utilizationPct = BigDecimal.ZERO;
        if (totalLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPct = creditTotal.multiply(new BigDecimal("100"))
                    .divide(totalLimit, 1, RoundingMode.HALF_UP);
        }

        return AccountGroupSummaryResponse.builder()
                .bankAccountsGroup(AccountGroupSummaryResponse.BankAccountsGroup.builder()
                        .totalBalance(bankTotal)
                        .items(bankItems)
                        .build())
                .creditCardsGroup(AccountGroupSummaryResponse.CreditCardsGroup.builder()
                        .totalSpent(creditTotal)
                        .totalLimit(totalLimit)
                        .utilizationPercentage(utilizationPct)
                        .items(creditItems)
                        .build())
                .investmentsGroup(AccountGroupSummaryResponse.InvestmentsGroup.builder()
                        .totalBalance(investmentTotal)
                        .items(investmentItems)
                        .build())
                .build();
    }

    private boolean isCreditCard(Account acc) {
        return acc.getType() == AccountType.CREDIT || acc.getSubtype() == AccountSubtype.CREDIT_CARD;
    }

    private boolean isInvestment(Account acc) {
        return acc.getType() == AccountType.INVESTMENT || acc.getSubtype() == AccountSubtype.INVESTMENT_ACCOUNT;
    }

    /**
     * Retorna o comparativo de gastos agrupados por categoria para o ano/mês especificado.
     */
    @Transactional(readOnly = true)
    public List<CategoryExpenseReportResponse> getExpensesByCategory(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Object[]> rawGrouped = transactionRepository.sumAmountGroupedByCategoryAndDateBetween(
                TransactionType.DEBIT, startDate, endDate);

        BigDecimal totalExpensesAllCategories = rawGrouped.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryExpenseReportResponse> responseList = new ArrayList<>();

        for (Object[] row : rawGrouped) {
            InternalCategory category = (InternalCategory) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            long count = (long) row[2];

            BigDecimal percentageOfTotal = BigDecimal.ZERO;
            if (totalExpensesAllCategories.compareTo(BigDecimal.ZERO) > 0) {
                percentageOfTotal = totalAmount.multiply(new BigDecimal("100"))
                        .divide(totalExpensesAllCategories, 2, RoundingMode.HALF_UP);
            }

            Optional<CategoryBudget> budgetOpt = categoryBudgetRepository.findByCategory(category);
            BigDecimal monthlyLimit = budgetOpt.map(CategoryBudget::getMonthlyLimit).orElse(null);

            responseList.add(CategoryExpenseReportResponse.builder()
                    .category(category)
                    .categoryDescription(category != null ? category.getDescription() : "Não Categorizado")
                    .totalAmount(totalAmount)
                    .percentageOfTotal(percentageOfTotal)
                    .transactionCount(count)
                    .monthlyLimit(monthlyLimit)
                    .build());
        }

        return responseList;
    }

    /**
     * Retorna o histórico mensal de receitas e despesas dos últimos N meses.
     */
    @Transactional(readOnly = true)
    public List<MonthlyExpenseReportResponse> getMonthlyHistory(Integer monthsCount) {
        int limit = monthsCount != null && monthsCount > 0 ? monthsCount : 6;
        LocalDate now = LocalDate.now();
        List<MonthlyExpenseReportResponse> history = new ArrayList<>();

        for (int i = limit - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            LocalDate startDate = monthDate.withDayOfMonth(1);
            LocalDate endDate = monthDate.withDayOfMonth(monthDate.lengthOfMonth());

            String yearMonth = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String monthName = monthDate.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
                    + " " + monthDate.getYear();

            BigDecimal totalExpenses = transactionRepository.sumAmountByTypeAndDateBetween(
                    TransactionType.DEBIT, startDate, endDate);
            if (totalExpenses == null) {
                totalExpenses = BigDecimal.ZERO;
            }

            BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDateBetween(
                    TransactionType.CREDIT, startDate, endDate);
            if (totalIncome == null) {
                totalIncome = BigDecimal.ZERO;
            }

            BigDecimal netResult = totalIncome.subtract(totalExpenses);

            history.add(MonthlyExpenseReportResponse.builder()
                    .yearMonth(yearMonth)
                    .monthName(monthName)
                    .totalIncome(totalIncome)
                    .totalExpenses(totalExpenses)
                    .netResult(netResult)
                    .build());
        }

        return history;
    }
}
