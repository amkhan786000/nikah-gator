package org.nikahgator.repository;

import java.util.Optional;

import org.nikahgator.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findFirstByRequestIdAndMasjidIdOrderByIdDesc(Long requestId, Long masjidId);

    Optional<Certificate> findByRequestIdAndMasjidIdAndStatus(Long requestId, Long masjidId, String status);

    Optional<Certificate> findByCertNumber(String certNumber);
}
