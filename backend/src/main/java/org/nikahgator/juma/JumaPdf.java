package org.nikahgator.juma;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

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

/** Renders a Juma letter on the masjid's letterhead. */
final class JumaPdf {
    private static final Color INK = Letterhead.INK;

    private JumaPdf() {
    }

    static byte[] render(JumaLetter l, Letterhead.Info head, String imamName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(head.page(), 64, 64, 54, 60);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            if (JumaLetter.VOID.equals(l.getStatus())) writer.setPageEvent(new VoidMark());
            doc.addTitle("Juma letter - " + l.getSchoolName());
            doc.addCreator("Masjidly");
            doc.open();
            Letterhead.write(doc, head);

            Font body = new Font(Font.TIMES_ROMAN, 12, Font.NORMAL, INK);
            Font bold = new Font(Font.TIMES_ROMAN, 12, Font.BOLD, INK);

            doc.add(para(JumaRules.date(l.getLetterDate()), body, 14));
            doc.add(para(l.getAddressee(), body, 0));
            doc.add(para(l.getSchoolName(), body, 0));
            if (notBlank(l.getSchoolAddress())) doc.add(para(l.getSchoolAddress(), body, 0));
            if (notBlank(l.getSchoolContact())) doc.add(para(l.getSchoolContact(), body, 0));
            doc.add(para("Re: Request to excuse students for Friday (Juma) prayer", bold, 0, 14, 10));
            doc.add(para("Dear " + l.getAddressee() + ",", body, 10));

            boolean many = l.getStudents().size() > 1;
            String masjid = head.name();
            doc.add(para("Assalamu alaikum. On behalf of " + masjid + ", we respectfully request that the "
                    + (many ? "students" : "student") + " named below be excused from school every Friday, "
                    + JumaRules.period(l.isStanding(), l.getStartDate(), l.getEndDate()) + ", between "
                    + JumaRules.window(l.getWindowStart(), l.getWindowEnd()) + ", to attend the Juma (Friday congregational) prayer. "
                    + "Juma is an obligatory weekly act of worship for Muslims and can be offered only at this time of day.", body, 10));

            PdfPTable t = new PdfPTable(new float[] { 1f, 6f, 3f });
            t.setWidthPercentage(100);
            t.setSpacingAfter(10);
            header(t, "#", bold);
            header(t, "Student", bold);
            header(t, "Grade", bold);
            int i = 1;
            for (JumaLetter.Student s : l.getStudents()) {
                cell(t, String.valueOf(i++), body);
                cell(t, s.getName(), body);
                cell(t, s.getGrade() == null ? "" : s.getGrade(), body);
            }
            doc.add(t);

            doc.add(para((many ? "The students" : "The student") + " will return to school promptly after the prayer. "
                    + "We appreciate your understanding and your support of our students' religious needs.", body, 10));
            if (notBlank(l.getNote())) doc.add(para(l.getNote(), body, 10));
            doc.add(para("Please contact the masjid if you need anything further.", body, 18));
            doc.add(para("Sincerely,", body, 34));
            Paragraph sig = para(notBlank(imamName) ? imamName : "Imam", bold, 0);
            sig.setSpacingBefore(0);
            doc.add(sig);
            doc.add(para(masjid, body, 0));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the Juma letter PDF", e);
        }
    }

    private static void header(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(new Color(0xf3, 0xf4, 0xf6));
        c.setPadding(5);
        t.addCell(c);
    }

    private static void cell(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(5);
        t.addCell(c);
    }

    private static Paragraph para(String text, Font f, float after) {
        return para(text, f, 0, after, 0);
    }

    private static Paragraph para(String text, Font f, float before, float after, float unused) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_LEFT);
        p.setSpacingBefore(before);
        p.setSpacingAfter(after);
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** A diagonal VOID mark on every page of a voided letter. */
    private static final class VoidMark extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                Rectangle ps = document.getPageSize();
                PdfContentByte cb = writer.getDirectContent();
                cb.saveState();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.2f);
                cb.setGState(gs);
                BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                cb.beginText();
                cb.setFontAndSize(bold, 150);
                cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                cb.showTextAligned(Element.ALIGN_CENTER, "VOID", ps.getWidth() / 2, ps.getHeight() / 2 - 50, 40);
                cb.endText();
                cb.restoreState();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
