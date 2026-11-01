package org.nikahgator.letters;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findByMasjidIdOrderByIdDesc(Long masjidId);

    Optional<Letter> findByIdAndMasjidId(Long id, Long masjidId);
}
