package com.finance.pluggy.infrastructure.webhook.event;

import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.webhook.dto.PluggyWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluggyWebhookEventListener {

    private final SyncService syncService;

    @Async
    @EventListener
    public void onPluggyWebhookEvent(PluggyWebhookEvent event) {
        PluggyWebhookPayload payload = event.getPayload();
        log.info("Processando evento Webhook assincronamente em segundo plano: event={}, itemId={}",
                payload.getEvent(), payload.getItemId());

        try {
            switch (payload.getEvent()) {
                case "item/created":
                case "item/updated":
                case "transactions/created":
                    log.info("Evento '{}' recebido para itemId: {}. Disparando SyncService...", payload.getEvent(), payload.getItemId());
                    syncService.syncItem(payload.getItemId());
                    break;
                case "item/error":
                case "item/waiting_user_input":
                    log.warn("Item necessita de atenção do usuário. Status: {}, itemId: {}. Atualizando status do item...", payload.getEvent(), payload.getItemId());
                    syncService.syncItem(payload.getItemId());
                    break;
                default:
                    log.info("Evento Pluggy '{}' recebido para itemId: {}. Executando sincronização de atualização...", payload.getEvent(), payload.getItemId());
                    syncService.syncItem(payload.getItemId());
                    break;
            }
        } catch (Exception e) {
            log.error("Erro durante o processamento assíncrono da sincronização do Item {}: {}", payload.getItemId(), e.getMessage(), e);
        }
    }
}
