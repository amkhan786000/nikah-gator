package org.nikahgator.repository;

import java.util.Optional;

import org.nikahgator.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    long countByRole(String role);

    Optional<AppUser> findFirstByMasjidIdAndRole(Long masjidId, String role);

    java.util.List<AppUser> findByMasjidIdAndRoleOrderByFullName(Long masjidId, String role);
}
