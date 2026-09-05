package com.finance.pluggy.infrastructure.rest.dto;

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
public class AccountGroupSummaryResponse {

    private BankAccountsGroup bankAccountsGroup;
    private CreditCardsGroup creditCardsGroup;
    private InvestmentsGroup investmentsGroup;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankAccountsGroup {
        private BigDecimal totalBalance;
        private List<BankAccountItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BankAccountItem {
        private Long id;
        private String institutionName;
        private String name;
        private String number;
        private BigDecimal balance;
        private BigDecimal percentageShare;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditCardsGroup {
        private BigDecimal totalSpent;
        private BigDecimal totalLimit;
        private BigDecimal utilizationPercentage;
        private List<CreditCardItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditCardItem {
        private Long id;
        private String name;
        private String maskedNumber;
        private BigDecimal balance; // Fatura atual
        private BigDecimal limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestmentsGroup {
        private BigDecimal totalBalance;
        private List<InvestmentItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestmentItem {
        private Long id;
        private String name;
        private String assetClass;
        private BigDecimal balance;
        private BigDecimal percentageShare;
    }
}
