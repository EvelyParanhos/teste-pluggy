package com.finance.pluggy.infrastructure.pluggy;

import com.finance.pluggy.infrastructure.pluggy.client.PluggyAuthService;
import com.finance.pluggy.infrastructure.pluggy.config.PluggyProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluggyAuthServiceTest {

    private MockWebServer mockWebServer;
    private PluggyAuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        PluggyProperties properties = new PluggyProperties();
        properties.setClientId("dummy-client-id");
        properties.setClientSecret("dummy-client-secret");
        properties.setBaseUrl(mockWebServer.url("/").toString());

        authService = new PluggyAuthService(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Deve buscar nova API key e reaproveitar do cache nas chamadas subsequentes")
    void shouldFetchApiKeyAndReuseCache() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"apiKey\":\"secret-api-key-123\"}"));

        String apiKey1 = authService.getApiKey();
        String apiKey2 = authService.getApiKey();

        assertThat(apiKey1).isEqualTo("secret-api-key-123");
        assertThat(apiKey2).isEqualTo("secret-api-key-123");

        // Apenas 1 requisição HTTP deve ter sido feita ao servidor
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/auth");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).contains("dummy-client-id");
    }

    @Test
    @DisplayName("Deve buscar nova chave se o cache for invalidado")
    void shouldRefetchApiKeyWhenInvalidated() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"apiKey\":\"key-1\"}"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"apiKey\":\"key-2\"}"));

        String key1 = authService.getApiKey();
        assertThat(key1).isEqualTo("key-1");

        authService.invalidateApiKey();

        String key2 = authService.getApiKey();
        assertThat(key2).isEqualTo("key-2");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve lançar exceção se a chamada de autenticação falhar")
    void shouldThrowExceptionOnAuthFailure() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"message\":\"Unauthorized\"}"));

        assertThatThrownBy(() -> authService.getApiKey())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na autenticação com a API Pluggy");
    }
}
