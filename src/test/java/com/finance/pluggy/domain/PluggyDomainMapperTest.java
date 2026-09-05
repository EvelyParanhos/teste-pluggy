package com.finance.pluggy.domain;

import com.finance.pluggy.domain.mapper.PluggyDomainMapper;
import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.service.CategoryResolutionService;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluggyDomainMapperTest {

    @Mock
    private CategoryResolutionService categoryResolutionService;

    @InjectMocks
    private PluggyDomainMapper mapper;

    @Test
    @DisplayName("Deve mapear PluggyItemResponse para entidade Item")
    void shouldMapItemResponseToEntity() {
        PluggyItemResponse dto = PluggyItemResponse.builder()
                .id("item-1")
                .status("UPDATED")
                .build();

        Item item = mapper.toItemEntity(dto, null);

        assertThat(item.getPluggyItemId()).isEqualTo("item-1");
        assertThat(item.getStatus()).isEqualTo(ItemStatus.UPDATED);
    }

    @Test
    @DisplayName("Deve mapear PluggyAccountResponse para entidade Account")
    void shouldMapAccountResponseToEntity() {
        Item item = Item.builder().id(10L).pluggyItemId("item-1").build();
        PluggyAccountResponse dto = PluggyAccountResponse.builder()
                .id("acc-1")
                .name("Conta Corrente")
                .type("BANK")
                .balance(new BigDecimal("1000.00"))
                .build();

        Account account = mapper.toAccountEntity(dto, item, null);

        assertThat(account.getPluggyAccountId()).isEqualTo("acc-1");
        assertThat(account.getName()).isEqualTo("Conta Corrente");
        assertThat(account.getType()).isEqualTo(AccountType.BANK);
        assertThat(account.getItem()).isEqualTo(item);
    }

    @Test
    @DisplayName("Deve mapear PluggyTransactionResponse para entidade Transaction com categoria interna resolvida")
    void shouldMapTransactionResponseAndResolveCategory() {
        Account account = Account.builder().id(20L).pluggyAccountId("acc-1").build();
        PluggyTransactionResponse dto = PluggyTransactionResponse.builder()
                .id("tx-1")
                .description("Supermercado Carrefour")
                .amount(new BigDecimal("250.00"))
                .date("2026-09-05")
                .type("DEBIT")
                .status("POSTED")
                .category("Groceries")
                .build();

        when(categoryResolutionService.resolveCategory("Groceries", "Supermercado Carrefour")).thenReturn(InternalCategory.ALIMENTACAO);

        Transaction tx = mapper.toTransactionEntity(dto, account, null);

        assertThat(tx.getPluggyTransactionId()).isEqualTo("tx-1");
        assertThat(tx.getDescription()).isEqualTo("Supermercado Carrefour");
        assertThat(tx.getAmount()).isEqualTo(new BigDecimal("250.00"));
        assertThat(tx.getPluggyCategory()).isEqualTo("Groceries");
        assertThat(tx.getInternalCategory()).isEqualTo(InternalCategory.ALIMENTACAO);
        assertThat(tx.getAccount()).isEqualTo(account);
    }
}
