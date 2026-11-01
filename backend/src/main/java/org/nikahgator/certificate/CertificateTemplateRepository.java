package org.nikahgator.certificate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
    Optional<CertificateTemplate> findByMasjidId(Long masjidId);
}
