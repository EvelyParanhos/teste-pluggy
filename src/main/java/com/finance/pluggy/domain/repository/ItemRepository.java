package com.finance.pluggy.domain.repository;

import com.finance.pluggy.domain.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByPluggyItemId(String pluggyItemId);
    List<Item> findByLastUpdatedAtBeforeOrLastUpdatedAtIsNull(LocalDateTime threshold);
}
