package org.nikahgator.letters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nikahgator.certificate.CertificateTemplate;
import org.nikahgator.certificate.CertificateTemplateService;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.letters.LetterDtos.*;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Letters of the imam's own masjid. Every query is scoped by masjid id; another masjid's letter is a 404. */
@Service
public class LetterService {
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private final LetterRepository letters;
    private final MasjidRepository masjids;
    private final CertificateTemplateService templates;
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    public LetterService(LetterRepository letters, MasjidRepository masjids, CertificateTemplateService templates,
            JdbcClient jdbc, TransactionTemplate tx) {
        this.letters = letters;
        this.masjids = masjids;
        this.templates = templates;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public Catalog catalog() {
        return new Catalog(LetterTemplates.TYPES.stream()
                .map(t -> new TypeView(t.key(), t.label(), t.subject(), t.body())).toList(), LetterTemplates.PLACEHOLDERS);
    }

    /** Fills a built-in template for the editor. Nothing is saved. */
    public Preview preview(AppUser imam, PreviewRequest in) {
        Masjid m = masjid(masjidOf(imam));
        LetterTemplates.Type type = type(in.letterType());
        Map<String, String> v = values(m, imam, in.recipientName(), in.addressee(), in.purpose(),
                in.letterDate() == null ? LocalDate.now() : in.letterDate());
        return new Preview(LetterTemplates.render(type.subject(), v), LetterTemplates.render(type.body(), v));
    }

    public List<View> list(AppUser imam) {
        return letters.findByMasjidIdOrderByIdDesc(masjidOf(imam)).stream().map(LetterService::toView).toList();
    }

    public View get(AppUser imam, Long id) {
        return toView(find(imam, id));
    }

    public View create(AppUser imam, Save in) {
        Long masjidId = masjidOf(imam);
        Masjid m = masjid(masjidId);
        type(in.letterType());
        Long id = tx.execute(s -> {
            int year = LocalDate.now().getYear();
            // Atomic per masjid and year: the upsert row-locks the counter until this transaction ends.
            jdbc.sql("""
                    INSERT INTO letter_sequences (masjid_id, letter_year, last_seq) VALUES (:m, :y, 1)
                    ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""")
                    .param("m", masjidId).param("y", year).update();
            int seq = jdbc.sql("SELECT last_seq FROM letter_sequences WHERE masjid_id = :m AND letter_year = :y")
                    .param("m", masjidId).param("y", year).query(Integer.class).single();
            Letter l = new Letter();
            l.setMasjidId(masjidId);
            l.setLetterNumber(LetterNumbers.format(masjidId, year, seq));
            l.setCreatedBy(imam.getId());
            l.setSignerName(imam.getFullName() == null ? "" : imam.getFullName().trim());
            apply(l, m, imam, in);
            return letters.saveAndFlush(l).getId();
        });
        return toView(letters.findById(id).orElseThrow());
    }

    public View update(AppUser imam, Long id, Save in) {
        Letter l = find(imam, id);
        requireIssued(l);
        type(in.letterType());
        apply(l, masjid(l.getMasjidId()), imam, in);
        letters.saveAndFlush(l);
        return toView(letters.findById(id).orElseThrow());
    }

    public View voidLetter(AppUser imam, Long id, String reason) {
        Letter l = find(imam, id);
        requireIssued(l);
        l.setStatus(Letter.VOID);
        l.setVoidReason(LetterTemplates.clean(reason));
        l.setVoidedAt(LocalDateTime.now());
        l.setVoidedBy(imam.getId());
        letters.saveAndFlush(l);
        return toView(letters.findById(id).orElseThrow());
    }

    public record Pdf(String filename, byte[] bytes) {
    }

    public Pdf pdf(AppUser imam, Long id) {
        Letter l = find(imam, id);
        Masjid m = masjid(l.getMasjidId());
        CertificateTemplate t = templates.forMasjid(m.getId());
        Map<String, String> v = LetterTemplates.values("", "", "", "", "", m.getName(), addressOf(m));
        String name = t.getMasjidDisplayName() != null && !t.getMasjidDisplayName().isBlank() ? t.getMasjidDisplayName() : m.getName();
        String address = t.getAddressText() != null ? t.getAddressText() : addressOf(m);
        String phone = t.getPhoneText() != null ? t.getPhoneText() : m.getPhone();
        LetterPdf.Letterhead head = new LetterPdf.Letterhead(LetterTemplates.render(name, v), LetterTemplates.render(address, v),
                LetterTemplates.render(phone, v), t.getAccentColor(), t.getPaperSize(), logoBytes(m.getId()));
        byte[] bytes = LetterPdf.render(l, head);
        return new Pdf(l.getLetterNumber() + ".pdf", bytes);
    }

    // ---- helpers ----

    private void apply(Letter l, Masjid m, AppUser imam, Save in) {
        Map<String, String> v = values(m, imam, in.recipientName(), in.addressee(), in.purpose(), in.letterDate());
        l.setLetterType(in.letterType());
        l.setRecipientName(cap(LetterTemplates.clean(in.recipientName()), 200));
        l.setAddressee(blankToNull(cap(LetterTemplates.clean(nz(in.addressee())), 300)));
        l.setPurpose(blankToNull(cap(LetterTemplates.clean(nz(in.purpose())), 500)));
        // placeholders the imam left or typed in the final text are resolved, so none reaches the PDF
        l.setSubject(cap(LetterTemplates.render(in.subject(), v).replace("\n", " "), 200));
        l.setBody(LetterTemplates.render(in.body(), v));
        if (l.getRecipientName().isEmpty() || l.getSubject().isEmpty() || l.getBody().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The person, subject and letter text cannot be empty.");
        }
        l.setLetterDate(in.letterDate());
    }

    private Map<String, String> values(Masjid m, AppUser imam, String recipient, String addressee, String purpose, LocalDate date) {
        return LetterTemplates.values(nz(recipient), nz(addressee), nz(purpose), date.format(LONG_DATE),
                imam.getFullName() == null ? "" : imam.getFullName(), nz(m.getName()), addressOf(m));
    }

    static String addressOf(Masjid m) {
        StringBuilder sb = new StringBuilder();
        append(sb, m.getAddressLine());
        append(sb, m.getCity());
        append(sb, ((m.getState() == null ? "" : m.getState().trim()) + " " + (m.getZip() == null ? "" : m.getZip().trim())).trim());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part.trim());
    }

    private byte[] logoBytes(Long masjidId) {
        try {
            Path p = templates.logoFile(masjidId);
            return p == null || !Files.isRegularFile(p) ? null : Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    private Letter find(AppUser imam, Long id) {
        return letters.findByIdAndMasjidId(id, masjidOf(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Letter not found"));
    }

    private static void requireIssued(Letter l) {
        if (!Letter.ISSUED.equals(l.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This letter is void and can no longer be changed.");
        }
    }

    private static LetterTemplates.Type type(String key) {
        return LetterTemplates.byKey(key).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown letter type."));
    }

    private Masjid masjid(Long id) {
        return masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String cap(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static View toView(Letter l) {
        String label = LetterTemplates.byKey(l.getLetterType()).map(LetterTemplates.Type::label).orElse(l.getLetterType());
        return new View(l.getId(), l.getLetterNumber(), l.getLetterType(), label, l.getRecipientName(), l.getAddressee(),
                l.getPurpose(), l.getSubject(), l.getBody(), l.getLetterDate(), l.getSignerName(), l.getStatus(),
                l.getVoidReason(), l.getVoidedAt(), l.getCreatedAt());
    }
}
