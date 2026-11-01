package org.nikahgator.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.nikahgator.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRequestIdAndMasjidIdOrderBySendAtAscIdAsc(Long requestId, Long masjidId);

    List<Notification> findByRequestId(Long requestId);

    List<Notification> findTop50ByChannelAndStatusAndSendAtLessThanEqualOrderBySendAtAsc(String channel, String status, LocalDateTime now);

    /** Atomically takes a pending row; 1 = this caller owns it and must send it, 0 = somebody else got it. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.status = 'SENDING', n.attempts = n.attempts + 1, n.claimedAt = :now "
            + "where n.id = :id and n.status = 'PENDING'")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** Rows stuck in SENDING (the server stopped mid-send) go back to PENDING. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.status = 'PENDING' where n.status = 'SENDING' and n.claimedAt < :cutoff")
    int releaseStale(@Param("cutoff") LocalDateTime cutoff);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.status = 'CANCELLED' where n.id = :id and n.masjidId = :masjidId and n.status in ('PENDING', 'FAILED')")
    int cancel(@Param("id") Long id, @Param("masjidId") Long masjidId);
}
