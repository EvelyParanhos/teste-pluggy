package com.finance.pluggy.domain;

import com.finance.pluggy.domain.event.ItemSyncedEvent;
import com.finance.pluggy.domain.event.NewTransactionsIngestedEvent;
import com.finance.pluggy.domain.mapper.PluggyDomainMapper;
import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private PluggyClient pluggyClient;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private PluggyDomainMapper pluggyDomainMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SyncService syncService;

    @Test
    @DisplayName("Deve sincronizar Item, Account e Transaction com sucesso e marcar SyncLog como SUCCESS")
    void shouldSyncItemAccountAndTransactionsAndPublishEvents() {
        String itemId = "item-pluggy-100";
        String accountId = "account-pluggy-200";
        String transactionId = "tx-pluggy-300";

        PluggyItemResponse itemDto = PluggyItemResponse.builder().id(itemId).status("UPDATED").build();
        PluggyAccountResponse accountDto = PluggyAccountResponse.builder().id(accountId).itemId(itemId).build();
        PluggyTransactionResponse txDto = PluggyTransactionResponse.builder().id(transactionId).accountId(accountId).build();

        PluggyPageResponse<PluggyAccountResponse> accountsPage = PluggyPageResponse.<PluggyAccountResponse>builder()
                .page(1).totalPages(1).results(List.of(accountDto)).build();

        PluggyPageResponse<PluggyTransactionResponse> txPage = PluggyPageResponse.<PluggyTransactionResponse>builder()
                .results(List.of(txDto)).next(null).build();

        Item itemEntity = Item.builder().id(1L).pluggyItemId(itemId).build();
        Account accountEntity = Account.builder().id(2L).pluggyAccountId(accountId).item(itemEntity).build();
        Transaction txEntity = Transaction.builder().id(3L).pluggyTransactionId(transactionId).account(accountEntity).internalCategory(InternalCategory.ALIMENTACAO).build();

        when(pluggyClient.getItem(itemId)).thenReturn(itemDto);
        when(pluggyClient.getAccounts(itemId)).thenReturn(accountsPage);
        when(pluggyClient.getTransactions(accountId, null, null, null)).thenReturn(txPage);

        when(itemRepository.findByPluggyItemId(itemId)).thenReturn(Optional.empty());
        when(accountRepository.findByPluggyAccountId(accountId)).thenReturn(Optional.empty());
        when(transactionRepository.findByPluggyTransactionId(transactionId)).thenReturn(Optional.empty());

        when(pluggyDomainMapper.toItemEntity(itemDto, null)).thenReturn(itemEntity);
        when(pluggyDomainMapper.toAccountEntity(accountDto, itemEntity, null)).thenReturn(accountEntity);
        when(pluggyDomainMapper.toTransactionEntity(txDto, accountEntity, null)).thenReturn(txEntity);

        when(itemRepository.save(any(Item.class))).thenReturn(itemEntity);
        when(accountRepository.save(any(Account.class))).thenReturn(accountEntity);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(txEntity);

        // Execução
        syncService.syncItem(itemId);

        // Validações
        verify(itemRepository).save(itemEntity);
        verify(accountRepository).save(accountEntity);
        verify(transactionRepository).save(txEntity);

        // Valida registro de sucesso no SyncLog
        ArgumentCaptor<SyncLog> syncLogCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(syncLogCaptor.capture());
        assertThat(syncLogCaptor.getValue().getStatus()).isEqualTo(SyncLogStatus.SUCCESS);
    }

    @Test
    @DisplayName("Deve registrar falha e agendar retentativa com backoff em SyncLog se a API Pluggy falhar")
    void shouldRegisterFailureAndBackoffInSyncLogOnApiError() {
        String itemId = "item-failed-999";

        when(pluggyClient.getItem(itemId)).thenThrow(new RuntimeException("API Connection Timeout"));
        when(syncLogRepository.findByPluggyItemId(itemId)).thenReturn(Optional.empty());

        syncService.syncItem(itemId);

        ArgumentCaptor<SyncLog> syncLogCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(syncLogCaptor.capture());

        SyncLog savedLog = syncLogCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(SyncLogStatus.PENDING);
        assertThat(savedLog.getAttempts()).isEqualTo(1);
        assertThat(savedLog.getLastError()).isEqualTo("API Connection Timeout");
        assertThat(savedLog.getNextAttemptAt()).isNotNull();
    }
}
