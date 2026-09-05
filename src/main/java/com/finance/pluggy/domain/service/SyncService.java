package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.event.ItemSyncedEvent;
import com.finance.pluggy.domain.event.NewTransactionsIngestedEvent;
import com.finance.pluggy.domain.mapper.PluggyDomainMapper;
import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.Item;
import com.finance.pluggy.domain.model.SyncLog;
import com.finance.pluggy.domain.model.SyncLogStatus;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final PluggyClient pluggyClient;
    private final ItemRepository itemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SyncLogRepository syncLogRepository;
    private final PluggyDomainMapper pluggyDomainMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Sincroniza um Item, suas contas e transações buscando o estado mais atualizado da API da Pluggy.
     * As chamadas HTTP externas são executadas fora de transações ativas para evitar reter conexões JDBC
     * do pool durante requisições de rede. Cada alteração no banco é persistida via Spring Data Repositories.
     *
     * @param pluggyItemId ID do Item no Pluggy
     */
    public void syncItem(String pluggyItemId) {
        log.info("Iniciando sincronização completa para o Item ID: {}", pluggyItemId);

        try {
            // 1. Confirma o estado atualizado do Item diretamente na API do Pluggy
            PluggyItemResponse itemResponse = pluggyClient.getItem(pluggyItemId);
            if (itemResponse == null) {
                log.error("Item não encontrado na API do Pluggy para o ID: {}", pluggyItemId);
                handleSyncFailure(pluggyItemId, new IllegalArgumentException("Item não encontrado na API Pluggy"));
                return;
            }

            Item existingItem = itemRepository.findByPluggyItemId(pluggyItemId).orElse(null);
            Item item = itemRepository.save(pluggyDomainMapper.toItemEntity(itemResponse, existingItem));

            // 2. Busca e sincroniza as contas vinculadas a este Item
            PluggyPageResponse<PluggyAccountResponse> accountsResponse = pluggyClient.getAccounts(pluggyItemId);
            if (accountsResponse == null || accountsResponse.getResults() == null || accountsResponse.getResults().isEmpty()) {
                log.info("Nenhuma conta encontrada para o Item ID: {}", pluggyItemId);
                handleSyncSuccess(pluggyItemId);
                eventPublisher.publishEvent(new ItemSyncedEvent(this, item.getId(), pluggyItemId, 0, 0));
                return;
            }

            int totalAccountsSynced = 0;
            List<Transaction> allIngestedTransactions = new ArrayList<>();

            for (PluggyAccountResponse accountDto : accountsResponse.getResults()) {
                Account existingAccount = accountRepository.findByPluggyAccountId(accountDto.getId()).orElse(null);
                Account account = accountRepository.save(pluggyDomainMapper.toAccountEntity(accountDto, item, existingAccount));
                totalAccountsSynced++;

                // 3. Busca e sincroniza as transações para cada conta usando paginação por cursor (v2)
                List<Transaction> accountTransactions = syncTransactionsForAccount(account);
                allIngestedTransactions.addAll(accountTransactions);
            }

            // Marca o log de sincronização como SUCESSO
            handleSyncSuccess(pluggyItemId);

            log.info("Sincronização concluída com sucesso para o Item ID: {} (Contas: {}, Transações: {})",
                    pluggyItemId, totalAccountsSynced, allIngestedTransactions.size());

            // 4. Publica evento de métricas da sincronização do item
            eventPublisher.publishEvent(new ItemSyncedEvent(
                    this, item.getId(), pluggyItemId, totalAccountsSynced, allIngestedTransactions.size()));

            // 5. Publica evento com a lista de transações novas ingeridas
            if (!allIngestedTransactions.isEmpty()) {
                eventPublisher.publishEvent(new NewTransactionsIngestedEvent(
                        this, item.getId(), pluggyItemId, allIngestedTransactions));
            }
        } catch (Exception e) {
            log.error("Erro durante a sincronização do Item {}: {}", pluggyItemId, e.getMessage(), e);
            handleSyncFailure(pluggyItemId, e);
        }
    }

    private List<Transaction> syncTransactionsForAccount(Account account) {
        String nextCursor = null;
        List<Transaction> savedTransactions = new ArrayList<>();

        do {
            PluggyPageResponse<PluggyTransactionResponse> txPage = pluggyClient.getTransactions(
                    account.getPluggyAccountId(), null, null, nextCursor);

            if (txPage == null || txPage.getResults() == null || txPage.getResults().isEmpty()) {
                break;
            }

            for (PluggyTransactionResponse txDto : txPage.getResults()) {
                Transaction existingTx = transactionRepository.findByPluggyTransactionId(txDto.getId()).orElse(null);
                Transaction savedTx = transactionRepository.save(pluggyDomainMapper.toTransactionEntity(txDto, account, existingTx));
                savedTransactions.add(savedTx);
            }

            nextCursor = txPage.getNext();
        } while (nextCursor != null && !nextCursor.isBlank());

        return savedTransactions;
    }

    private void handleSyncSuccess(String pluggyItemId) {
        SyncLog syncLog = syncLogRepository.findByPluggyItemId(pluggyItemId)
                .orElseGet(() -> SyncLog.builder().pluggyItemId(pluggyItemId).build());

        syncLog.setStatus(SyncLogStatus.SUCCESS);
        syncLog.setAttempts(0);
        syncLog.setLastError(null);
        syncLog.setNextAttemptAt(null);

        syncLogRepository.save(syncLog);
    }

    private void handleSyncFailure(String pluggyItemId, Exception e) {
        SyncLog syncLog = syncLogRepository.findByPluggyItemId(pluggyItemId)
                .orElseGet(() -> SyncLog.builder().pluggyItemId(pluggyItemId).attempts(0).build());

        int currentAttempts = (syncLog.getAttempts() != null ? syncLog.getAttempts() : 0) + 1;
        syncLog.setAttempts(currentAttempts);
        syncLog.setLastError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

        if (currentAttempts < MAX_RETRY_ATTEMPTS) {
            syncLog.setStatus(SyncLogStatus.PENDING);
            long delayMinutes = (long) Math.pow(2, currentAttempts);
            syncLog.setNextAttemptAt(LocalDateTime.now().plusMinutes(delayMinutes));
            log.warn("Agendada nova tentativa de sincronização para o Item {} (Tentativa {}/{}) em {} minutos ({})",
                    pluggyItemId, currentAttempts, MAX_RETRY_ATTEMPTS, delayMinutes, syncLog.getNextAttemptAt());
        } else {
            syncLog.setStatus(SyncLogStatus.FAILED);
            syncLog.setNextAttemptAt(null);
            log.error("Sincronização para o Item {} FALHOU definitivamente após {} tentativas.", pluggyItemId, currentAttempts);
        }

        syncLogRepository.save(syncLog);
    }
}
