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
public class PluggyTransactionResponse {
    private String id;
    private String accountId;
    private String description;
    private String rawDescription;
    private BigDecimal amount;
    private String date;
    private String type;
    private String status;
    private String category;
}
