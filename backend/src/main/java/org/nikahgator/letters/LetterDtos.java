package org.nikahgator.letters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LetterDtos {
    private LetterDtos() {
    }

    public record TypeView(String key, String label, String subject, String body) {
    }

    public record Catalog(List<TypeView> types, List<LetterTemplates.Placeholder> placeholders) {
    }

    public record PreviewRequest(@NotBlank @Size(max = 30) String letterType, @Size(max = 200) String recipientName,
            @Size(max = 300) String addressee, @Size(max = 500) String purpose, LocalDate letterDate) {
    }

    public record Preview(String subject, String body) {
    }

    public record Save(@NotBlank @Size(max = 30) String letterType, @NotBlank @Size(max = 200) String recipientName,
            @Size(max = 300) String addressee, @Size(max = 500) String purpose, @NotNull LocalDate letterDate,
            @NotBlank @Size(max = 200) String subject, @NotBlank @Size(max = 10000) String body) {
    }

    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record View(Long id, String letterNumber, String letterType, String letterTypeLabel, String recipientName,
            String addressee, String purpose, String subject, String body, LocalDate letterDate, String signerName,
            String status, String voidReason, LocalDateTime voidedAt, LocalDateTime createdAt) {
    }
}
