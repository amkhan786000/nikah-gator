package org.nikahgator.mail;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class MailDtos {
    private MailDtos() {
    }

    public record SendRequest(@NotEmpty @Size(max = 5) List<String> to, @Size(max = 1000) String message) {
    }

    public record Failure(String to, String error) {
    }

    /** Who got the e-mail and who did not. */
    public record Outcome(List<String> sent, List<Failure> failed) {
    }

    public record LogRow(Long id, LocalDateTime createdAt, String kind, String status, String recipient, String subject,
            String sentByName, Long masjidId, String masjidName, String attachmentName, String relatedType, Long relatedId) {
    }

    public record LogDetail(Long id, LocalDateTime createdAt, String kind, String status, String recipient, String subject,
            String body, String error, String sentByName, Long sentByUserId, Long masjidId, String masjidName,
            String attachmentName, String relatedType, Long relatedId, boolean canResend) {
    }

    public record LogPage(List<LogRow> items, long total, int page, int size) {
    }
}
