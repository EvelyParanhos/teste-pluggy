package com.finance.pluggy.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.pluggy.infrastructure.webhook.dto.PluggyWebhookPayload;
import com.finance.pluggy.infrastructure.webhook.event.PluggyWebhookEvent;
import com.finance.pluggy.infrastructure.webhook.event.PluggyWebhookEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PluggyWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PluggyWebhookEventListener eventListener;

    @Test
    @DisplayName("Deve aceitar payload válido de Webhook do Pluggy e responder 200 OK imediatamente")
    void shouldAcceptValidWebhookAndRespondImmediately() throws Exception {
        PluggyWebhookPayload payload = PluggyWebhookPayload.builder()
                .event("item/updated")
                .id("evt-12345")
                .itemId("item-67890")
                .build();

        mockMvc.perform(post("/api/v1/webhooks/pluggy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.event").value("item/updated"))
                .andExpect(jsonPath("$.itemId").value("item-67890"));

        // Valida que o listener assíncrono é acionado sem bloquear o controller HTTP
        verify(eventListener, timeout(2000)).onPluggyWebhookEvent(any(PluggyWebhookEvent.class));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se campos obrigatórios (event ou itemId) estiverem ausentes")
    void shouldReturn400OnInvalidPayload() throws Exception {
        PluggyWebhookPayload invalidPayload = PluggyWebhookPayload.builder()
                .event("")
                .itemId(null)
                .build();

        mockMvc.perform(post("/api/v1/webhooks/pluggy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }
}
