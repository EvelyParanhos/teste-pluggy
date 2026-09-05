package com.finance.pluggy.domain.model;

import lombok.Getter;

@Getter
public enum InternalCategory {
    ALIMENTACAO("Alimentação e Restaurantes"),
    MORADIA("Moradia e Contas Domésticas"),
    TRANSPORTE("Transporte e Combustível"),
    LAZER("Lazer e Entretenimento"),
    SAUDE("Saúde e Cuidados Pessoais"),
    EDUCACAO("Educação"),
    COMPRAS("Compras e Vestuário"),
    SERVICOS("Serviços e Assinaturas"),
    SALARIO("Salário e Renda"),
    INVESTIMENTO("Investimentos e Aplicações"),
    TRANSFERENCIA("Transferências e Pix"),
    IMPOSTOS("Impostos e Taxas"),
    OUTROS("Outros / Não Categorizado");

    private final String description;

    InternalCategory(String description) {
        this.description = description;
    }
}
