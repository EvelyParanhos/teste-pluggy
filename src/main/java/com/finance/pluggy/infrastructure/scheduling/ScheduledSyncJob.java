package com.finance.pluggy.infrastructure.scheduling;

import com.finance.pluggy.domain.model.Item;
import com.finance.pluggy.domain.model.SyncLog;
import com.finance.pluggy.domain.model.SyncLogStatus;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledSyncJob {

    private final SyncService syncService;
    private final SyncLogRepository syncLogRepository;
    private final ItemRepository itemRepository;

    /**
     * Job de Retentativas: Varre logs de sincronização pendentes cuja hora de retentativa já passou.
     * Executa a cada 1 minuto por padrão (configurável via pluggy.sync.retry-delay-ms).
     */
    @Scheduled(fixedDelayString = "${pluggy.sync.retry-delay-ms:60000}")
    public void processPendingRetries() {
        LocalDateTime now = LocalDateTime.now();
        List<SyncLog> pendingSyncs = syncLogRepository.findByStatusAndNextAttemptAtBefore(SyncLogStatus.PENDING, now);

        if (!pendingSyncs.isEmpty()) {
            log.info("Job de Retentativa: Encontrados {} itens pendentes para reprocessamento.", pendingSyncs.size());
            for (SyncLog syncLog : pendingSyncs) {
                log.info("Reprocessando retentativa (Tentativa {}) para o Item: {}", syncLog.getAttempts(), syncLog.getPluggyItemId());
                try {
                    syncService.syncItem(syncLog.getPluggyItemId());
                } catch (Exception e) {
                    log.error("Erro ao reprocessar item pendente {}: {}", syncLog.getPluggyItemId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Job de Rede de Segurança (Safety Net): Varre os itens cadastrados no sistema que não foram sincronizados recentemente
     * (ex: há mais de 12 horas) para cobrir eventuais webhooks perdidos ou instabilidades de rede.
     * Executa a cada 6 horas por padrão (configurável via pluggy.sync.safety-net-cron).
     */
    @Scheduled(cron = "${pluggy.sync.safety-net-cron:0 0 */6 * * *}")
    public void syncOutdatedItemsSafetyNet() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<Item> outdatedItems = itemRepository.findByLastUpdatedAtBeforeOrLastUpdatedAtIsNull(threshold);

        if (!outdatedItems.isEmpty()) {
            log.info("Rede de Segurança: Encontrados {} itens sem sincronização recente (mais de 12h).", outdatedItems.size());
            for (Item item : outdatedItems) {
                log.info("Rede de Segurança acionando sincronização para Item: {}", item.getPluggyItemId());
                try {
                    syncService.syncItem(item.getPluggyItemId());
                } catch (Exception e) {
                    log.error("Erro na rede de segurança para Item {}: {}", item.getPluggyItemId(), e.getMessage());
                }
            }
        }
    }
}
