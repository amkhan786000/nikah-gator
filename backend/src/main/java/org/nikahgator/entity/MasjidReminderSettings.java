package org.nikahgator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** The reminder columns of a masjid (a narrow view of the masjids table, so Masjid itself stays untouched). */
@Entity
@Table(name = "masjids")
@Getter @Setter
public class MasjidReminderSettings {
    @Id
    private Long id;

    @Column(name = "reminders_enabled")
    private boolean remindersEnabled = true;

    @Column(name = "reminder_days_before")
    private int reminderDaysBefore = 7;
}
