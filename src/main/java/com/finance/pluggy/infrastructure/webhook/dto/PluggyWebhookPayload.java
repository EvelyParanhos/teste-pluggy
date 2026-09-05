package com.finance.pluggy.infrastructure.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluggyWebhookPayload {

    @NotBlank(message = "O campo 'event' é obrigatório")
    private String event;

    private String id;

    private String eventId;

    @NotBlank(message = "O campo 'itemId' é obrigatório")
    private String itemId;

    private Object error;

    /**
     * Retorna o ID do evento (priorizando 'id' e caindo de volta em 'eventId').
     */
    public String getResolvedEventId() {
        if (id != null && !id.isBlank()) {
            return id;
        }
        return eventId;
    }
}
