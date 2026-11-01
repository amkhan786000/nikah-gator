package org.nikahgator.divorce;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.nikahgator.letterhead.Letterhead;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/** Renders the divorce record on the masjid's letterhead. Private notes are never printed. */
final class DivorcePdf {
    private static final Color INK = Letterhead.INK;
    private static final Color MUTED = Letterhead.MUTED;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    static final String DISCLAIMER = "This is a religious record kept by the masjid. It does not replace a civil divorce decree "
            + "and has no legal effect on its own.";

    private DivorcePdf() {
    }

    static byte[] render(Divorce d, Letterhead.Info head, LocalDate today) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(head.page(), 64, 64, 54, 80);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer(d));
            doc.addTitle("Divorce record " + d.getRecordNumber());
            doc.addCreator("Masjidly");
            doc.open();
            Letterhead.write(doc, head);

            Font title = new Font(Font.TIMES_ROMAN, 24, Font.BOLD, head.accent());
            Font body = new Font(Font.TIMES_ROMAN, 12.5f, Font.NORMAL, INK);
            Font bold = new Font(Font.TIMES_ROMAN, 12.5f, Font.BOLD, INK);
            Font small = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, MUTED);

            doc.add(centered("Divorce Record", title, 4, 4));
            doc.add(centered("Record no. " + d.getRecordNumber(), small, 0, 14));

            Paragraph intro = new Paragraph("This is to record that the following couple's marriage was ended as stated below.", body);
            intro.setSpacingAfter(10);
            doc.add(intro);

            PdfPTable t = new PdfPTable(new float[] { 32, 68 });
            t.setWidthPercentage(100);
            row(t, "Husband", join(d.getGroomName(), d.getGroomFatherName(), "son of "), bold, body);
            row(t, "Wife", join(d.getBrideName(), d.getBrideFatherName(), "daughter of "), bold, body);
            if (d.getMarriageDate() != null || d.getMarriagePlace() != null) {
                String m = d.getMarriageDate() == null ? "" : d.getMarriageDate().format(DATE);
                if (d.getMarriagePlace() != null) m += (m.isEmpty() ? "" : ", ") + d.getMarriagePlace();
                row(t, "Marriage (nikah)", m, bold, body);
            }
            row(t, "Type of divorce", d.getDivorceType().label(), bold, body);
            if (d.getDivorceType() == DivorceType.TALAQ && d.getTalaqNumber() != null) {
                row(t, "Talaq", ordinal(d.getTalaqNumber()) + " talaq" + (d.getTalaqNumber() == 3 ? " (final)" : ""), bold, body);
            }
            row(t, "Date of divorce", d.getDivorceDate().format(DATE), bold, body);
            if (d.getIddahEndDate() != null) row(t, "Iddah ends", d.getIddahEndDate().format(DATE), bold, body);
            if (Divorce.REVOKED.equals(d.getStatus()) && d.getRevokedDate() != null) {
                row(t, "Status", "Revoked: reconciled during the iddah on " + d.getRevokedDate().format(DATE), bold, body);
            } else {
                row(t, "Status", "Recorded", bold, body);
            }
            if (d.getSettlementNotes() != null) row(t, "Mahr and settlement", d.getSettlementNotes(), bold, body);
            if (d.getReconciliationNotes() != null) row(t, "Reconciliation efforts", d.getReconciliationNotes(), bold, body);
            if (d.getWitness1Name() != null) {
                row(t, "Witnesses", d.getWitness2Name() == null ? d.getWitness1Name() : d.getWitness1Name() + ", " + d.getWitness2Name(), bold, body);
            }
            doc.add(t);

            Paragraph note = new Paragraph(DISCLAIMER, new Font(Font.TIMES_ROMAN, 11, Font.ITALIC, INK));
            note.setSpacingBefore(18);
            doc.add(note);

            PdfPTable sig = new PdfPTable(2);
            sig.setWidthPercentage(100);
            sig.setSpacingBefore(46);
            String imam = d.getImamName() == null || d.getImamName().isBlank() ? "Imam" : "Imam: " + d.getImamName();
            sig.addCell(signature(imam, small));
            sig.addCell(signature("Date: " + today.format(DATE), small));
            doc.add(sig);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the divorce record PDF", e);
        }
    }

    static String ordinal(int n) {
        return switch (n) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> n + "th";
        };
    }

    private static String join(String name, String father, String relation) {
        return father == null ? name : name + ", " + relation + father;
    }

    private static void row(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell a = new PdfPCell(new Phrase(label, lf));
        PdfPCell b = new PdfPCell(new Phrase(value, vf));
        for (PdfPCell c : new PdfPCell[] { a, b }) {
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(new Color(0xe5, 0xe7, 0xeb));
            c.setPaddingTop(5);
            c.setPaddingBottom(6);
            t.addCell(c);
        }
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
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(MUTED);
        c.setPaddingTop(6);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    /** Record number and a short disclaimer on every page; a diagonal VOID mark when voided. */
    private static final class Footer extends PdfPageEventHelper {
        private final Divorce d;

        Footer(Divorce d) {
            this.d = d;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                Rectangle ps = document.getPageSize();
                BaseFont f = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.beginText();
                cb.setFontAndSize(f, 8);
                cb.setColorFill(MUTED);
                cb.showTextAligned(Element.ALIGN_LEFT, "Record no. " + d.getRecordNumber(), 64, 40, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Religious record - not a civil divorce decree", ps.getWidth() - 64, 40, 0);
                cb.endText();
                if (d.isVoided()) {
                    BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                    cb.saveState();
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(0.2f);
                    cb.setGState(gs);
                    cb.beginText();
                    cb.setFontAndSize(bold, 150);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_CENTER, "VOID", ps.getWidth() / 2, ps.getHeight() / 2 - 50, 40);
                    cb.endText();
                    cb.restoreState();
                    cb.beginText();
                    cb.setFontAndSize(bold, 9);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_LEFT, "VOIDED - this record is no longer valid", 64, 54, 0);
                    cb.endText();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
