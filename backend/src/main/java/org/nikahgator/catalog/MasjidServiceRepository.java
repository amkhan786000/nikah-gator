package org.nikahgator.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MasjidServiceRepository extends JpaRepository<MasjidService, Long> {
    List<MasjidService> findByMasjidId(Long masjidId);

    Optional<MasjidService> findByMasjidIdAndServiceKey(Long masjidId, String serviceKey);
}
