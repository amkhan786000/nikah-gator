package org.nikahgator.salah;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** How a masjid publishes its times: how often they change, and what the shared link shows. */
@Entity
@Table(name = "salah_settings")
@Getter @Setter
public class SalahSettings {
    @Id
    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "change_unit")
    private String changeUnit = "MONTHS";

    @Column(name = "change_every")
    private int changeEvery = 1;

    @Column(name = "public_enabled")
    private boolean publicEnabled = true;

    @Column(name = "public_days")
    private int publicDays = 1;

    @Column(name = "show_sunrise")
    private boolean showSunrise = true;
}
