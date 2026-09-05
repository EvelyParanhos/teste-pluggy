package com.finance.pluggy.domain;

import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.Item;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class IdempotentSyncIntegrationTest {

    @MockBean
    private PluggyClient pluggyClient;

    @Autowired
    private SyncService syncService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("Garantir que execuções repetidas da sincronização atualizem os registros sem duplicá-los no banco")
    void shouldPerformIdempotentSyncWithoutDuplicates() {
        String itemId = "pluggy-item-id-999";
        String accountId = "pluggy-acc-id-888";
        String transactionId = "pluggy-tx-id-777";

        // PRIMEIRA EXECUÇÃO DE SINCRONIZAÇÃO
        PluggyItemResponse itemDto1 = PluggyItemResponse.builder()
                .id(itemId)
                .status("UPDATED")
                .build();

        PluggyAccountResponse accountDto1 = PluggyAccountResponse.builder()
                .id(accountId)
                .itemId(itemId)
                .name("Conta Corrente")
                .balance(new BigDecimal("1000.00"))
                .currencyCode("BRL")
                .build();

        PluggyTransactionResponse txDto1 = PluggyTransactionResponse.builder()
                .id(transactionId)
                .accountId(accountId)
                .description("Supermercado Silva")
                .amount(new BigDecimal("85.50"))
                .date("2026-09-05")
                .category("Food & Dining")
                .build();

        when(pluggyClient.getItem(itemId)).thenReturn(itemDto1);
        when(pluggyClient.getAccounts(itemId)).thenReturn(
                PluggyPageResponse.<PluggyAccountResponse>builder().page(1).totalPages(1).results(List.of(accountDto1)).build());
        when(pluggyClient.getTransactions(accountId, null, null, null)).thenReturn(
                PluggyPageResponse.<PluggyTransactionResponse>builder().results(List.of(txDto1)).next(null).build());

        syncService.syncItem(itemId);

        // Validação da 1ª execução
        assertThat(itemRepository.count()).isEqualTo(1);
        assertThat(accountRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);

        Account accountAfterFirstSync = accountRepository.findByPluggyAccountId(accountId).orElseThrow();
        assertThat(accountAfterFirstSync.getBalance()).isEqualByComparingTo("1000.00");

        // SEGUNDA EXECUÇÃO DE SINCRONIZAÇÃO (REENVIO DE WEBHOOK COM DADOS ATUALIZADOS)
        PluggyAccountResponse accountDto2 = PluggyAccountResponse.builder()
                .id(accountId)
                .itemId(itemId)
                .name("Conta Corrente")
                .balance(new BigDecimal("1500.00"))
                .currencyCode("BRL")
                .build();

        PluggyTransactionResponse txDto2 = PluggyTransactionResponse.builder()
                .id(transactionId)
                .accountId(accountId)
                .description("Supermercado Silva - Loja Central")
                .amount(new BigDecimal("85.50"))
                .date("2026-09-05")
                .category("Food & Dining")
                .build();

        when(pluggyClient.getAccounts(itemId)).thenReturn(
                PluggyPageResponse.<PluggyAccountResponse>builder().page(1).totalPages(1).results(List.of(accountDto2)).build());
        when(pluggyClient.getTransactions(accountId, null, null, null)).thenReturn(
                PluggyPageResponse.<PluggyTransactionResponse>builder().results(List.of(txDto2)).next(null).build());

        syncService.syncItem(itemId);

        // Validação da 2ª execução (Idempotência: NENHUM registro duplicado criado)
        assertThat(itemRepository.count()).isEqualTo(1);
        assertThat(accountRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);

        Account accountAfterSecondSync = accountRepository.findByPluggyAccountId(accountId).orElseThrow();
        assertThat(accountAfterSecondSync.getBalance()).isEqualByComparingTo("1500.00");

        Transaction txAfterSecondSync = transactionRepository.findByPluggyTransactionId(transactionId).orElseThrow();
        assertThat(txAfterSecondSync.getDescription()).isEqualTo("Supermercado Silva - Loja Central");
    }
}
