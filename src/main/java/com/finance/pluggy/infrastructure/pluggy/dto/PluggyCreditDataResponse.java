package com.finance.pluggy.infrastructure.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluggyCreditDataResponse {
    private BigDecimal creditLimit;
    private BigDecimal availableCreditLimit;
    private String balanceCloseDate;
    private String balanceDueDate;
    private BigDecimal minimumPaymentAmount;
}
