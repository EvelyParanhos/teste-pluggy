package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.AccountSubtype;
import com.finance.pluggy.domain.model.AccountType;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.finance.pluggy.domain.model.TransactionType;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Retorna a lista de faturas ativas e projetadas para todos os cartões de crédito.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices() {
        List<Account> allAccounts = accountRepository.findAll();
        List<InvoiceResponse> invoices = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Account acc : allAccounts) {
            if (isCreditCard(acc)) {
                List<Transaction> accountTxs = transactionRepository.findByAccountId(acc.getId());

                LocalDate closeDate = acc.getBalanceCloseDate() != null
                        ? acc.getBalanceCloseDate()
                        : now.withDayOfMonth(now.lengthOfMonth()).minusDays(5);

                LocalDate dueDate = acc.getBalanceDueDate();
                if (dueDate == null || !dueDate.isAfter(closeDate)) {
                    dueDate = closeDate.plusDays(10);
                }

                List<Transaction> currentTxs = new ArrayList<>();
                List<Transaction> futureTxs = new ArrayList<>();

                BigDecimal currentBalance = BigDecimal.ZERO;
                BigDecimal futureBalance = BigDecimal.ZERO;

                for (Transaction tx : accountTxs) {
                    BigDecimal txAmount = tx.getAmount() != null ? tx.getAmount().abs() : BigDecimal.ZERO;

                    if (tx.getDate() != null && tx.getDate().isAfter(closeDate)) {
                        futureTxs.add(tx);
                        if (tx.getType() == TransactionType.DEBIT) {
                            futureBalance = futureBalance.add(txAmount);
                        } else if (tx.getType() == TransactionType.CREDIT) {
                            futureBalance = futureBalance.subtract(txAmount);
                        }
                    } else {
                        currentTxs.add(tx);
                        if (tx.getType() == TransactionType.DEBIT) {
                            currentBalance = currentBalance.add(txAmount);
                        } else if (tx.getType() == TransactionType.CREDIT) {
                            currentBalance = currentBalance.subtract(txAmount);
                        }
                    }
                }

                // Se o saldo da conta fornecido pela API for positivo e maior que zero, utiliza como fallback
                if (currentBalance.compareTo(BigDecimal.ZERO) == 0 && acc.getBalance() != null && acc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    currentBalance = acc.getBalance();
                }

                BigDecimal limit = acc.getCreditLimit() != null && acc.getCreditLimit().compareTo(BigDecimal.ZERO) > 0
                        ? acc.getCreditLimit()
                        : new BigDecimal("5000.00");

                BigDecimal availableLimit = acc.getAvailableCreditLimit();
                BigDecimal totalUsedLimit;
                if (availableLimit != null) {
                    totalUsedLimit = limit.subtract(availableLimit).max(BigDecimal.ZERO);
                } else {
                    totalUsedLimit = currentBalance.add(futureBalance).max(BigDecimal.ZERO);
                    availableLimit = limit.subtract(totalUsedLimit).max(BigDecimal.ZERO);
                }

                BigDecimal utilizationPct = limit.compareTo(BigDecimal.ZERO) > 0
                        ? totalUsedLimit.multiply(new BigDecimal("100")).divide(limit, 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                String status = "OPEN";
                if (dueDate.isBefore(now) && currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                    status = "OVERDUE";
                } else if (closeDate.isBefore(now)) {
                    status = "CLOSED";
                }

                String maskedNum = acc.getNumber() != null && acc.getNumber().length() >= 4
                        ? "xxxx " + acc.getNumber().substring(acc.getNumber().length() - 4)
                        : "xxxx 0000";

                invoices.add(InvoiceResponse.builder()
                        .accountId(acc.getId())
                        .accountName(acc.getName() != null ? acc.getName() : "Cartão de Crédito")
                        .maskedNumber(maskedNum)
                        .status(status)
                        .currentBalance(currentBalance)
                        .futureBalance(futureBalance)
                        .totalUsedLimit(totalUsedLimit)
                        .creditLimit(limit)
                        .availableCreditLimit(availableLimit)
                        .utilizationPercentage(utilizationPct)
                        .balanceCloseDate(closeDate)
                        .balanceDueDate(dueDate)
                        .minimumPaymentAmount(acc.getMinimumPaymentAmount())
                        .transactionCount(currentTxs.size())
                        .transactions(currentTxs)
                        .futureTransactions(futureTxs)
                        .build());
            }
        }

        return invoices;
    }

    private boolean isCreditCard(Account acc) {
        return acc.getType() == AccountType.CREDIT || acc.getSubtype() == AccountSubtype.CREDIT_CARD;
    }
}
