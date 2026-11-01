package org.nikahgator.repository;

import org.nikahgator.entity.Masjid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasjidRepository extends JpaRepository<Masjid, Long> {
    boolean existsBySlug(String slug);
}
