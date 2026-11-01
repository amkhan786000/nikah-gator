package org.nikahgator.hall;

import java.time.LocalDateTime;

import jakarta.validation.constraints.*;

public final class HallDtos {
    private HallDtos() {}

    public record Input(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 200) String contactName,
            @Size(max = 50) String contactPhone,
            @Email @Size(max = 200) String contactEmail,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            @Size(max = 100) String space,
            @Min(0) @Max(100000) Integer expectedGuests,
            String status,
            @Size(max = 200) String feeText,
            @Size(max = 5000) String notes) {}

    public record View(Long id, String title, String contactName, String contactPhone, String contactEmail,
            LocalDateTime startsAt, LocalDateTime endsAt, String space, Integer expectedGuests, String status,
            String feeText, String notes) {}
}
