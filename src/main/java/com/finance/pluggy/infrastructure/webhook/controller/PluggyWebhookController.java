package com.finance.pluggy.infrastructure.webhook.controller;

import com.finance.pluggy.infrastructure.webhook.dto.PluggyWebhookPayload;
import com.finance.pluggy.infrastructure.webhook.event.PluggyWebhookEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class PluggyWebhookController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/pluggy")
    public ResponseEntity<Map<String, String>> handlePluggyWebhook(@Valid @RequestBody PluggyWebhookPayload payload) {
        log.info("Webhook Pluggy recebido: event={}, itemId={}, eventId={}",
                payload.getEvent(), payload.getItemId(), payload.getResolvedEventId());

        // Enfileira o processamento assincronamente publicando evento no Spring
        eventPublisher.publishEvent(new PluggyWebhookEvent(this, payload));

        // Responde 200 OK / 202 Accepted imediatamente sem bloquear a chamada do Pluggy
        return ResponseEntity.ok(Map.of(
                "status", "RECEIVED",
                "event", payload.getEvent(),
                "itemId", payload.getItemId()
        ));
    }
}
