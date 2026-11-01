package org.nikahgator.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.NikahRequest;
import org.springframework.data.repository.Repository;

/** Read-only nikah lookups for the reminder feature. */
public interface NikahReminderQueries extends Repository<NikahRequest, Long> {
    Optional<NikahRequest> findById(Long id);

    Optional<NikahRequest> findByIdAndMasjidId(Long id, Long masjidId);

    List<NikahRequest> findByMasjidIdAndStatusAndScheduledAtAfter(Long masjidId, String status, LocalDateTime after);
}
