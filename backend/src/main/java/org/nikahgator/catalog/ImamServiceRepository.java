package org.nikahgator.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImamServiceRepository extends JpaRepository<ImamService, Long> {
    List<ImamService> findByUserId(Long userId);

    Optional<ImamService> findByUserIdAndServiceKey(Long userId, String serviceKey);
}
