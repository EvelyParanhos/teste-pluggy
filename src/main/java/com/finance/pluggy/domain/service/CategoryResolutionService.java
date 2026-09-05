package com.finance.pluggy.domain.service;

import com.finance.pluggy.domain.model.CategoryMapping;
import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.repository.CategoryMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryResolutionService {

    private final CategoryMappingRepository categoryMappingRepository;

    /**
     * Resolve a categoria bruta fornecida pelo Pluggy para uma InternalCategory.
     * Se houver um mapeamento cadastrado no banco, utiliza a categoria mapeada.
     * Caso contrário, faz um fallback para a categoria OUTROS.
     *
     * @param rawPluggyCategory Nome/ID da categoria original Pluggy
     * @return InternalCategory resolvida
     */
    public InternalCategory resolveCategory(String rawPluggyCategory) {
        if (rawPluggyCategory == null || rawPluggyCategory.isBlank()) {
            return InternalCategory.OUTROS;
        }

        Optional<CategoryMapping> mapping = categoryMappingRepository
                .findByPluggyCategoryIgnoreCase(rawPluggyCategory.trim());

        if (mapping.isPresent()) {
            return mapping.get().getInternalCategory();
        }

        // Tenta uma inferência básica baseada em palavras-chave se o mapeamento exato não estiver cadastrado
        return fallbackCategoryInference(rawPluggyCategory.trim());
    }

    private InternalCategory fallbackCategoryInference(String category) {
        String lower = category.toLowerCase();
        if (lower.contains("food") || lower.contains("restauran") || lower.contains("refeicao") || lower.contains("aliment")) {
            return InternalCategory.ALIMENTACAO;
        }
        if (lower.contains("transport") || lower.contains("uber") || lower.contains("combustivel") || lower.contains("gasolina")) {
            return InternalCategory.TRANSPORTE;
        }
        if (lower.contains("housing") || lower.contains("aluguel") || lower.contains("moradia") || lower.contains("luz") || lower.contains("agua")) {
            return InternalCategory.MORADIA;
        }
        if (lower.contains("salary") || lower.contains("salario") || lower.contains("renda")) {
            return InternalCategory.SALARIO;
        }
        if (lower.contains("transfer") || lower.contains("pix")) {
            return InternalCategory.TRANSFERENCIA;
        }
        if (lower.contains("health") || lower.contains("saude") || lower.contains("farmacia")) {
            return InternalCategory.SAUDE;
        }
        if (lower.contains("invest") || lower.contains("aplica")) {
            return InternalCategory.INVESTIMENTO;
        }
        if (lower.contains("leisure") || lower.contains("lazer") || lower.contains("entret")) {
            return InternalCategory.LAZER;
        }

        return InternalCategory.OUTROS;
    }
}
