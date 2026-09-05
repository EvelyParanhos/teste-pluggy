package com.finance.pluggy.domain.mapper;

import com.finance.pluggy.domain.model.*;
import com.finance.pluggy.domain.service.CategoryResolutionService;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyAccountResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyItemResponse;
import com.finance.pluggy.infrastructure.pluggy.dto.PluggyTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PluggyDomainMapper {

    private final CategoryResolutionService categoryResolutionService;

    /**
     * Mapeia um PluggyItemResponse para uma entidade Item (criando nova ou atualizando existente).
     */
    public Item toItemEntity(PluggyItemResponse dto, Item target) {
        Item item = target != null ? target : Item.builder().pluggyItemId(dto.getId()).build();

        if (dto.getConnector() != null) {
            item.setConnectorId(dto.getConnector().getId());
            item.setConnectorName(dto.getConnector().getName());
        }

        item.setStatus(parseItemStatus(dto.getStatus()));
        item.setLastUpdatedAt(dto.getLastUpdatedAt() != null ? dto.getLastUpdatedAt() : LocalDateTime.now());

        return item;
    }

    /**
     * Mapeia um PluggyAccountResponse para uma entidade Account (criando nova ou atualizando existente).
     */
    public Account toAccountEntity(PluggyAccountResponse dto, Item item, Account target) {
        Account account = target != null ? target : Account.builder().pluggyAccountId(dto.getId()).build();

        account.setItem(item);
        account.setName(dto.getName() != null ? dto.getName() : "Conta Pluggy");
        account.setMarketingName(dto.getMarketingName());
        account.setNumber(dto.getNumber());
        account.setAgency(dto.getAgency());
        account.setType(parseAccountType(dto.getType()));
        account.setSubtype(parseAccountSubtype(dto.getSubtype()));
        account.setBalance(dto.getBalance());
        account.setCurrencyCode(dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "BRL");

        return account;
    }

    /**
     * Mapeia um PluggyTransactionResponse para uma entidade Transaction (criando nova ou atualizando existente),
     * aplicando o CategoryMapping para resolver a categoria interna.
     */
    public Transaction toTransactionEntity(PluggyTransactionResponse dto, Account account, Transaction target) {
        Transaction transaction = target != null ? target : Transaction.builder().pluggyTransactionId(dto.getId()).build();

        transaction.setAccount(account);
        transaction.setDescription(dto.getDescription() != null ? dto.getDescription() : "Transação sem descrição");
        transaction.setRawDescription(dto.getRawDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setDate(parseDate(dto.getDate()));
        transaction.setType(parseTransactionType(dto.getType()));
        transaction.setStatus(parseTransactionStatus(dto.getStatus()));
        transaction.setPluggyCategory(dto.getCategory());

        // Resolução centralizada da categoria interna
        InternalCategory internalCategory = categoryResolutionService.resolveCategory(dto.getCategory());
        transaction.setInternalCategory(internalCategory);

        return transaction;
    }

    private ItemStatus parseItemStatus(String status) {
        if (status == null) return ItemStatus.OUT_OF_DATE;
        try {
            return ItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ItemStatus.OUT_OF_DATE;
        }
    }

    private AccountType parseAccountType(String type) {
        if (type == null) return AccountType.BANK;
        try {
            return AccountType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AccountType.BANK;
        }
    }

    private AccountSubtype parseAccountSubtype(String subtype) {
        if (subtype == null) return AccountSubtype.OTHER;
        try {
            return AccountSubtype.valueOf(subtype.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AccountSubtype.OTHER;
        }
    }

    private TransactionType parseTransactionType(String type) {
        if (type == null) return TransactionType.DEBIT;
        try {
            return TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TransactionType.DEBIT;
        }
    }

    private TransactionStatus parseTransactionStatus(String status) {
        if (status == null) return TransactionStatus.POSTED;
        try {
            return TransactionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TransactionStatus.POSTED;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            if (dateStr.length() >= 10) {
                return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            }
            return LocalDate.now();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
