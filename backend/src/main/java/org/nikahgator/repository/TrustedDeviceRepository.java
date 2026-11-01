package org.nikahgator.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.nikahgator.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    Optional<TrustedDevice> findByTokenHash(String tokenHash);

    @Transactional
    long deleteByUserId(Long userId);

    @Transactional
    long deleteByExpiresAtBefore(LocalDateTime time);
}
