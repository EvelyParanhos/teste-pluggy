package com.finance.pluggy.domain.event;

import com.finance.pluggy.domain.model.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
public class NewTransactionsIngestedEvent extends ApplicationEvent {

    private final Long itemId;
    private final String pluggyItemId;
    private final List<Transaction> transactions;
    private final LocalDateTime ingestedAt;

    public NewTransactionsIngestedEvent(Object source, Long itemId, String pluggyItemId, List<Transaction> transactions) {
        super(source);
        this.itemId = itemId;
        this.pluggyItemId = pluggyItemId;
        this.transactions = transactions != null ? List.copyOf(transactions) : Collections.emptyList();
        this.ingestedAt = LocalDateTime.now();
    }
}
