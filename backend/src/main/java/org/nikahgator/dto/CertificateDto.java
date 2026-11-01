package org.nikahgator.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificateDto(Long id, String number, String status, LocalDate nikahDate, LocalDateTime issuedAt,
        String voidReason, LocalDateTime voidedAt, String verifyUrl) {

    /** What anyone with the number may see: first names only, no surnames or contact details. */
    public record Verification(boolean valid, String status, String masjidName, String city, String state,
            LocalDate nikahDate, String groomFirstName, String brideFirstName) {
    }

    public record VoidRequest(@NotBlank @Size(min = 3, max = 500) String reason) {
    }
}
