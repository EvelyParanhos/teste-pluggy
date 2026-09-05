package com.finance.pluggy.infrastructure.pluggy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pluggy")
public class PluggyProperties {

    /**
     * Client ID obtido do Dashboard da Pluggy.
     */
    private String clientId;

    /**
     * Client Secret obtido do Dashboard da Pluggy.
     */
    private String clientSecret;

    /**
     * URL base da API da Pluggy (padrão: https://api.pluggy.ai).
     */
    private String baseUrl = "https://api.pluggy.ai";
}
