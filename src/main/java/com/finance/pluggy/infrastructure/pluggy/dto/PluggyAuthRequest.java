package com.finance.pluggy.infrastructure.pluggy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluggyAuthRequest {
    private String clientId;
    private String clientSecret;
}
