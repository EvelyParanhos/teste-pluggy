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
public class PluggyBillPaymentResponse {
    private String id;
    private String date;
    private String paymentDate;
    private BigDecimal amount;
    private String paymentMode;
}
