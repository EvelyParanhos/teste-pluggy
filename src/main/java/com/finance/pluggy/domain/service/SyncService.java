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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final com.finance.pluggy.domain.repository.InvoiceRepository invoiceRepository;
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
            // 1. Solicita a atualização em tempo real (on-demand update) via PATCH /items/{id}
            try {
                log.info("Disparando PATCH /items/{} para atualização em tempo real na Pluggy...", pluggyItemId);
                pluggyClient.requestItemUpdate(pluggyItemId);
            } catch (Exception e) {
                log.warn("Não foi possível solicitar PATCH /items/{} (usando cache/estado atual): {}", pluggyItemId, e.getMessage());
            }

            // 2. Confirma o estado do Item e aguarda conclusão caso esteja UPDATING (até 5 tentativas de 2s)
            PluggyItemResponse itemResponse = pluggyClient.getItem(pluggyItemId);
            int pollAttempts = 0;
            while (itemResponse != null && "UPDATING".equalsIgnoreCase(itemResponse.getStatus()) && pollAttempts < 5) {
                log.info("Item {} está com status UPDATING na Pluggy. Aguardando atualização do conector ({}/5)...", pluggyItemId, pollAttempts + 1);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                pollAttempts++;
                itemResponse = pluggyClient.getItem(pluggyItemId);
            }

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

                // 3. Se for cartão de crédito, busca e sincroniza as faturas (bills) oficiais
                if (isCreditCard(account)) {
                    syncBillsForAccount(account);
                }

                // 4. Busca e sincroniza as transações para cada conta usando paginação por cursor (v2)
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

    private void syncBillsForAccount(Account account) {
        List<com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse> billDtos = null;
        try {
            com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse<com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse> billsPage =
                    pluggyClient.getBills(account.getPluggyAccountId());
            if (billsPage != null && billsPage.getResults() != null && !billsPage.getResults().isEmpty()) {
                billDtos = new ArrayList<>(billsPage.getResults());

                // Ordena as faturas recebidas por dueDate ascendente
                billDtos.sort(Comparator.comparing(
                        b -> pluggyDomainMapper.parseDate(b.getDueDate()),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

                for (com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse billDto : billDtos) {
                    com.finance.pluggy.domain.model.Invoice existingInvoice =
                            invoiceRepository.findByPluggyBillId(billDto.getId()).orElse(null);
                    com.finance.pluggy.domain.model.Invoice invoice =
                            pluggyDomainMapper.toInvoiceEntity(billDto, account, existingInvoice);
                    invoiceRepository.save(invoice);
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível buscar faturas para a conta {}: {}", account.getPluggyAccountId(), e.getMessage());
        }

        try {
            // Segunda passada: reconciliação de status sobre TODAS as faturas persistidas no banco para a conta
            reconcileInvoiceStatuses(account, billDtos);
        } catch (Exception e) {
            log.warn("Erro ao reconciliar status das faturas para a conta {}: {}", account.getPluggyAccountId(), e.getMessage());
        }
    }

    /**
     * Segunda passada de reconciliação de status de pagamento:
     * 1. Regra oficial Cross-Bill: O pagamento da fatura N aparece no payments[] da fatura N+1 (ciclo seguinte).
     * 2. Fallback de alta precisão por transação: Procura por uma transação CREDIT na conta após o fechamento da fatura cujo valor coincida com totalAmount.
     */
    private void reconcileInvoiceStatuses(Account account,
                                           List<com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse> billDtos) {
        List<com.finance.pluggy.domain.model.Invoice> invoices =
                invoiceRepository.findByAccountIdOrderByDueDateAsc(account.getId());

        if (invoices == null || invoices.isEmpty()) {
            return;
        }

        Map<String, com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse> dtoMap = new HashMap<>();
        if (billDtos != null) {
            for (com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse dto : billDtos) {
                if (dto.getId() != null) {
                    dtoMap.put(dto.getId(), dto);
                }
            }
        }

        LocalDate now = LocalDate.now();
        int size = invoices.size();

        for (int i = 0; i < size; i++) {
            com.finance.pluggy.domain.model.Invoice invoice = invoices.get(i);
            BigDecimal totalPaid = BigDecimal.ZERO;
            boolean isPaid = false;

            // 1. Regra Cross-Bill Pluggy: O pagamento da fatura N aparece registrado no payments[] da fatura N+1
            if (i + 1 < size) {
                com.finance.pluggy.domain.model.Invoice nextInvoice = invoices.get(i + 1);
                com.finance.pluggy.infrastructure.pluggy.dto.PluggyBillResponse nextBillDto = dtoMap.get(nextInvoice.getPluggyBillId());
                if (nextBillDto != null && nextBillDto.getPayments() != null && !nextBillDto.getPayments().isEmpty()) {
                    totalPaid = nextBillDto.getPayments().stream()
                            .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            }

            BigDecimal totalAmount = invoice.getTotalAmount();
            if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0 && totalPaid.compareTo(totalAmount) >= 0) {
                isPaid = true;
            }

            // 2. Match direto com transações de pagamento (CREDIT) na conta (resolve faturas fechadas sem fatura N+1 ainda gerada no Pluggy)
            if (!isPaid && isPaidByTransaction(invoice, account)) {
                isPaid = true;
            }

            String status = "OPEN";
            if (isPaid) {
                status = "PAID";
            } else if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(now)) {
                status = "OVERDUE";
            } else if (invoice.getCloseDate() != null && invoice.getCloseDate().isBefore(now)) {
                status = "CLOSED";
            }

            invoice.setStatus(status);
            invoiceRepository.save(invoice);
        }
    }

    private boolean isPaidByTransaction(com.finance.pluggy.domain.model.Invoice invoice, Account account) {
        LocalDate minDate = invoice.getCloseDate() != null
                ? invoice.getCloseDate()
                : (invoice.getDueDate() != null ? invoice.getDueDate().minusDays(10) : null);

        if (minDate == null) {
            return false;
        }

        List<Transaction> accountTxs = transactionRepository.findByAccountId(account.getId());
        if (accountTxs == null || accountTxs.isEmpty()) {
            return false;
        }

        for (Transaction tx : accountTxs) {
            if (tx.getType() == com.finance.pluggy.domain.model.TransactionType.CREDIT && tx.getDate() != null && !tx.getDate().isBefore(minDate)) {
                boolean isCategoryPayment = tx.getPluggyCategory() != null && tx.getPluggyCategory().equalsIgnoreCase("Credit card payment");
                boolean isDescPayment = tx.getDescription() != null && (
                        tx.getDescription().toLowerCase().contains("pagamento de fatura")
                        || tx.getDescription().toLowerCase().contains("pagamento de cartão")
                        || tx.getDescription().toLowerCase().contains("pagamento de cartao")
                        || tx.getDescription().toLowerCase().contains("pagto fatura")
                );

                if (isCategoryPayment || isDescPayment) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCreditCard(Account acc) {
        return acc.getType() == com.finance.pluggy.domain.model.AccountType.CREDIT 
                || acc.getSubtype() == com.finance.pluggy.domain.model.AccountSubtype.CREDIT_CARD;
    }
}
