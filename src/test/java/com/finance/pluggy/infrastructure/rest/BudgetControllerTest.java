package com.finance.pluggy.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.pluggy.domain.model.CategoryBudget;
import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.repository.BudgetAlertLogRepository;
import com.finance.pluggy.domain.repository.CategoryBudgetRepository;
import com.finance.pluggy.domain.repository.TransactionRepository;
import com.finance.pluggy.infrastructure.rest.dto.CategoryBudgetRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryBudgetRepository categoryBudgetRepository;

    @MockBean
    private BudgetAlertLogRepository alertLogRepository;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Deve salvar novo limite de orçamento via POST /api/v1/budgets")
    void shouldSaveCategoryBudget() throws Exception {
        CategoryBudgetRequest request = CategoryBudgetRequest.builder()
                .category(InternalCategory.ALIMENTACAO)
                .monthlyLimit(new BigDecimal("1000.00"))
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        CategoryBudget savedBudget = CategoryBudget.builder()
                .id(1L)
                .category(InternalCategory.ALIMENTACAO)
                .monthlyLimit(new BigDecimal("1000.00"))
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        when(categoryBudgetRepository.findByCategory(InternalCategory.ALIMENTACAO)).thenReturn(Optional.empty());
        when(categoryBudgetRepository.save(any(CategoryBudget.class))).thenReturn(savedBudget);

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.category").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.monthlyLimit").value(1000.00));
    }

    @Test
    @DisplayName("Deve consultar status dos orçamentos via GET /api/v1/budgets")
    void shouldGetBudgetsStatus() throws Exception {
        CategoryBudget budget = CategoryBudget.builder()
                .id(1L)
                .category(InternalCategory.ALIMENTACAO)
                .monthlyLimit(new BigDecimal("1000.00"))
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        when(categoryBudgetRepository.findAll()).thenReturn(List.of(budget));
        when(transactionRepository.sumAmountByCategoryAndTypeAndDateBetween(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("850.00"));

        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("ALIMENTACAO"))
                .andExpect(jsonPath("$[0].currentSpent").value(850.00))
                .andExpect(jsonPath("$[0].percentageUsed").value(85.00))
                .andExpect(jsonPath("$[0].status").value("WARN"));
    }
}
