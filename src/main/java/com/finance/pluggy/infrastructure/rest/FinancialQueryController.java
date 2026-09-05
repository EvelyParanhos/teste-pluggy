package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.model.Account;
import com.finance.pluggy.domain.model.Item;
import com.finance.pluggy.domain.model.SyncLog;
import com.finance.pluggy.domain.model.Transaction;
import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyConnectTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FinancialQueryController {

    private final SyncService syncService;
    private final PluggyClient pluggyClient;
    private final ItemRepository itemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SyncLogRepository syncLogRepository;

    /**
     * Gera um Connect Token (accessToken) para o widget do frontend do Pluggy.
     */
    @PostMapping("/pluggy/connect-token")
    public ResponseEntity<PluggyConnectTokenResponse> getConnectToken(
            @RequestParam(required = false) String itemId,
            @RequestBody(required = false) Map<String, Object> options) {
        return ResponseEntity.ok(pluggyClient.createConnectToken(itemId, options));
    }

    /**
     * Endpoint utilitário para disparar manualmente a sincronização de um Item (para testes).
     */
    @PostMapping("/sync/{itemId}")
    public ResponseEntity<Map<String, String>> manualSync(@PathVariable String itemId) {
        syncService.syncItem(itemId);
        return ResponseEntity.ok(Map.of(
                "status", "EXECUTED",
                "message", "Sincronização acionada para o item " + itemId
        ));
    }

    /**
     * Lista o histórico e status de logs de sincronização e retentativas (SyncLog).
     */
    @GetMapping("/sync-logs")
    public ResponseEntity<List<SyncLog>> getSyncLogs() {
        return ResponseEntity.ok(syncLogRepository.findAll());
    }

    /**
     * Lista todos os Items (conexões) gravados no banco de dados.
     */
    @GetMapping("/items")
    public ResponseEntity<List<Item>> getItems() {
        return ResponseEntity.ok(itemRepository.findAll());
    }

    /**
     * Lista todas as Contas salvas no banco de dados local.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    /**
     * Lista todas as Transações salvas com suas categorias internas resolvidas.
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }
}
