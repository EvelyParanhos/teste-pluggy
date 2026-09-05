package com.finance.pluggy.domain;

import com.finance.pluggy.domain.model.CategoryMapping;
import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.repository.CategoryMappingRepository;
import com.finance.pluggy.domain.service.CategoryResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryResolutionServiceTest {

    @Mock
    private CategoryMappingRepository categoryMappingRepository;

    @InjectMocks
    private CategoryResolutionService categoryResolutionService;

    @Test
    @DisplayName("Deve retornar a categoria interna quando existir mapeamento cadastrado no repositório")
    void shouldReturnMappedCategoryWhenMappingExists() {
        CategoryMapping mapping = CategoryMapping.builder()
                .id(1L)
                .pluggyCategory("Food & Dining")
                .internalCategory(InternalCategory.ALIMENTACAO)
                .build();

        when(categoryMappingRepository.findByPluggyCategoryIgnoreCase("Food & Dining"))
                .thenReturn(Optional.of(mapping));

        InternalCategory resolved = categoryResolutionService.resolveCategory("Food & Dining");

        assertThat(resolved).isEqualTo(InternalCategory.ALIMENTACAO);
    }

    @Test
    @DisplayName("Deve usar inferência por palavra-chave se não houver mapeamento exato")
    void shouldInferCategoryWhenNoMappingExists() {
        when(categoryMappingRepository.findByPluggyCategoryIgnoreCase("Uber Ride"))
                .thenReturn(Optional.empty());

        InternalCategory resolved = categoryResolutionService.resolveCategory("Uber Ride");

        assertThat(resolved).isEqualTo(InternalCategory.TRANSPORTE);
    }

    @Test
    @DisplayName("Deve retornar OUTROS quando categoria for nula ou desconhecida")
    void shouldReturnOutrosWhenCategoryIsUnknownOrNull() {
        InternalCategory resolvedNull = categoryResolutionService.resolveCategory(null);
        InternalCategory resolvedUnknown = categoryResolutionService.resolveCategory("XYZ Unknown Category");

        assertThat(resolvedNull).isEqualTo(InternalCategory.OUTROS);
        assertThat(resolvedUnknown).isEqualTo(InternalCategory.OUTROS);
    }
}
