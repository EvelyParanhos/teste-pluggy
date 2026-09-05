package com.finance.pluggy.infrastructure.webhook.event;

import com.finance.pluggy.infrastructure.webhook.dto.PluggyWebhookPayload;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PluggyWebhookEvent extends ApplicationEvent {

    private final PluggyWebhookPayload payload;

    public PluggyWebhookEvent(Object source, PluggyWebhookPayload payload) {
        super(source);
        this.payload = payload;
    }
}
