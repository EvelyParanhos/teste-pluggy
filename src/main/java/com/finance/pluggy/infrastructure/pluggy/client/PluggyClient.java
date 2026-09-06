package com.finance.pluggy.infrastructure.pluggy.client;

import com.finance.pluggy.infrastructure.pluggy.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluggyClient {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final WebClient pluggyWebClient;
    private final PluggyAuthService authService;

    /**
     * Gera um Connect Token (accessToken) para inicializar o Widget Pluggy Connect no frontend.
     */
    public PluggyConnectTokenResponse createConnectToken(String itemId, Map<String, Object> options) {
        PluggyConnectTokenRequest request = PluggyConnectTokenRequest.builder()
                .itemId(itemId)
                .options(options)
                .build();

        return executeWithAuth(apiKey ->
                pluggyWebClient.post()
                        .uri("/connect_token")
                        .header(API_KEY_HEADER, apiKey)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(PluggyConnectTokenResponse.class)
                        .block()
        );
    }

    /**
     * Solicita a atualização em tempo real (on-demand update) de um Item enviando PATCH /items/{id}.
     */
    public PluggyItemResponse requestItemUpdate(String itemId) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.patch()
                        .uri("/items/{id}", itemId)
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(PluggyItemResponse.class)
                        .block()
        );
    }

    /**
     * Busca os detalhes de um Item (conexão).
     */
    public PluggyItemResponse getItem(String itemId) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.get()
                        .uri("/items/{id}", itemId)
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(PluggyItemResponse.class)
                        .block()
        );
    }

    /**
     * Busca todas as contas vinculadas a um Item.
     */
    public PluggyPageResponse<PluggyAccountResponse> getAccounts(String itemId) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/accounts")
                                .queryParam("itemId", itemId)
                                .build())
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<PluggyPageResponse<PluggyAccountResponse>>() {})
                        .block()
        );
    }

    /**
     * Busca detalhes de uma conta específica pelo ID.
     */
    public PluggyAccountResponse getAccount(String accountId) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.get()
                        .uri("/accounts/{id}", accountId)
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(PluggyAccountResponse.class)
                        .block()
        );
    }

    /**
     * Busca transações de uma conta utilizando a API v2 da Pluggy (/v2/transactions) com paginação por cursor (next).
     */
    public PluggyPageResponse<PluggyTransactionResponse> getTransactions(String accountId, LocalDate from, LocalDate to, String nextCursor) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.get()
                        .uri(uriBuilder -> {
                            var builder = uriBuilder.path("/v2/transactions")
                                    .queryParam("accountId", accountId);

                            if (from != null) {
                                builder.queryParam("from", from.toString());
                            }
                            if (to != null) {
                                builder.queryParam("to", to.toString());
                            }
                            if (nextCursor != null && !nextCursor.isBlank()) {
                                builder.queryParam("next", nextCursor);
                            }

                            return builder.build();
                        })
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<PluggyPageResponse<PluggyTransactionResponse>>() {})
                        .block()
        );
    }

    /**
     * Busca faturas (bills) de uma conta de cartão de crédito no Pluggy (/bills).
     */
    public PluggyPageResponse<PluggyBillResponse> getBills(String accountId) {
        return executeWithAuth(apiKey ->
                pluggyWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/bills")
                                .queryParam("accountId", accountId)
                                .build())
                        .header(API_KEY_HEADER, apiKey)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<PluggyPageResponse<PluggyBillResponse>>() {})
                        .block()
        );
    }

    /**
     * Executa a chamada à API injetando o cabeçalho X-API-KEY e tratando renovação automática em caso de 401.
     */
    private <R> R executeWithAuth(Function<String, R> apiCall) {
        String apiKey = authService.getApiKey();
        try {
            return apiCall.apply(apiKey);
        } catch (WebClientResponseException e) {
            if (Objects.equals(e.getStatusCode(), HttpStatusCode.valueOf(401))) {
                log.warn("Recebido 401 Unauthorized da Pluggy. Invalidadando API key e tentando novamente...");
                authService.invalidateApiKey();
                String newApiKey = authService.getApiKey();
                return apiCall.apply(newApiKey);
            }
            log.error("Erro na chamada à API Pluggy [status={}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
