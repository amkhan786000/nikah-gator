package org.nikahgator.counseling;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.*;

public final class CounselingDtos {
    private CounselingDtos() {}

    public record Input(
            @NotBlank @Size(max = 200) String clientName,
            @NotBlank String sessionType,
            @NotNull LocalDateTime sessionAt,
            @Min(1) @Max(1440) Integer durationMin,
            @Size(max = 20000) String notes,
            LocalDate followUpOn,
            String status) {}

    /** List row: deliberately has no notes. */
    public record Row(Long id, String clientName, String sessionType, LocalDateTime sessionAt, Integer durationMin,
            LocalDate followUpOn, String status) {}

    public record Detail(Long id, String clientName, String sessionType, LocalDateTime sessionAt, Integer durationMin,
            String notes, LocalDate followUpOn, String status) {}
}
