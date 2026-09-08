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
public class InvoiceHistoryItem {
    private Long id;
    private LocalDate closeDate;
    private LocalDate dueDate;
    private String status; // PAID, OVERDUE, OPEN, CLOSED
    private BigDecimal totalAmount;
    private BigDecimal minimumPaymentAmount;
    private List<Transaction> transactions;
}
