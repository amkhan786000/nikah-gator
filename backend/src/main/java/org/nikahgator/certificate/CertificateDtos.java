package org.nikahgator.certificate;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CertificateDtos {
    private CertificateDtos() {}

    /** The editable fields (PUT body, and the draft sent to /preview). */
    public record Draft(
            @Size(max = 120) String title,
            @NotNull Boolean bismillahEnabled,
            @Size(max = 160) String masjidDisplayName,
            @Size(max = 300) String addressText,
            @Size(max = 60) String phoneText,
            @Size(max = 500) String introText,
            @Size(max = 2000) String bodyText,
            @Size(max = 500) String footerText,
            @Size(max = 80) String leftSignatureLabel,
            @Size(max = 80) String rightSignatureLabel,
            @Size(max = 7) String accentColor,
            @Size(max = 10) String paperSize) {
    }

    public record Placeholder(String key, String description, String example) {
    }

    /** GET response: the effective template plus the masjid's own defaults for blank fields. */
    public record TemplateView(
            boolean designed, String title, boolean bismillahEnabled, String masjidDisplayName, String addressText,
            String phoneText, String introText, String bodyText, String footerText, String leftSignatureLabel,
            String rightSignatureLabel, String accentColor, String paperSize, boolean hasLogo,
            String masjidName, String masjidAddress, String masjidPhone, List<Placeholder> placeholders) {
    }

    public record PreviewRequest(@jakarta.validation.Valid Draft draft, Long nikahId) {
    }

    /** A fully rendered certificate, ready for the frontend preview component. */
    public record Content(
            String title, boolean bismillah, String masjidName, String addressText, String phoneText, String intro,
            String body, String footer, String leftLabel, String rightLabel, String accentColor, boolean hasLogo) {
    }
}
