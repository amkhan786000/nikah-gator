package org.nikahgator.dto;

/**
 * The resolved certificate (template with placeholders filled in). Both the PDF and the on-screen preview are
 * built from this. {@code watermark} is true while the nikah is not COMPLETED (preview only, not valid).
 */
public record CertificateContent(String title, boolean bismillah, String masjidName, String address, String phone,
        String intro, String body, String footer, String leftLabel, String rightLabel, String accentColor,
        String logoUrl, String number, boolean watermark, String paperSize, String status) {
}
