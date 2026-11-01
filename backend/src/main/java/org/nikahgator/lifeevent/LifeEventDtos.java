package org.nikahgator.lifeevent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LifeEventDtos {
    private LifeEventDtos() {}

    /** Create / edit body. Which fields are required depends on the service (see LifeEventRules). */
    public record Input(
            @Size(max = 200) String personName, @Size(max = 200) String altName, LocalDate eventDate,
            LocalDate secondaryDate, LocalTime secondaryTime, @Size(max = 300) String place,
            LocalDate burialDate, LocalTime burialTime, @Size(max = 300) String burialPlace, @Size(max = 200) String cemetery,
            @Size(max = 200) String contactName, @Size(max = 60) String contactPhone,
            @Size(max = 300) String parents, @Size(max = 300) String witnesses, @Size(max = 160) String imamName,
            @Size(max = 2000) String notes) {
    }

    public record View(
            Long id, String number, String status, String personName, String altName, LocalDate eventDate,
            LocalDate secondaryDate, LocalTime secondaryTime, String place, LocalDate burialDate, LocalTime burialTime,
            String burialPlace, String cemetery, String contactName, String contactPhone, String parents, String witnesses,
            String imamName, String notes, String voidReason, LocalDateTime voidedAt, LocalDateTime createdAt) {
    }

    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {
    }
}
