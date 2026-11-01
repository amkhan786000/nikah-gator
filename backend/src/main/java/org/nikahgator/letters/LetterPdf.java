package org.nikahgator.letters;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/** Renders a letter on the masjid letterhead (logo, display name, address, phone). Built-in fonts only. */
public final class LetterPdf {
    private static final Color INK = new Color(0x22, 0x2b, 0x36);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final Color RED = new Color(0xc0, 0x39, 0x2b);

    public record Letterhead(String name, String address, String phone, String accentColor, String paperSize, byte[] logo) {
    }

    private LetterPdf() {
    }

    public static byte[] render(Letter l, Letterhead h) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            org.openpdf.text.Rectangle page = "A4".equalsIgnoreCase(h.paperSize()) ? PageSize.A4 : PageSize.LETTER;
            Document doc = new Document(page, 64, 64, 54, 72);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            Color accent = color(h.accentColor());
            writer.setPageEvent(new Footer(l, accent));
            doc.addTitle("Letter " + l.getLetterNumber());
            doc.addCreator("Nikah Gator");
            doc.open();

            Font name = new Font(Font.TIMES_ROMAN, 20, Font.BOLD, accent);
            Font small = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
            Font body = new Font(Font.TIMES_ROMAN, 12, Font.NORMAL, INK);
            Font bold = new Font(Font.TIMES_ROMAN, 12, Font.BOLD, INK);

            if (h.logo() != null) {
                try {
                    org.openpdf.text.Image img = org.openpdf.text.Image.getInstance(h.logo());
                    img.scaleToFit(70, 60);
                    img.setAlignment(Element.ALIGN_CENTER);
                    img.setSpacingAfter(4);
                    doc.add(img);
                } catch (Exception ignored) {
                    // an unreadable logo must not block the letter
                }
            }
            doc.add(centered(h.name(), name, 0, 2));
            if (notBlank(h.address())) doc.add(centered(h.address(), small, 0, 0));
            if (notBlank(h.phone())) doc.add(centered(h.phone(), small, 0, 0));
            // letterhead rule
            Paragraph rule = new Paragraph(" ", small);
            rule.setSpacingAfter(10);
            doc.add(rule);
            doc.add(new org.openpdf.text.pdf.draw.LineSeparator(1.2f, 100, accent, Element.ALIGN_CENTER, 0));

            Paragraph date = new Paragraph(l.getLetterDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)), body);
            date.setSpacingBefore(16);
            doc.add(date);
            Paragraph no = new Paragraph("Ref: " + l.getLetterNumber(), small);
            no.setSpacingAfter(14);
            doc.add(no);

            Paragraph subject = new Paragraph("Re: " + l.getSubject(), bold);
            subject.setSpacingAfter(12);
            doc.add(subject);

            for (String line : l.getBody().split("\\R", -1)) {
                Paragraph p = new Paragraph(line.isBlank() ? " " : line, body);
                p.setLeading(16);
                p.setSpacingAfter(2);
                doc.add(p);
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the letter PDF", e);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return new Color(0x4f, 0x7a, 0x00);
        }
    }

    private static Paragraph centered(String text, Font f, float before, float after) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        return p;
    }

    /** Letter number on every page; a diagonal VOID mark when the letter was voided. */
    private static final class Footer extends PdfPageEventHelper {
        private final Letter letter;
        private final Color accent;

        Footer(Letter letter, Color accent) {
            this.letter = letter;
            this.accent = accent;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w = document.getPageSize().getWidth();
            float h = document.getPageSize().getHeight();
            try {
                BaseFont f = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.saveState();
                cb.setColorStroke(accent);
                cb.setLineWidth(0.8f);
                cb.moveTo(64, 50);
                cb.lineTo(w - 64, 50);
                cb.stroke();
                cb.beginText();
                cb.setFontAndSize(f, 8);
                cb.setColorFill(MUTED);
                cb.showTextAligned(Element.ALIGN_LEFT, "Letter no. " + letter.getLetterNumber(), 64, 38, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Page " + writer.getPageNumber(), w - 64, 38, 0);
                cb.endText();
                cb.restoreState();

                if (Letter.VOID.equals(letter.getStatus())) {
                    cb.saveState();
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(0.22f);
                    cb.setGState(gs);
                    BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                    cb.beginText();
                    cb.setFontAndSize(bold, 150);
                    cb.setColorFill(RED);
                    cb.showTextAligned(Element.ALIGN_CENTER, "VOID", w / 2, h / 2 - 50, 40);
                    cb.endText();
                    cb.restoreState();
                    cb.beginText();
                    cb.setFontAndSize(bold, 9);
                    cb.setColorFill(RED);
                    cb.showTextAligned(Element.ALIGN_LEFT, "VOIDED - this letter is no longer valid", 64, 56, 0);
                    cb.endText();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
