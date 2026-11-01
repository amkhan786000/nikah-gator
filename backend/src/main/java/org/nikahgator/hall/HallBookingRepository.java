package org.nikahgator.hall;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallBookingRepository extends JpaRepository<HallBooking, Long> {
    List<HallBooking> findByMasjidIdOrderByStartsAtAscIdAsc(Long masjidId);

    List<HallBooking> findByMasjidIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAscIdAsc(
            Long masjidId, LocalDateTime from, LocalDateTime to);

    Optional<HallBooking> findByIdAndMasjidId(Long id, Long masjidId);

    /** Confirmed bookings of the same masjid whose space matches and whose time range touches [from, to). */
    @Query("select b from HallBooking b where b.masjidId = :masjidId and b.status = 'CONFIRMED' "
            + "and lower(b.space) = lower(:space) and b.startsAt < :to and b.endsAt > :from")
    List<HallBooking> findConfirmedInRange(@Param("masjidId") Long masjidId, @Param("space") String space,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
