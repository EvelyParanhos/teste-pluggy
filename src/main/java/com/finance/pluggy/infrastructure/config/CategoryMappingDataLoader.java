package com.finance.pluggy.infrastructure.config;

import com.finance.pluggy.domain.model.CategoryMapping;
import com.finance.pluggy.domain.model.InternalCategory;
import com.finance.pluggy.domain.repository.CategoryMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryMappingDataLoader implements CommandLineRunner {

    private final CategoryMappingRepository categoryMappingRepository;

    @Override
    public void run(String... args) {
        if (categoryMappingRepository.count() > 0) {
            log.info("Mapeamentos de categorias Pluggy já inicializados (total: {}).", categoryMappingRepository.count());
            return;
        }

        log.info("Inicializando mapeamentos padrão de categorias oficiais da Pluggy...");

        List<CategoryMapping> initialMappings = List.of(
                // Alimentação
                map("Food and Drink", InternalCategory.ALIMENTACAO),
                map("03000000", InternalCategory.ALIMENTACAO),
                map("Restaurant", InternalCategory.ALIMENTACAO),
                map("03000003", InternalCategory.ALIMENTACAO),
                map("Delivery Services", InternalCategory.ALIMENTACAO),
                map("03000005", InternalCategory.ALIMENTACAO),
                map("Bar", InternalCategory.ALIMENTACAO),
                map("Café", InternalCategory.ALIMENTACAO),
                map("Groceries", InternalCategory.ALIMENTACAO),
                map("08000002", InternalCategory.ALIMENTACAO),

                // Moradia & Contas
                map("Rent", InternalCategory.MORADIA),
                map("06000003", InternalCategory.MORADIA),

                // Transporte
                map("Automotive Services", InternalCategory.TRANSPORTE),
                map("Automotive", InternalCategory.TRANSPORTE),

                // Saúde
                map("Healthcare", InternalCategory.SAUDE),
                map("04000000", InternalCategory.SAUDE),
                map("Pharmacy", InternalCategory.SAUDE),
                map("04000001", InternalCategory.SAUDE),
                map("Gyms and Fitness Centers", InternalCategory.SAUDE),
                map("12000001", InternalCategory.SAUDE),

                // Lazer & Entretenimento
                map("Entertainment", InternalCategory.LAZER),
                map("Online Subscriptions", InternalCategory.LAZER),
                map("07000001", InternalCategory.LAZER),
                map("Recreation", InternalCategory.LAZER),
                map("12000000", InternalCategory.LAZER),
                map("Travel", InternalCategory.LAZER),
                map("09000000", InternalCategory.LAZER),

                // Compras
                map("Shop", InternalCategory.COMPRAS),
                map("08000000", InternalCategory.COMPRAS),
                map("Online Shopping", InternalCategory.COMPRAS),
                map("08000001", InternalCategory.COMPRAS),

                // Salário & Renda
                map("Payroll", InternalCategory.SALARIO),
                map("01000006", InternalCategory.SALARIO),

                // Transferências & Pix
                map("Transfer", InternalCategory.TRANSFERENCIA),
                map("01000000", InternalCategory.TRANSFERENCIA),

                // Serviços & Tarifas
                map("Services", InternalCategory.SERVICOS),
                map("07000000", InternalCategory.SERVICOS),
                map("Telecommunication Services", InternalCategory.SERVICOS),
                map("07000002", InternalCategory.SERVICOS),
                map("Bank Fees", InternalCategory.SERVICOS),
                map("02000000", InternalCategory.SERVICOS),

                // Impostos & Taxas
                map("Taxes", InternalCategory.IMPOSTOS),
                map("10000000", InternalCategory.IMPOSTOS),

                // Investimento
                map("Investment", InternalCategory.INVESTIMENTO),
                map("11000000", InternalCategory.INVESTIMENTO),
                map("Mutual Fund", InternalCategory.INVESTIMENTO),
                map("11000001", InternalCategory.INVESTIMENTO)
        );

        for (CategoryMapping mapping : initialMappings) {
            if (categoryMappingRepository.findByPluggyCategoryIgnoreCase(mapping.getPluggyCategory()).isEmpty()) {
                categoryMappingRepository.save(mapping);
            }
        }

        log.info("Carga inicial de mapeamentos de categorias concluída com sucesso (total inserido: {}).", categoryMappingRepository.count());
    }

    private CategoryMapping map(String category, InternalCategory internal) {
        return CategoryMapping.builder()
                .pluggyCategory(category)
                .internalCategory(internal)
                .build();
    }
}
