package org.nikahgator.certificate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nikahgator.certificate.CertificateDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Designs and renders a masjid's nikah certificate. Every method is limited to the given masjid. */
@Service
public class CertificateTemplateService {
    static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9]*)\\s*}}");
    static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final List<Placeholder> PLACEHOLDERS = List.of(
            new Placeholder("groomName", "Groom's full name", "Yusuf Ahmed"),
            new Placeholder("groomFather", "Groom's father's name", "Ibrahim Ahmed"),
            new Placeholder("brideName", "Bride's full name", "Maryam Hassan"),
            new Placeholder("brideFather", "Bride's father's name", "Omar Hassan"),
            new Placeholder("nikahDate", "Date of the nikah", "Saturday, December 5, 2026"),
            new Placeholder("nikahTime", "Time of the nikah", "2:30 PM"),
            new Placeholder("mahr", "Mahr", "$1,000 and a gold ring"),
            new Placeholder("wali", "Wali's name", "Ibrahim Hassan"),
            new Placeholder("waliRelation", "Wali's relation to the bride", "Father"),
            new Placeholder("witnesses", "Both witnesses", "Bilal Khan and Idris Ali"),
            new Placeholder("masjidName", "Masjid name", "Masjid Al-Noor"),
            new Placeholder("masjidAddress", "Masjid address", "123 Main St, Gainesville, FL 32601"),
            new Placeholder("imamName", "Imam / officiant", "Imam Abdullah"),
            new Placeholder("certNumber", "Certificate number", "NG-2026-0001"));

    private final CertificateTemplateRepository templates;
    private final MasjidRepository masjids;
    private final NikahRequestRepository nikahs;
    private final Path uploadRoot;

    public CertificateTemplateService(CertificateTemplateRepository templates, MasjidRepository masjids,
            NikahRequestRepository nikahs, @Value("${app.upload-dir}") String uploadDir) {
        this.templates = templates;
        this.masjids = masjids;
        this.nikahs = nikahs;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    // ---- contract used by the certificate-issuing feature ----

    /** The masjid's design, or an unsaved default one. Never null. */
    public CertificateTemplate forMasjid(Long masjidId) {
        return templates.findByMasjidId(masjidId).orElseGet(() -> newTemplate(masjidId));
    }

    /** Replaces {{key}} placeholders with plain-text values; unknown keys become empty. Values are never re-scanned. */
    public String render(String text, Map<String, String> values) {
        if (text == null) return "";
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = values == null ? null : values.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : stripControl(v).trim()));
        }
        m.appendTail(sb);
        return tidy(sb.toString());
    }

    public Map<String, String> valuesFor(Masjid m, NikahRequest n, AppUser imam, String certNumberOrNull) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("groomName", s(n.getGroomName()));
        v.put("brideName", s(n.getBrideName()));
        v.put("groomFather", s(n.getGroomFatherName()));
        v.put("brideFather", s(n.getBrideFatherName()));
        // scheduledAt is the masjid's local wall-clock time
        LocalDateTime at = n.getScheduledAt();
        v.put("nikahDate", at == null ? "" : at.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)));
        v.put("nikahTime", at == null ? "" : at.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)).replace(' ', ' '));
        v.put("mahr", s(n.getMahrDescription()));
        v.put("wali", s(n.getWaliName()));
        v.put("waliRelation", s(n.getWaliRelation()));
        String w1 = s(n.getWitness1Name()), w2 = s(n.getWitness2Name());
        v.put("witnesses", w1.isEmpty() ? w2 : w2.isEmpty() ? w1 : w1 + " and " + w2);
        v.put("masjidName", s(m.getName()));
        v.put("masjidAddress", addressOf(m));
        v.put("imamName", imam == null ? "" : s(imam.getFullName()));
        v.put("certNumber", s(certNumberOrNull));
        return v;
    }

    /** The logo file of the masjid, or null when none. */
    public Path logoFile(Long masjidId) {
        String rel = forMasjid(masjidId).getLogoPath();
        if (rel == null) return null;
        Path p = uploadRoot.resolve(rel).normalize();
        return p.startsWith(uploadRoot.resolve(String.valueOf(masjidId))) && Files.isRegularFile(p) ? p : null;
    }

    // ---- imam endpoints ----

    public TemplateView view(AppUser imam) {
        Long id = masjidOf(imam);
        Masjid m = masjid(id);
        boolean designed = templates.findByMasjidId(id).isPresent();
        CertificateTemplate t = forMasjid(id);
        return new TemplateView(designed, t.getTitle(), t.isBismillahEnabled(), t.getMasjidDisplayName(), t.getAddressText(),
                t.getPhoneText(), t.getIntroText(), t.getBodyText(), t.getFooterText(), t.getLeftSignatureLabel(),
                t.getRightSignatureLabel(), t.getAccentColor(), t.getPaperSize(), logoFile(id) != null,
                m.getName(), addressOf(m), s(m.getPhone()), PLACEHOLDERS);
    }

    @Transactional
    public TemplateView save(AppUser imam, Draft in) {
        Long id = masjidOf(imam);
        CertificateTemplate t = forMasjid(id);
        apply(t, in);
        templates.save(t);
        return view(imam);
    }

    @Transactional
    public TemplateView reset(AppUser imam) {
        Long id = masjidOf(imam);
        deleteFile(id);
        templates.findByMasjidId(id).ifPresent(templates::delete);
        return view(imam);
    }

    public Content preview(AppUser imam, PreviewRequest req) {
        Long id = masjidOf(imam);
        Masjid m = masjid(id);
        CertificateTemplate saved = forMasjid(id);
        // a detached copy: nothing is saved
        CertificateTemplate draft = newTemplate(id);
        if (req != null && req.draft() != null) {
            apply(draft, req.draft());
        } else {
            draft.setTitle(saved.getTitle());
            draft.setBismillahEnabled(saved.isBismillahEnabled());
            draft.setMasjidDisplayName(saved.getMasjidDisplayName());
            draft.setAddressText(saved.getAddressText());
            draft.setPhoneText(saved.getPhoneText());
            draft.setIntroText(saved.getIntroText());
            draft.setBodyText(saved.getBodyText());
            draft.setFooterText(saved.getFooterText());
            draft.setLeftSignatureLabel(saved.getLeftSignatureLabel());
            draft.setRightSignatureLabel(saved.getRightSignatureLabel());
            draft.setAccentColor(saved.getAccentColor());
            draft.setPaperSize(saved.getPaperSize());
        }
        Map<String, String> values;
        if (req != null && req.nikahId() != null) {
            NikahRequest n = nikahs.findByIdAndMasjidId(req.nikahId(), id)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
            values = valuesFor(m, n, imam, null);
        } else {
            values = sampleValues(m, imam);
        }
        return content(draft, m, values, logoFile(id) != null);
    }

    /** Builds the rendered certificate content; also usable by the issuing feature. */
    public Content content(CertificateTemplate t, Masjid m, Map<String, String> values, boolean hasLogo) {
        String name = blankToNull(t.getMasjidDisplayName()) != null ? t.getMasjidDisplayName() : s(m.getName());
        String address = t.getAddressText() != null ? t.getAddressText() : addressOf(m);
        String phone = t.getPhoneText() != null ? t.getPhoneText() : s(m.getPhone());
        return new Content(render(t.getTitle(), values), t.isBismillahEnabled(), name, render(address, values), render(phone, values),
                render(t.getIntroText(), values), render(t.getBodyText(), values), render(t.getFooterText(), values),
                render(t.getLeftSignatureLabel(), values), render(t.getRightSignatureLabel(), values),
                t.getAccentColor(), hasLogo);
    }

    @Transactional
    public TemplateView saveLogo(AppUser imam, MultipartFile file) {
        Long id = masjidOf(imam);
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Choose an image to upload.");
        if (file.getSize() > MAX_LOGO_BYTES) throw new ApiException(HttpStatus.BAD_REQUEST, "The logo must be 2 MB or smaller.");
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(8);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the file.");
        }
        String ext = detectExtension(head);
        if (ext == null) throw new ApiException(HttpStatus.BAD_REQUEST, "The logo must be a PNG or JPG image.");
        byte[] rnd = new byte[8];
        RANDOM.nextBytes(rnd);
        String rel = id + "/branding/logo-" + HexFormat.of().formatHex(rnd) + "." + ext;
        Path target = uploadRoot.resolve(rel).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the logo.");
        }
        deleteFile(id);
        CertificateTemplate t = forMasjid(id);
        t.setLogoPath(rel);
        templates.save(t);
        return view(imam);
    }

    @Transactional
    public TemplateView removeLogo(AppUser imam) {
        Long id = masjidOf(imam);
        deleteFile(id);
        templates.findByMasjidId(id).ifPresent(t -> {
            t.setLogoPath(null);
            templates.save(t);
        });
        return view(imam);
    }

    public Path logoFor(AppUser imam) {
        Path p = logoFile(masjidOf(imam));
        if (p == null) throw new ApiException(HttpStatus.NOT_FOUND, "No logo uploaded.");
        return p;
    }

    // ---- helpers ----

    static String detectExtension(byte[] h) {
        if (h.length >= 8 && (h[0] & 0xff) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G' && h[4] == 0x0d && h[5] == 0x0a
                && h[6] == 0x1a && h[7] == 0x0a) return "png";
        if (h.length >= 3 && (h[0] & 0xff) == 0xff && (h[1] & 0xff) == 0xd8 && (h[2] & 0xff) == 0xff) return "jpg";
        return null;
    }

    public static String contentType(Path p) {
        return p.getFileName().toString().endsWith(".png") ? "image/png" : "image/jpeg";
    }

    static String validateColor(String c) {
        if (c == null || c.isBlank()) return CertificateTemplate.DEFAULT_COLOR;
        String t = c.trim();
        if (!COLOR.matcher(t).matches()) throw new ApiException(HttpStatus.BAD_REQUEST, "Accent color must look like #4f7a00.");
        return t.toLowerCase(Locale.ROOT);
    }

    static String validatePaper(String p) {
        if (p == null || p.isBlank()) return "LETTER";
        String u = p.trim().toUpperCase(Locale.ROOT);
        if (!u.equals("LETTER") && !u.equals("A4")) throw new ApiException(HttpStatus.BAD_REQUEST, "Paper size must be LETTER or A4.");
        return u;
    }

    private static CertificateTemplate newTemplate(Long masjidId) {
        CertificateTemplate t = new CertificateTemplate();
        t.setMasjidId(masjidId);
        return t;
    }

    private void apply(CertificateTemplate t, Draft in) {
        t.setTitle(or(clean(in.title(), 120), CertificateTemplate.DEFAULT_TITLE));
        t.setBismillahEnabled(in.bismillahEnabled() == null || in.bismillahEnabled());
        t.setMasjidDisplayName(blankToNull(clean(in.masjidDisplayName(), 160)));
        t.setAddressText(blankToNull(clean(in.addressText(), 300)));
        t.setPhoneText(blankToNull(clean(in.phoneText(), 60)));
        t.setIntroText(clean(in.introText(), 500));
        t.setBodyText(clean(in.bodyText(), 2000));
        t.setFooterText(clean(in.footerText(), 500));
        t.setLeftSignatureLabel(clean(in.leftSignatureLabel(), 80));
        t.setRightSignatureLabel(clean(in.rightSignatureLabel(), 80));
        t.setAccentColor(validateColor(in.accentColor()));
        t.setPaperSize(validatePaper(in.paperSize()));
    }

    /** Strips control characters (keeping newlines), trims, caps the length. */
    static String clean(String s, int max) {
        String r = stripControl(s == null ? "" : s).trim();
        return r.length() > max ? r.substring(0, max) : r;
    }

    static String stripControl(String s) {
        return s.replace("\r", "").replaceAll("[\\p{Cc}&&[^\\n]]", "");
    }

    /** Drops clauses left empty by blank values and fixes spacing/punctuation. */
    static String tidy(String s) {
        String r = s;
        r = r.replaceAll("\\(\\s*\\)", "");
        r = r.replaceAll("(?i)\\s(son|daughter) of\\s+(?=with\\b|was\\b|[,.;:]|$)", " ");
        r = r.replaceAll("(?i),?\\s*\\b(with mahr|mahr|wali|witnesses|witness)\\s*:\\s*(?=[.,;]|$)", "");
        r = r.replaceAll("[ \\t]{2,}", " ");
        r = r.replaceAll("[ \\t]+([,.;:])", "$1");
        r = r.replaceAll(",{2,}", ",").replaceAll("\\.{2,}(?!\\.)", ".").replaceAll(",\\s*\\.", ".");
        return r.replaceAll("[ \\t]+\\n", "\n").trim();
    }

    private Map<String, String> sampleValues(Masjid m, AppUser imam) {
        Map<String, String> v = new LinkedHashMap<>();
        for (Placeholder p : PLACEHOLDERS) v.put(p.key(), p.example());
        v.put("masjidName", s(m.getName()));
        v.put("masjidAddress", addressOf(m));
        if (imam != null && imam.getFullName() != null) v.put("imamName", imam.getFullName());
        return v;
    }

    static String addressOf(Masjid m) {
        StringBuilder sb = new StringBuilder();
        append(sb, m.getAddressLine());
        append(sb, m.getCity());
        append(sb, (s(m.getState()) + " " + s(m.getZip())).trim());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part.trim());
    }

    private void deleteFile(Long masjidId) {
        Path p = logoFile(masjidId);
        if (p != null) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private Masjid masjid(Long id) {
        return masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }

    private static String s(String x) {
        return x == null ? "" : x.trim();
    }

    private static String blankToNull(String x) {
        return x == null || x.isBlank() ? null : x;
    }

    private static String or(String x, String d) {
        return x == null || x.isBlank() ? d : x;
    }
}
