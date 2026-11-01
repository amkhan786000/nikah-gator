package org.nikahgator.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class ReminderDtos {
    private ReminderDtos() {}

    public record ReminderSettings(boolean enabled, @Min(1) @Max(30) int daysBefore) {}

    /** The e-mail text itself is not returned, only what the imam needs to follow what was sent. */
    public record NotificationView(Long id, String kind, String channel, String recipient, String subject, String status,
                                   Instant sendAt, Instant sentAt, String error, int attempts) {}
}
