package com.finance.pluggy.infrastructure.pluggy;

import com.finance.pluggy.infrastructure.pluggy.client.PluggyAuthService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyPageResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluggyClientTest {

    private MockWebServer mockWebServer;
    private PluggyClient pluggyClient;

    @Mock
    private PluggyAuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        pluggyClient = new PluggyClient(webClient, authService);

        lenient().when(authService.getApiKey()).thenReturn("test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Deve buscar o Item com o cabeçalho X-API-KEY correto")
    void shouldGetItemWithAuthHeader() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"item-123\",\"status\":\"UPDATED\"}"));

        PluggyItemResponse item = pluggyClient.getItem("item-123");

        assertThat(item).isNotNull();
        assertThat(item.getId()).isEqualTo("item-123");
        assertThat(item.getStatus()).isEqualTo("UPDATED");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/items/item-123");
        assertThat(request.getHeader("X-API-KEY")).isEqualTo("test-api-key");
    }

    @Test
    @DisplayName("Deve buscar as contas filtrando por itemId")
    void shouldGetAccountsByItemId() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"page\":1,\"total\":1,\"results\":[{\"id\":\"acc-456\",\"itemId\":\"item-123\",\"name\":\"Conta Corrente\",\"balance\":1500.50}]}"));

        PluggyPageResponse<PluggyAccountResponse> response = pluggyClient.getAccounts("item-123");

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getId()).isEqualTo("acc-456");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/accounts?itemId=item-123");
        assertThat(request.getHeader("X-API-KEY")).isEqualTo("test-api-key");
    }

    @Test
    @DisplayName("Deve buscar transações no endpoint /v2/transactions com filtros de data e cursor")
    void shouldGetTransactionsWithFilters() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"results\":[{\"id\":\"tx-789\",\"accountId\":\"acc-456\",\"description\":\"Supermercado\",\"amount\":120.00}]}"));

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        PluggyPageResponse<PluggyTransactionResponse> response = pluggyClient.getTransactions("acc-456", from, to, null);

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getDescription()).isEqualTo("Supermercado");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v2/transactions?accountId=acc-456&from=2026-01-01&to=2026-01-31");
        assertThat(request.getHeader("X-API-KEY")).isEqualTo("test-api-key");
    }

    @Test
    @DisplayName("Deve invalidar chave e tentar novamente ao receber 401 Unauthorized")
    void shouldRetryOn401Unauthorized() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"item-123\",\"status\":\"UPDATED\"}"));

        when(authService.getApiKey()).thenReturn("expired-key").thenReturn("new-valid-key");

        PluggyItemResponse item = pluggyClient.getItem("item-123");

        assertThat(item).isNotNull();
        assertThat(item.getId()).isEqualTo("item-123");

        verify(authService, times(1)).invalidateApiKey();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve solicitar atualização em tempo real de Item via PATCH /items/{id}")
    void shouldSendPatchToRequestItemUpdate() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"item-123\",\"status\":\"UPDATING\"}"));

        PluggyItemResponse item = pluggyClient.requestItemUpdate("item-123");

        assertThat(item).isNotNull();
        assertThat(item.getId()).isEqualTo("item-123");
        assertThat(item.getStatus()).isEqualTo("UPDATING");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getPath()).isEqualTo("/items/item-123");
        assertThat(request.getHeader("X-API-KEY")).isEqualTo("test-api-key");
    }
}
