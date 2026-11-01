package org.nikahgator.divorce;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DivorceRepository extends JpaRepository<Divorce, Long> {
    List<Divorce> findByMasjidIdOrderByDivorceDateDescIdDesc(Long masjidId);

    Optional<Divorce> findByIdAndMasjidId(Long id, Long masjidId);

    /** The non-void divorce of a nikah, if any (scoped to the masjid). */
    Optional<Divorce> findByNikahIdAndMasjidIdAndVoidedFalse(Long nikahId, Long masjidId);
}
