package com.finance.pluggy.infrastructure.pluggy.client;

import com.finance.pluggy.infrastructure.pluggy.config.PluggyProperties;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAuthRequest;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluggyAuthService {

    private static final long DEFAULT_TOKEN_TTL_SECONDS = 7200; // 2 horas
    private static final long RENEWAL_THRESHOLD_SECONDS = 600;  // 10 minutos de margem antes de expirar

    private final WebClient pluggyWebClient;
    private final PluggyProperties properties;

    private String cachedApiKey;
    private Instant expiresAt;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Retorna a API key válida em cache ou efetua a autenticação com a Pluggy se estiver expirada/ausente.
     */
    public String getApiKey() {
        if (isTokenValid()) {
            return cachedApiKey;
        }

        lock.lock();
        try {
            // Re-verifica após adquirir o lock para evitar chamadas concorrentes duplicadas
            if (isTokenValid()) {
                return cachedApiKey;
            }

            log.info("Obtendo nova API Key do Pluggy (autenticação)...");
            PluggyAuthRequest request = PluggyAuthRequest.builder()
                    .clientId(properties.getClientId())
                    .clientSecret(properties.getClientSecret())
                    .build();

            PluggyAuthResponse response = pluggyWebClient.post()
                    .uri("/auth")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PluggyAuthResponse.class)
                    .block();

            if (response == null || response.getApiKey() == null || response.getApiKey().isBlank()) {
                throw new IllegalStateException("Resposta inválida ao autenticar com a API da Pluggy");
            }

            this.cachedApiKey = response.getApiKey();
            this.expiresAt = Instant.now().plusSeconds(DEFAULT_TOKEN_TTL_SECONDS);

            log.info("API Key obtida com sucesso e armazenada em cache até {}", expiresAt);
            return cachedApiKey;
        } catch (Exception e) {
            log.error("Erro ao autenticar na API Pluggy: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na autenticação com a API Pluggy", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Invalida o token em cache (útil se receber um HTTP 401 durante a execução).
     */
    public void invalidateApiKey() {
        lock.lock();
        try {
            log.warn("Invalidando API Key em cache...");
            this.cachedApiKey = null;
            this.expiresAt = null;
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenValid() {
        if (cachedApiKey == null || expiresAt == null) {
            return false;
        }
        // Se a hora atual já passou do ponto de renovação (expirações menos a margem de 10 min)
        return Instant.now().isBefore(expiresAt.minusSeconds(RENEWAL_THRESHOLD_SECONDS));
    }
}
