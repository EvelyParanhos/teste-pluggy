package com.finance.pluggy.domain.event;

import com.finance.pluggy.domain.service.BudgetRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewTransactionsEventListener {

    private final BudgetRuleService budgetRuleService;

    @EventListener
    public void onNewTransactionsIngested(NewTransactionsIngestedEvent event) {
        log.info("Recebidas {} transações no evento pós-ingestão para o Item ID: {} (Pluggy ID: {})",
                event.getTransactions().size(), event.getItemId(), event.getPluggyItemId());

        event.getTransactions().forEach(tx ->
                log.info("   -> Transação Ingerida: ID={}, Descrição='{}', Valor={}, Categoria={}",
                        tx.getId(), tx.getDescription(), tx.getAmount(), tx.getInternalCategory())
        );

        // Executa a avaliação do motor de regras de orçamento por categoria
        try {
            budgetRuleService.evaluateTransactions(event.getTransactions());
        } catch (Exception e) {
            log.error("Erro ao avaliar regras de orçamento para transações pós-ingestão: {}", e.getMessage(), e);
        }
    }
}
