package org.nikahgator.letterhead;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.nikahgator.certificate.CertificateTemplate;
import org.nikahgator.certificate.CertificateTemplateService;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.MasjidRepository;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The masjid's letterhead (logo, display name, address, phone, accent color), taken from its certificate design.
 * Shared by the letter-style services; read-only use of the certificate template.
 */
@Component
public class Letterhead {
    public static final Color INK = new Color(0x22, 0x2b, 0x36);
    public static final Color MUTED = new Color(0x6b, 0x72, 0x80);

    /** Everything the header needs; plain text, already placeholder-free. */
    public record Info(String name, String address, String phone, Color accent, byte[] logo, boolean a4) {
        public Rectangle page() {
            return a4 ? PageSize.A4 : PageSize.LETTER;
        }
    }

    private final CertificateTemplateService templates;
    private final MasjidRepository masjids;

    public Letterhead(CertificateTemplateService templates, MasjidRepository masjids) {
        this.templates = templates;
        this.masjids = masjids;
    }

    public Info forMasjid(Long masjidId) {
        Masjid m = masjids.findById(masjidId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        CertificateTemplate t = templates.forMasjid(masjidId);
        String address = addressOf(m);
        Map<String, String> values = Map.of("masjidName", nz(m.getName()), "masjidAddress", address);
        String name = blank(t.getMasjidDisplayName()) ? nz(m.getName()) : templates.render(t.getMasjidDisplayName(), values);
        String addr = t.getAddressText() == null ? address : templates.render(t.getAddressText(), values);
        String phone = t.getPhoneText() == null ? nz(m.getPhone()) : templates.render(t.getPhoneText(), values);
        return new Info(name, addr, phone, color(t.getAccentColor()), logoBytes(masjidId), "A4".equalsIgnoreCase(t.getPaperSize()));
    }

    /** Logo, name, address and phone centered, then a rule in the accent color. */
    public static void write(Document doc, Info info) throws DocumentException {
        if (info.logo() != null) {
            try {
                org.openpdf.text.Image img = org.openpdf.text.Image.getInstance(info.logo());
                img.scaleToFit(70, 60);
                img.setAlignment(Element.ALIGN_CENTER);
                img.setSpacingAfter(4);
                doc.add(img);
            } catch (Exception ignored) {
                // an unreadable logo must not block the document
            }
        }
        doc.add(centered(info.name(), new Font(Font.TIMES_ROMAN, 20, Font.BOLD, info.accent()), 0));
        Font small = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, MUTED);
        if (!blank(info.address())) doc.add(centered(info.address(), small, 0));
        if (!blank(info.phone())) doc.add(centered(info.phone(), small, 0));
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(8);
        rule.setSpacingAfter(14);
        PdfPCell c = new PdfPCell(new Paragraph(" ", new Font(Font.HELVETICA, 2)));
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(info.accent());
        c.setBorderWidthTop(2f);
        c.setPadding(0);
        rule.addCell(c);
        doc.add(rule);
    }

    private static Paragraph centered(String text, Font f, float after) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(after);
        return p;
    }

    private byte[] logoBytes(Long masjidId) {
        try {
            Path p = templates.logoFile(masjidId);
            return p == null || !Files.isRegularFile(p) ? null : Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    private static Color color(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.decode(CertificateTemplate.DEFAULT_COLOR);
        }
    }

    static String addressOf(Masjid m) {
        StringBuilder sb = new StringBuilder();
        append(sb, m.getAddressLine());
        append(sb, m.getCity());
        append(sb, (nz(m.getState()) + " " + nz(m.getZip())).trim());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part.trim());
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
