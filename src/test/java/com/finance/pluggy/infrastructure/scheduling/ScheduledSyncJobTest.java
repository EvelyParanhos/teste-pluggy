package com.finance.pluggy.infrastructure.scheduling;

import com.finance.pluggy.domain.model.Item;
import com.finance.pluggy.domain.model.SyncLog;
import com.finance.pluggy.domain.model.SyncLogStatus;
import com.finance.pluggy.domain.repository.ItemRepository;
import com.finance.pluggy.domain.repository.SyncLogRepository;
import com.finance.pluggy.domain.service.SyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledSyncJobTest {

    @Mock
    private SyncService syncService;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ScheduledSyncJob scheduledSyncJob;

    @Test
    @DisplayName("Deve reprocessar itens pendentes de retentativa")
    void shouldProcessPendingRetries() {
        SyncLog pendingLog = SyncLog.builder()
                .id(1L)
                .pluggyItemId("item-retry-100")
                .status(SyncLogStatus.PENDING)
                .attempts(1)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(syncLogRepository.findByStatusAndNextAttemptAtBefore(eq(SyncLogStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingLog));

        scheduledSyncJob.processPendingRetries();

        verify(syncService, times(1)).syncItem("item-retry-100");
    }

    @Test
    @DisplayName("Deve varrer e reprocessar itens desatualizados na rede de segurança")
    void shouldSyncOutdatedItemsInSafetyNet() {
        Item outdatedItem = Item.builder()
                .id(2L)
                .pluggyItemId("item-outdated-200")
                .lastUpdatedAt(LocalDateTime.now().minusHours(24))
                .build();

        when(itemRepository.findByLastUpdatedAtBeforeOrLastUpdatedAtIsNull(any(LocalDateTime.class)))
                .thenReturn(List.of(outdatedItem));

        scheduledSyncJob.syncOutdatedItemsSafetyNet();

        verify(syncService, times(1)).syncItem("item-outdated-200");
    }
}
