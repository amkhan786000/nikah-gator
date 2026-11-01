package org.nikahgator.juma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class JumaDtos {
    private JumaDtos() {
    }

    public record StudentDto(@NotBlank @Size(max = 150) String name, @Size(max = 40) String grade) {
    }

    /** Create / edit body. endDate is required unless the request is standing; the window defaults to 12:30-2:00 pm. */
    public record Request(
            @NotBlank @Size(max = 200) String schoolName,
            @Size(max = 300) String schoolAddress,
            @Size(max = 200) String schoolContact,
            @NotBlank @Size(max = 200) String addressee,
            LocalDate letterDate,
            boolean standing,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            LocalTime windowStart,
            LocalTime windowEnd,
            @Size(max = 1000) String note,
            @NotEmpty @Size(max = 60) List<@Valid StudentDto> students) {
    }

    public record View(
            Long id, String schoolName, String schoolAddress, String schoolContact, String addressee, LocalDate letterDate,
            boolean standing, LocalDate startDate, LocalDate endDate, LocalTime windowStart, LocalTime windowEnd,
            String note, String status, String voidReason, LocalDateTime voidedAt, List<StudentDto> students) {
    }

    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record Pdf(String filename, byte[] bytes) {
    }
}
