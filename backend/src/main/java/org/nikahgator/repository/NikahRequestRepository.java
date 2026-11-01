package org.nikahgator.repository;

import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.NikahRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NikahRequestRepository extends JpaRepository<NikahRequest, Long> {
    List<NikahRequest> findByMasjidIdOrderByScheduledAtDescIdDesc(Long masjidId);

    Optional<NikahRequest> findByIdAndMasjidId(Long id, Long masjidId);
}
