package org.nikahgator.repository;

import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.LoginOtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginOtpRepository extends JpaRepository<LoginOtp, Long> {
    Optional<LoginOtp> findFirstByUserIdAndUsedFalseOrderByIdDesc(Long userId);

    List<LoginOtp> findByUserIdAndUsedFalse(Long userId);
}
