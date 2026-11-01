package org.nikahgator.counseling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselingRepository extends JpaRepository<CounselingSession, Long> {
    List<CounselingSession> findByMasjidIdOrderBySessionAtDescIdDesc(Long masjidId);

    Optional<CounselingSession> findByIdAndMasjidId(Long id, Long masjidId);

    List<CounselingSession> findByMasjidIdAndFollowUpOnGreaterThanEqualAndStatusNotOrderByFollowUpOnAsc(
            Long masjidId, LocalDate from, String status);
}
