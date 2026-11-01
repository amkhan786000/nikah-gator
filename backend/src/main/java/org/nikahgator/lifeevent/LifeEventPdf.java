package org.nikahgator.lifeevent;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/** Renders the janazah letter and the shahada / baby-naming certificates on the masjid's letterhead. */
public final class LifeEventPdf {
    private static final Color INK = new Color(0x22, 0x2b, 0x36);
    private static final Color MUTED = new Color(0x6b, 0x72, 0x80);
    private static final String BISMILLAH = "بسم الله الرحمن الرحيم";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private LifeEventPdf() {
    }

    /** The masjid's letterhead: name, address, phone, accent colour (#rrggbb), paper (LETTER or A4). */
    public record Letterhead(String name, String address, String phone, String accent, String paper) {
    }

    public static byte[] render(LifeEvent e, Letterhead lh, byte[] logo, byte[] arabicFont, LocalDate today) {
        boolean letter = e.getEventType() == LifeEventType.JANAZAH;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document("A4".equalsIgnoreCase(lh.paper()) ? PageSize.A4 : PageSize.LETTER, 64, 64, 60, 90);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            Color accent = color(lh.accent());
            writer.setPageEvent(new Frame(e, accent, !letter));
            doc.addTitle(e.getEventType().title() + " " + e.getCertNumber());
            doc.addCreator("Masjidly");
            doc.open();

            Font title = new Font(Font.TIMES_ROMAN, letter ? 22 : 30, Font.BOLD, accent);
            Font masjid = new Font(Font.TIMES_ROMAN, 18, Font.BOLD, INK);
            Font small = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
            Font body = new Font(Font.TIMES_ROMAN, letter ? 13 : 16, Font.NORMAL, INK);
            Font name = new Font(Font.TIMES_ROMAN, 22, Font.BOLD, INK);

            if (logo != null) {
                try {
                    org.openpdf.text.Image img = org.openpdf.text.Image.getInstance(logo);
                    img.scaleToFit(80, 70);
                    img.setAlignment(Element.ALIGN_CENTER);
                    img.setSpacingAfter(6);
                    doc.add(img);
                } catch (Exception ignored) {
                    // an unreadable logo must not block the document
                }
            }
            if (arabicFont != null) addBismillah(doc, arabicFont, accent);
            doc.add(centered(lh.name(), masjid, 0, 0));
            if (!blank(lh.address())) doc.add(centered(lh.address(), small, 0, 0));
            if (!blank(lh.phone())) doc.add(centered(lh.phone(), small, 0, 0));

            if (letter) {
                writeLetter(doc, e, lh, title, body, small, today);
            } else {
                doc.add(centered(e.getEventType().title(), title, 22, 14));
                if (e.getEventType() == LifeEventType.SHAHADA) writeShahada(doc, e, body, name);
                else writeNaming(doc, e, body, name);
            }

            PdfPTable sig = new PdfPTable(2);
            sig.setWidthPercentage(100);
            sig.setSpacingBefore(letter ? 40 : 50);
            String imam = blank(e.getImamName()) ? "Imam" : "Imam: " + e.getImamName();
            sig.addCell(signature(imam, small));
            sig.addCell(signature("Date: " + today.format(DATE), small));
            doc.add(sig);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build the PDF", ex);
        }
    }

    // ---- bodies ----

    private static void writeLetter(Document doc, LifeEvent e, Letterhead lh, Font title, Font body, Font small, LocalDate today) throws Exception {
        doc.add(centered(e.getEventType().title(), title, 22, 4));
        doc.add(centered("To the family of " + e.getPersonName(), body, 0, 14));
        doc.add(centered("Inna lillahi wa inna ilayhi raji'un. To Allah we belong and to Him we return.", small, 0, 12));
        List<String> lines = new ArrayList<>();
        lines.add("This is to confirm that the funeral prayer (janazah) for " + e.getPersonName()
                + ", who passed away on " + e.getEventDate().format(DATE) + ", " + janazahWhen(e) + ".");
        if (e.getBurialDate() != null || !blank(e.getCemetery()) || !blank(e.getBurialPlace())) {
            lines.add("The burial " + burialWhen(e) + ".");
        }
        if (!blank(e.getContactName()) || !blank(e.getContactPhone())) {
            lines.add("Contact for the family: " + join(" ", e.getContactName(), e.getContactPhone()) + ".");
        }
        if (!blank(e.getNotes())) lines.add(e.getNotes());
        lines.add("May Allah forgive " + e.getPersonName() + ", have mercy on them, and grant patience and comfort to the family.");
        for (String l : lines) {
            Paragraph p = new Paragraph(l, body);
            p.setAlignment(Element.ALIGN_LEFT);
            p.setSpacingAfter(10);
            doc.add(p);
        }
    }

    private static void writeShahada(Document doc, LifeEvent e, Font body, Font name) throws Exception {
        doc.add(centered("This is to certify that", body, 0, 8));
        doc.add(centered(e.getPersonName(), name, 0, 6));
        if (!blank(e.getAltName())) doc.add(centered("who has chosen the Muslim name  " + e.getAltName(), body, 0, 8));
        StringBuilder sb = new StringBuilder("declared the Shahada, the testimony of faith, and embraced Islam on ")
                .append(e.getEventDate().format(DATE));
        if (!blank(e.getPlace())) sb.append(" at ").append(e.getPlace());
        sb.append('.');
        doc.add(centered(sb.toString(), body, 0, 10));
        doc.add(centered("Witnesses: " + e.getWitnesses(), body, 0, 10));
        if (!blank(e.getNotes())) doc.add(centered(e.getNotes(), body, 0, 6));
        doc.add(centered("May Allah keep them firm upon the straight path.", body, 6, 0));
    }

    private static void writeNaming(Document doc, LifeEvent e, Font body, Font name) throws Exception {
        doc.add(centered("This is to certify that", body, 0, 8));
        doc.add(centered(e.getPersonName(), name, 0, 6));
        doc.add(centered("born on " + e.getEventDate().format(DATE) + (blank(e.getParents()) ? "" : ", child of " + e.getParents()), body, 0, 8));
        StringBuilder sb = new StringBuilder("was given this name");
        if (e.getSecondaryDate() != null) sb.append(" on ").append(e.getSecondaryDate().format(DATE));
        if (!blank(e.getPlace())) sb.append(" at ").append(e.getPlace());
        sb.append(", with the aqeeqah according to the Sunnah.");
        doc.add(centered(sb.toString(), body, 0, 10));
        if (!blank(e.getNotes())) doc.add(centered(e.getNotes(), body, 0, 6));
        doc.add(centered("May Allah make them a source of joy and righteousness for their family.", body, 6, 0));
    }

    static String janazahWhen(LifeEvent e) {
        StringBuilder sb = new StringBuilder("takes place on ").append(e.getSecondaryDate().format(DATE));
        if (e.getSecondaryTime() != null) sb.append(" at ").append(time(e.getSecondaryTime()));
        if (!blank(e.getPlace())) sb.append(" at ").append(e.getPlace());
        return sb.toString();
    }

    static String burialWhen(LifeEvent e) {
        StringBuilder sb = new StringBuilder("takes place");
        if (e.getBurialDate() != null) sb.append(" on ").append(e.getBurialDate().format(DATE));
        if (e.getBurialTime() != null) sb.append(" at ").append(time(e.getBurialTime()));
        String where = join(", ", e.getCemetery(), e.getBurialPlace());
        if (!where.isEmpty()) sb.append(" at ").append(where);
        return sb.toString();
    }

    private static String time(LocalTime t) {
        return t.format(TIME);
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (blank(p)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    // ---- drawing helpers ----

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception ex) {
            return new Color(0x4f, 0x7a, 0x00);
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
        c.setBorderWidthLeft(0);
        return c;
    }

    /** Border (certificates), number footer, and a diagonal VOID mark when the record was voided. */
    private static final class Frame extends PdfPageEventHelper {
        private final LifeEvent e;
        private final Color accent;
        private final boolean border;

        Frame(LifeEvent e, Color accent, boolean border) {
            this.e = e;
            this.accent = accent;
            this.border = border;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w = document.getPageSize().getWidth();
            float h = document.getPageSize().getHeight();
            if (border) {
                cb.saveState();
                cb.setColorStroke(accent);
                cb.setLineWidth(2.5f);
                cb.rectangle(28, 28, w - 56, h - 56);
                cb.stroke();
                cb.setLineWidth(0.8f);
                cb.rectangle(36, 36, w - 72, h - 72);
                cb.stroke();
                cb.restoreState();
            }
            try {
                BaseFont f = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                cb.beginText();
                cb.setFontAndSize(f, 8);
                cb.setColorFill(MUTED);
                cb.setTextMatrix(64, 60);
                cb.showText("Reference no. " + e.getCertNumber());
                cb.endText();
                if (LifeEvent.VOID.equals(e.getStatus())) {
                    BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
                    cb.saveState();
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(0.22f);
                    cb.setGState(gs);
                    cb.beginText();
                    cb.setFontAndSize(bold, 150);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_CENTER, "VOID", w / 2, h / 2 - 50, 40);
                    cb.endText();
                    cb.restoreState();
                    cb.beginText();
                    cb.setFontAndSize(bold, 9);
                    cb.setColorFill(new Color(0xc0, 0x39, 0x2b));
                    cb.showTextAligned(Element.ALIGN_LEFT, "VOIDED - this document is no longer valid", 64, 48, 0);
                    cb.endText();
                }
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
