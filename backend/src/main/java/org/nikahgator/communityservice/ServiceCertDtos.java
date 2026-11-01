package org.nikahgator.communityservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ServiceCertDtos {
    private ServiceCertDtos() {
    }

    public record Request(
            @NotBlank @Size(max = 200) String personName,
            @NotBlank @Size(max = 10) String personType,
            @Size(max = 200) String organization,
            @NotBlank @Size(max = 2000) String activities,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull BigDecimal hours,
            @NotBlank @Size(max = 200) String supervisor,
            @Size(max = 1000) String notes) {
    }

    public record View(
            Long id, String certNumber, String personName, String personType, String organization, String activities,
            LocalDate startDate, LocalDate endDate, BigDecimal hours, String supervisor, String notes, LocalDate issueDate,
            String status, String voidReason, LocalDateTime voidedAt) {
    }

    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record Pdf(String filename, byte[] bytes) {
    }
}
