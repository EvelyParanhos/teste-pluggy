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
public class PluggyAccountResponse {
    private String id;
    private String itemId;
    private String type;
    private String subtype;
    private String name;
    private String marketingName;
    private BigDecimal balance;
    private String currencyCode;
    private String number;
    private String agency;
}
