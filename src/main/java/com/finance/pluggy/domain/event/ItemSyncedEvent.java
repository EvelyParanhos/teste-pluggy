package com.finance.pluggy.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class ItemSyncedEvent extends ApplicationEvent {

    private final Long itemId;
    private final String pluggyItemId;
    private final int syncedAccountsCount;
    private final int syncedTransactionsCount;
    private final LocalDateTime syncedAt;

    public ItemSyncedEvent(Object source, Long itemId, String pluggyItemId, int syncedAccountsCount, int syncedTransactionsCount) {
        super(source);
        this.itemId = itemId;
        this.pluggyItemId = pluggyItemId;
        this.syncedAccountsCount = syncedAccountsCount;
        this.syncedTransactionsCount = syncedTransactionsCount;
        this.syncedAt = LocalDateTime.now();
    }
}
