package org.nikahgator.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.nikahgator.entity.Certificate;
import org.nikahgator.dto.CertificateContent;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Renders the A4 nikah certificate. Only built-in fonts unless an Arabic font is configured (then the Bismillah is added). */
public final class CertificatePdf {
    private static final Color GOLD = new Color(0x9a, 0x7b, 0x2c);
    private static final Color INK = new Color(0x22, 0x2b, 0x36);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final String BISMILLAH = "بسم الله الرحمن الرحيم";

    private CertificatePdf() {
    }

    /** Loads the optional Arabic font (a file path, else classpath fonts/Amiri-Regular.ttf); null when unavailable. */
    public static byte[] loadArabicFont(String configuredPath) {
        try {
            if (configuredPath != null && !configuredPath.isBlank()) {
                return Files.readAllBytes(Path.of(configuredPath));
            }
            try (InputStream in = CertificatePdf.class.getResourceAsStream("/fonts/Amiri-Regular.ttf")) {
                return in == null ? null : in.readAllBytes();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] render(Certificate cert, CertificateContent c, byte[] logo, String verifyUrl, byte[] arabicFont) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            org.openpdf.text.Rectangle page = "A4".equalsIgnoreCase(c.paperSize()) ? PageSize.A4 : PageSize.LETTER;
            Document doc = new Document(page, 64, 64, 60, 100);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            Color accent = color(c.accentColor());
            writer.setPageEvent(new Frame(cert, verifyUrl, accent));
            doc.addTitle("Nikah certificate " + cert.getCertNumber());
            doc.addCreator("Masjidly");
            doc.open();

            Font title = new Font(Font.TIMES_ROMAN, 32, Font.BOLD, accent);
            Font masjid = new Font(Font.TIMES_ROMAN, 18, Font.BOLD, INK);
            Font small = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
            Font body = new Font(Font.TIMES_ROMAN, 14, Font.NORMAL, INK);

            if (logo != null) {
                try {
                    org.openpdf.text.Image img = org.openpdf.text.Image.getInstance(logo);
                    img.scaleToFit(80, 70);
                    img.setAlignment(Element.ALIGN_CENTER);
                    img.setSpacingAfter(6);
                    doc.add(img);
                } catch (Exception ignored) {
                    // an unreadable logo must not block the certificate
                }
            }
            if (arabicFont != null && c.bismillah()) {
                addBismillah(doc, arabicFont, accent);
            }
            doc.add(centered(c.title(), title, 4, 6));
            doc.add(centered(c.masjidName(), masjid, 0, 0));
            doc.add(centered(c.address(), small, 0, 0));
            if (c.phone() != null && !c.phone().isBlank()) doc.add(centered(c.phone(), small, 0, 0));
            doc.add(centered(" ", small, 0, 8));
            if (c.intro() != null && !c.intro().isBlank()) doc.add(centered(c.intro(), body, 0, 12));
            Font strong = new Font(Font.TIMES_ROMAN, 17, Font.NORMAL, INK);
            for (String line : safe(c.body()).split("\\R")) {
                doc.add(centered(line.isBlank() ? " " : line, strong, 0, 6));
            }
            if (c.footer() != null && !c.footer().isBlank()) doc.add(centered(c.footer(), small, 18, 0));

            PdfPTable sig = new PdfPTable(2);
            sig.setWidthPercentage(100);
            sig.setSpacingBefore(48);
            sig.addCell(signature(c.leftLabel(), small));
            String right = c.rightLabel() == null || c.rightLabel().isBlank() ? "Date" : c.rightLabel();
            sig.addCell(signature(right + ": " + cert.getIssuedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)), small));
            doc.add(sig);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the certificate PDF", e);
        }
    }

    private static Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return GOLD;
        }
    }

    private static void addBismillah(Document doc, byte[] fontBytes, Color accent) throws Exception {
        BaseFont bf = BaseFont.createFont("arabic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(BISMILLAH, new Font(bf, 22, Font.NORMAL, accent)));
        c.setBorder(0);
        c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingBottom(8);
        t.addCell(c);
        doc.add(t);
    }

    private static Paragraph centered(String text, Font f, float before, float after) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        return p;
    }

    private static PdfPCell signature(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(org.openpdf.text.Rectangle.TOP);
        c.setBorderColor(MUTED);
        c.setPaddingTop(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        // gap between the two signature lines
        c.setBorderWidthLeft(0);
        return c;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Border, QR code, footer on every page; a diagonal VOID mark when the certificate was voided. */
    private static final class Frame extends PdfPageEventHelper {
        private final Certificate cert;
        private final String verifyUrl;

        private final Color accent;

        Frame(Certificate cert, String verifyUrl, Color accent) {
            this.cert = cert;
            this.verifyUrl = verifyUrl;
            this.accent = accent;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w = document.getPageSize().getWidth();
            float h = document.getPageSize().getHeight();
            cb.saveState();
            cb.setColorStroke(accent);
            cb.setLineWidth(2.5f);
            cb.rectangle(28, 28, w - 56, h - 56);
            cb.stroke();
            cb.setLineWidth(0.8f);
            cb.rectangle(36, 36, w - 72, h - 72);
            cb.stroke();
            cb.restoreState();

            drawQr(cb, verifyUrl, w - 64 - 62, 52, 62);
            try {
                BaseFont f = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.beginText();
                cb.setFontAndSize(f, 8);
                cb.setColorFill(MUTED);
                cb.setTextMatrix(64, 84);
                cb.showText("Certificate no. " + cert.getCertNumber());
                cb.setTextMatrix(64, 72);
                cb.showText("Verify at " + verifyUrl);
                cb.endText();

                if (Certificate.VOID.equals(cert.getStatus())) {
                    cb.saveState();
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(0.22f);
                    cb.setGState(gs);
                    BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                    cb.beginText();
                    cb.setFontAndSize(bold, 150);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_CENTER, "VOID", w / 2, h / 2 - 50, 40);
                    cb.endText();
                    cb.restoreState();
                    cb.beginText();
                    cb.setFontAndSize(bold, 9);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_LEFT, "VOIDED - this certificate is no longer valid", 64, 60, 0);
                    cb.endText();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static void drawQr(PdfContentByte cb, String text, float x, float y, float size) {
            try {
                BitMatrix bm = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                        java.util.Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 0));
                int n = bm.getWidth();
                float cell = size / n;
                cb.saveState();
                cb.setColorFill(INK);
                for (int r = 0; r < n; r++) {
                    for (int c = 0; c < n; c++) {
                        if (bm.get(c, r)) {
                            cb.rectangle(x + c * cell, y + size - (r + 1) * cell, cell + 0.2f, cell + 0.2f);
                        }
                    }
                }
                cb.fill();
                cb.restoreState();
            } catch (Exception e) {
                // QR is a convenience; the printed number and URL still verify the certificate.
            }
        }
    }
}
