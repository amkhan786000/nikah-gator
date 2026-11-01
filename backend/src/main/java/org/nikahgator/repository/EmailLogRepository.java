package org.nikahgator.repository;

import java.time.LocalDateTime;

import org.nikahgator.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The e-mail log is append-only: the app only ever saves and reads it. */
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /** masjidId 0 means every masjid; kind and status "" mean any; recipient is a lower-case LIKE pattern. */
    @Query("""
            select e from EmailLog e
            where (:masjidId = 0L or e.masjidId = :masjidId)
              and e.createdAt >= :from and e.createdAt < :to
              and lower(e.recipient) like :recipient escape '\\'
              and (:kind = '' or e.kind = :kind)
              and (:status = '' or e.status = :status)
            order by e.createdAt desc, e.id desc""")
    Page<EmailLog> search(@Param("masjidId") long masjidId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("recipient") String recipient, @Param("kind") String kind, @Param("status") String status, Pageable page);
}
