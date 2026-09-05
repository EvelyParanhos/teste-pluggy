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
        return resolveCategory(rawPluggyCategory, null);
    }

    /**
     * Resolve a categoria bruta fornecida pelo Pluggy e pela descrição para uma InternalCategory.
     */
    public InternalCategory resolveCategory(String rawPluggyCategory, String description) {
        if (rawPluggyCategory != null && !rawPluggyCategory.isBlank()) {
            Optional<CategoryMapping> mapping = categoryMappingRepository
                    .findByPluggyCategoryIgnoreCase(rawPluggyCategory.trim());

            if (mapping.isPresent()) {
                return mapping.get().getInternalCategory();
            }
        }

        // Concatena categoria e descrição para aumentar assertividade da inferência por palavra-chave
        String textToAnalyze = (rawPluggyCategory != null ? rawPluggyCategory : "") + " " + (description != null ? description : "");
        if (textToAnalyze.isBlank()) {
            return InternalCategory.OUTROS;
        }

        return fallbackCategoryInference(textToAnalyze.trim());
    }

    private InternalCategory fallbackCategoryInference(String text) {
        String lower = text.toLowerCase();
        
        // ALIMENTACAO
        if (lower.contains("food") || lower.contains("restauran") || lower.contains("refeicao") || lower.contains("aliment")
                || lower.contains("ifood") || lower.contains("mercado") || lower.contains("supermercado") || lower.contains("padaria")
                || lower.contains("atacad") || lower.contains("hortifruti") || lower.contains("mcdonald") || lower.contains("outback")
                || lower.contains("carrefour") || lower.contains("acucar") || lower.contains("extra") || lower.contains("dia%")) {
            return InternalCategory.ALIMENTACAO;
        }

        // TRANSPORTE
        if (lower.contains("transport") || lower.contains("uber") || lower.contains("99") || lower.contains("cabify")
                || lower.contains("combustivel") || lower.contains("gasolina") || lower.contains("etanol") || lower.contains("posto")
                || lower.contains("shell") || lower.contains("ipiranga") || lower.contains("bilhete") || lower.contains("estac")
                || lower.contains("pedagio") || lower.contains("azul") || lower.contains("gol") || lower.contains("latam")) {
            return InternalCategory.TRANSPORTE;
        }

        // MORADIA
        if (lower.contains("housing") || lower.contains("aluguel") || lower.contains("moradia") || lower.contains("luz")
                || lower.contains("agua") || lower.contains("energia") || lower.contains("enel") || lower.contains("sabesp")
                || lower.contains("condominio") || lower.contains("internet") || lower.contains("claro") || lower.contains("vivo")
                || lower.contains("tim")) {
            return InternalCategory.MORADIA;
        }

        // SALARIO
        if (lower.contains("salary") || lower.contains("salario") || lower.contains("renda") || lower.contains("provento")) {
            return InternalCategory.SALARIO;
        }

        // TRANSFERENCIA
        if (lower.contains("transfer") || lower.contains("pix") || lower.contains("ted") || lower.contains("doc")) {
            return InternalCategory.TRANSFERENCIA;
        }

        // SAUDE
        if (lower.contains("health") || lower.contains("saude") || lower.contains("farmacia") || lower.contains("drogaria")
                || lower.contains("drogasil") || lower.contains("raia") || lower.contains("medico") || lower.contains("hospital")) {
            return InternalCategory.SAUDE;
        }

        // INVESTIMENTO
        if (lower.contains("invest") || lower.contains("aplica") || lower.contains("sofisa") || lower.contains("nuinvest")
                || lower.contains("b3") || lower.contains("tesouro") || lower.contains("cdb") || lower.contains("fundo")
                || lower.contains("rico") || lower.contains("xp")) {
            return InternalCategory.INVESTIMENTO;
        }

        // LAZER
        if (lower.contains("leisure") || lower.contains("lazer") || lower.contains("entret") || lower.contains("cinema")
                || lower.contains("netflix") || lower.contains("spotify") || lower.contains("steam") || lower.contains("disney")
                || lower.contains("hbo") || lower.contains("prime")) {
            return InternalCategory.LAZER;
        }

        return InternalCategory.OUTROS;
    }
}
