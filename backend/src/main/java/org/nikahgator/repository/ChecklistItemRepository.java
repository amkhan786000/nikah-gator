package org.nikahgator.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByRequestIdOrderBySortOrderAscIdAsc(Long requestId);

    Optional<ChecklistItem> findByIdAndRequestId(Long id, Long requestId);

    List<ChecklistItem> findByRequestIdIn(Collection<Long> requestIds);
}
