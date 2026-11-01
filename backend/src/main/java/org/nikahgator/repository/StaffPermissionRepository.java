package org.nikahgator.repository;

import java.util.List;

import org.nikahgator.entity.StaffPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Long> {
    List<StaffPermission> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
