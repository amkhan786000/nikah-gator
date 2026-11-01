package org.nikahgator.divorce;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DivorceDtos {
    private DivorceDtos() {}

    /** Create / edit body. nikahId is only used on create (the link cannot be changed later). */
    public record Input(
            Long nikahId, @Size(max = 10) String divorceType, @Size(max = 10) String status,
            @Size(max = 200) String groomName, @Size(max = 200) String brideName,
            @Size(max = 200) String groomFatherName, @Size(max = 200) String brideFatherName,
            LocalDate marriageDate, @Size(max = 300) String marriagePlace,
            LocalDate divorceDate, Integer talaqNumber, LocalDate iddahEndDate, LocalDate revokedDate,
            @Size(max = 2000) String settlementNotes, @Size(max = 2000) String reconciliationNotes,
            @Size(max = 200) String witness1Name, @Size(max = 200) String witness2Name,
            @Size(max = 2000) String privateNotes) {
    }

    public record View(
            Long id, Long nikahId, String number, String divorceType, String status,
            String groomName, String brideName, String groomFatherName, String brideFatherName,
            LocalDate marriageDate, String marriagePlace, LocalDate divorceDate, Integer talaqNumber,
            LocalDate iddahEndDate, LocalDate revokedDate, String settlementNotes, String reconciliationNotes,
            String witness1Name, String witness2Name, String privateNotes, String imamName,
            boolean voided, String voidReason, LocalDateTime voidedAt, LocalDateTime createdAt) {
    }

    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {
    }
}
