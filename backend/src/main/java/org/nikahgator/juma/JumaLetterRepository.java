package org.nikahgator.juma;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JumaLetterRepository extends JpaRepository<JumaLetter, Long> {
    List<JumaLetter> findByMasjidIdOrderByIdDesc(Long masjidId);

    Optional<JumaLetter> findByIdAndMasjidId(Long id, Long masjidId);
}
