package org.nikahgator.salah;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One set of prayer times that applies from start_date to end_date (inclusive). */
@Entity
@Table(name = "salah_periods")
@Getter @Setter
public class SalahPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private LocalTime fajr;
    private LocalTime sunrise;
    private LocalTime dhuhr;
    private LocalTime asr;
    private LocalTime maghrib;
    private LocalTime isha;
    private LocalTime jumuah;
    private String notes;
}
