package org.nikahgator.lifeevent;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeEventRepository extends JpaRepository<LifeEvent, Long> {
    List<LifeEvent> findByMasjidIdAndEventTypeOrderByEventDateDescIdDesc(Long masjidId, LifeEventType type);

    Optional<LifeEvent> findByIdAndMasjidIdAndEventType(Long id, Long masjidId, LifeEventType type);
}
