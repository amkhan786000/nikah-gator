package org.nikahgator.repository;

import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByRequestIdAndMasjidIdOrderByPaidAtDescIdDesc(Long requestId, Long masjidId);

    List<Payment> findByMasjidIdAndStatus(Long masjidId, String status);

    Optional<Payment> findByIdAndRequestIdAndMasjidId(Long id, Long requestId, Long masjidId);

    /** Takes the next receipt sequence number for a masjid and year (atomic); read it with lastInsertId(). */
    @Modifying
    @Query(value = "INSERT INTO receipt_counters (masjid_id, year, last_seq) VALUES (:masjidId, :year, LAST_INSERT_ID(1)) "
            + "ON DUPLICATE KEY UPDATE last_seq = LAST_INSERT_ID(last_seq + 1)", nativeQuery = true)
    void bumpReceiptCounter(@Param("masjidId") Long masjidId, @Param("year") int year);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    long lastInsertId();
}
