package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.SyncLog;
import com.finance.pluggy.domain.model.SyncLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    Optional<SyncLog> findByPluggyItemId(String pluggyItemId);
    List<SyncLog> findByStatusAndNextAttemptAtBefore(SyncLogStatus status, LocalDateTime now);
}
