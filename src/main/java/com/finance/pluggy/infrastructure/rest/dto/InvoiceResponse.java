package com.finance.pluggy.infrastructure.rest.dto;

import com.finance.pluggy.domain.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    private Long accountId;
    private String accountName;
    private String maskedNumber;
    private String status; // OPEN, CLOSED, OVERDUE, PAID
    private BigDecimal currentBalance; // Fatura aberta (ciclo atual)
    private BigDecimal futureBalance; // Faturas futuras (projetadas)
    private BigDecimal totalUsedLimit; // Limite total utilizado
    private BigDecimal creditLimit;
    private BigDecimal availableCreditLimit;
    private BigDecimal utilizationPercentage;
    private LocalDate balanceCloseDate;
    private LocalDate balanceDueDate;
    private BigDecimal minimumPaymentAmount;
    private int transactionCount;
    private List<Transaction> transactions; // Transações do ciclo atual
    private List<Transaction> futureTransactions; // Transações futuras / parcelamentos futuros
    private boolean pendingSync;
    private boolean isCurrent;
}
