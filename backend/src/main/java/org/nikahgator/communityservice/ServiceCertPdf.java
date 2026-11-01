package org.nikahgator.communityservice;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
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

/** Renders a community service certificate on the masjid's letterhead. */
final class ServiceCertPdf {
    private static final Color INK = Letterhead.INK;
    private static final Color MUTED = Letterhead.MUTED;

    private ServiceCertPdf() {
    }

    static byte[] render(ServiceCert c, Letterhead.Info head, String imamName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(head.page(), 64, 64, 54, 70);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Footer(c));
            doc.addTitle("Community service certificate " + c.getCertNumber());
            doc.addCreator("Masjidly");
            doc.open();
            Letterhead.write(doc, head);

            Font title = new Font(Font.TIMES_ROMAN, 28, Font.BOLD, head.accent());
            Font body = new Font(Font.TIMES_ROMAN, 13, Font.NORMAL, INK);
            Font name = new Font(Font.TIMES_ROMAN, 24, Font.BOLD, INK);
            Font small = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, MUTED);
            Font bold = new Font(Font.TIMES_ROMAN, 13, Font.BOLD, INK);

            doc.add(centered("Certificate of Community Service", title, 18, 18));
            doc.add(centered("This is to certify that", body, 0, 8));
            doc.add(centered(c.getPersonName(), name, 0, 4));
            if (c.getOrganization() != null) {
                String who = ServiceCert.STUDENT.equals(c.getPersonType()) ? "a student of " : "of ";
                doc.add(centered(who + c.getOrganization(), body, 0, 12));
            }
            doc.add(centered("completed " + ServiceCertRules.hoursText(c.getHours()) + " of community service with "
                    + head.name() + " from " + date(c) + ".", bold, 8, 14));
            doc.add(centered("Service performed", small, 6, 2));
            for (String line : c.getActivities().split("\\R")) {
                if (!line.isBlank()) doc.add(centered(line, body, 0, 3));
            }
            if (c.getNotes() != null) doc.add(centered(c.getNotes(), small, 12, 0));

            PdfPTable sig = new PdfPTable(2);
            sig.setWidthPercentage(100);
            sig.setSpacingBefore(54);
            sig.addCell(signature(c.getSupervisor() + ", Supervisor", small));
            String imam = imamName == null || imamName.isBlank() ? "Imam" : imamName;
            sig.addCell(signature(imam + ", Imam  -  "
                    + c.getIssueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)), small));
            doc.add(sig);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the community service certificate PDF", e);
        }
    }

    private static String date(ServiceCert c) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
        return c.getStartDate().equals(c.getEndDate()) ? c.getStartDate().format(f)
                : c.getStartDate().format(f) + " to " + c.getEndDate().format(f);
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

    /** Certificate number on every page; a diagonal VOID mark when voided. */
    private static final class Footer extends PdfPageEventHelper {
        private final ServiceCert cert;

        Footer(ServiceCert cert) {
            this.cert = cert;
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
                cb.showTextAligned(Element.ALIGN_LEFT, "Certificate no. " + cert.getCertNumber(), 64, 40, 0);
                cb.endText();
                if (ServiceCert.VOID.equals(cert.getStatus())) {
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
                    cb.beginText();
                    cb.setFontAndSize(bold, 9);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_LEFT, "VOIDED - this certificate is no longer valid", 64, 52, 0);
                    cb.endText();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
