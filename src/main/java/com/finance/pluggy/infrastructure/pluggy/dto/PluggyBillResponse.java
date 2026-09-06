package com.finance.pluggy.infrastructure.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluggyBillResponse {
    private String id;
    private String dueDate;
    private String billClosingDate;
    private BigDecimal totalAmount;
    private BigDecimal totalBalance;
    private BigDecimal minimumPaymentAmount;
    private List<PluggyBillPaymentResponse> payments;
}
