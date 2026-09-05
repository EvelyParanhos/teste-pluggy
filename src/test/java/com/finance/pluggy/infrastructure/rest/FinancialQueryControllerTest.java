package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.repository.AccountRepository;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.domain.service.SyncService;
import com.finance.pluggy.infrastructure.pluggy.client.PluggyClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinancialQueryController.class)
class FinancialQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SyncService syncService;

    @MockBean
    private PluggyClient pluggyClient;

    @MockBean
    private ItemRepository itemRepository;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private SyncLogRepository syncLogRepository;

    @Test
    @DisplayName("Deve permitir disparar sync manual e consultar endpoints REST")
    void shouldAllowManualSyncAndQueries() throws Exception {
        mockMvc.perform(post("/api/v1/sync/item-123"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sync-logs"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pluggy/connect-token"))
                .andExpect(status().isOk());
    }
}
