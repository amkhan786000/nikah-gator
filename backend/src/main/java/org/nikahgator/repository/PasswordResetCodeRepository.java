package org.nikahgator.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findFirstByUserIdAndUsedFalseOrderByIdDesc(Long userId);

    List<PasswordResetCode> findByUserIdAndUsedFalse(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
