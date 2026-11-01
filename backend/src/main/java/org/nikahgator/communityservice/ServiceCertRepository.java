package org.nikahgator.communityservice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCertRepository extends JpaRepository<ServiceCert, Long> {
    List<ServiceCert> findByMasjidIdOrderByIdDesc(Long masjidId);

    Optional<ServiceCert> findByIdAndMasjidId(Long id, Long masjidId);
}
