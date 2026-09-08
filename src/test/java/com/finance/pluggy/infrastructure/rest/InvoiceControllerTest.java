package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.service.InvoiceService;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Deve consultar faturas de cartão via GET /api/v1/invoices")
    void shouldGetInvoices() throws Exception {
        InvoiceResponse invoice = InvoiceResponse.builder()
                .accountId(1L)
                .accountName("Cartão Nubank")
                .maskedNumber("xxxx 1234")
                .status("OPEN")
                .currentBalance(new BigDecimal("1200.00"))
                .creditLimit(new BigDecimal("6000.00"))
                .availableCreditLimit(new BigDecimal("4800.00"))
                .utilizationPercentage(new BigDecimal("20.0"))
                .balanceCloseDate(LocalDate.now().plusDays(2))
                .balanceDueDate(LocalDate.now().plusDays(12))
                .transactionCount(3)
                .build();

        when(invoiceService.getInvoices()).thenReturn(List.of(invoice));

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountName").value("Cartão Nubank"))
                .andExpect(jsonPath("$[0].currentBalance").value(1200.00))
                .andExpect(jsonPath("$[0].creditLimit").value(6000.00))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("Deve consultar histórico de faturas via GET /api/v1/invoices/{accountId}/history")
    void shouldGetInvoiceHistory() throws Exception {
        com.finance.pluggy.infrastructure.rest.dto.InvoiceHistoryItem historyItem =
                com.finance.pluggy.infrastructure.rest.dto.InvoiceHistoryItem.builder()
                        .id(10L)
                        .closeDate(LocalDate.of(2026, 8, 28))
                        .dueDate(LocalDate.of(2026, 9, 5))
                        .status("PAID")
                        .totalAmount(new BigDecimal("1500.00"))
                        .transactions(List.of())
                        .build();

        when(invoiceService.getInvoiceHistory(1L)).thenReturn(List.of(historyItem));

        mockMvc.perform(get("/api/v1/invoices/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].totalAmount").value(1500.00));
    }
}
