package org.nikahgator.salah;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalahPeriodRepository extends JpaRepository<SalahPeriod, Long> {
    List<SalahPeriod> findByMasjidIdOrderByStartDate(Long masjidId);

    Optional<SalahPeriod> findByIdAndMasjidId(Long id, Long masjidId);

    /** Periods that share at least one day with [from, to]. */
    List<SalahPeriod> findByMasjidIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDate(
            Long masjidId, LocalDate from, LocalDate to);
}
