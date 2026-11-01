package org.nikahgator.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.nikahgator.certificate.CertificateTemplate;
import org.nikahgator.certificate.CertificateTemplateService;
import org.nikahgator.dto.CertificateContent;
import org.nikahgator.dto.CertificateDto;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Certificate;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.CertificateRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Issues, voids and renders certificates. Every imam call is limited to the imam's own masjid. */
@Service
public class CertificateService implements org.nikahgator.mail.DocumentSource {

    private final CertificateRepository certs;
    private final NikahRequestRepository nikahs;
    private final MasjidRepository masjids;
    private final AppUserRepository users;
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final CertificateTemplateService templates;

    @Value("${app.site-url:}")
    private String siteUrl;

    @Value("${app.certificate.arabic-font:}")
    private String arabicFontPath;

    private volatile byte[] arabicFont;
    private volatile boolean arabicFontLoaded;

    public CertificateService(CertificateRepository certs, NikahRequestRepository nikahs, MasjidRepository masjids,
            AppUserRepository users, JdbcClient jdbc, TransactionTemplate tx, CertificateTemplateService templates) {
        this.templates = templates;
        this.certs = certs;
        this.nikahs = nikahs;
        this.masjids = masjids;
        this.users = users;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Issues the certificate for a nikah; idempotent while a valid one exists. */
    public CertificateDto issue(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        NikahRequest n = findNikah(masjidId, nikahId);
        requireCompleted(n);
        try {
            Long id = tx.execute(s -> certs.findByRequestIdAndMasjidIdAndStatus(nikahId, masjidId, Certificate.VALID)
                    .orElseGet(() -> create(imam, n)).getId());
            // re-read so database defaults (issued_at) are filled in
            return toDto(certs.findById(id).orElseThrow());
        } catch (DataIntegrityViolationException race) {
            // Two requests issued at once: the unique "one valid certificate per nikah" key rejected the second.
            return certs.findByRequestIdAndMasjidIdAndStatus(nikahId, masjidId, Certificate.VALID).map(this::toDto).orElseThrow(() -> race);
        }
    }

    public CertificateDto get(AppUser imam, Long nikahId) {
        return toDto(latest(imam, nikahId));
    }

    public CertificateDto voidCertificate(AppUser imam, Long nikahId, String reason) {
        Certificate c = latest(imam, nikahId);
        if (!Certificate.VALID.equals(c.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This certificate is already void.");
        }
        c.setStatus(Certificate.VOID);
        c.setVoidReason(reason.trim());
        c.setVoidedAt(LocalDateTime.now());
        c.setVoidedBy(imam.getId());
        return toDto(certs.save(c));
    }

    public record Pdf(String filename, byte[] bytes) {
    }

    public Pdf pdf(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        Certificate c = latest(imam, nikahId);
        NikahRequest n = findNikah(masjidId, nikahId);
        requireCompleted(n);
        Masjid m = masjid(masjidId);
        AppUser issuer = users.findById(c.getIssuedBy()).orElse(imam);
        CertificateContent content = content(m, n, issuer, c.getCertNumber(), false, false);
        byte[] bytes = CertificatePdf.render(c, content, logoBytes(masjidId), verifyUrl(c.getCertNumber()), arabicFont());
        return new Pdf(c.getCertNumber() + ".pdf", bytes);
    }

    @Override
    public java.util.Set<String> documentTypes() {
        return java.util.Set.of("nikah-certificate");
    }

    /** E-mailing needs a valid issued certificate for a COMPLETED nikah; the preview is never emailable. */
    @Override
    public Doc load(AppUser imam, String type, Long nikahId) {
        Long masjidId = masjidOf(imam);
        NikahRequest n = findNikah(masjidId, nikahId);
        requireCompleted(n);
        Certificate c = certs.findByRequestIdAndMasjidIdAndStatus(nikahId, masjidId, Certificate.VALID)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "There is no valid certificate to e-mail. Issue the certificate first."));
        Pdf pdf = pdf(imam, nikahId);
        return new Doc(masjidId, "Nikah certificate", c.getCertNumber(), pdf.filename(), pdf.bytes(), false);
    }

    /** What the on-screen preview shows. Allowed in any status; watermarked until the nikah is COMPLETED. */
    public CertificateContent content(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        NikahRequest n = findNikah(masjidId, nikahId);
        String number = certs.findByRequestIdAndMasjidIdAndStatus(nikahId, masjidId, Certificate.VALID).map(Certificate::getCertNumber).orElse(null);
        return content(masjid(masjidId), n, imam, number, !isCompleted(n), true);
    }

    private CertificateContent content(Masjid m, NikahRequest n, AppUser imam, String number, boolean watermark, boolean inlineLogo) {
        CertificateTemplate t = templates.forMasjid(m.getId());
        java.util.Map<String, String> v = templates.valuesFor(m, n, imam, number);
        String logo = null;
        if (inlineLogo) {
            byte[] b = logoBytes(m.getId());
            if (b != null) logo = "data:" + (b.length > 3 && b[1] == 'P' ? "image/png" : "image/jpeg") + ";base64," + java.util.Base64.getEncoder().encodeToString(b);
        }
        return new CertificateContent(templates.render(t.getTitle(), v), t.isBismillahEnabled(),
                templates.render(t.getMasjidDisplayName(), v), templates.render(t.getAddressText(), v),
                t.getPhoneText() == null ? null : templates.render(t.getPhoneText(), v),
                templates.render(t.getIntroText(), v), templates.render(t.getBodyText(), v),
                t.getFooterText() == null ? null : templates.render(t.getFooterText(), v),
                templates.render(t.getLeftSignatureLabel(), v), templates.render(t.getRightSignatureLabel(), v),
                t.getAccentColor(), logo, number, watermark, t.getPaperSize(), n.getStatus());
    }

    private byte[] logoBytes(Long masjidId) {
        try {
            java.nio.file.Path p = templates.logoFile(masjidId);
            return p == null || !java.nio.file.Files.isRegularFile(p) ? null : java.nio.file.Files.readAllBytes(p);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private Masjid masjid(Long id) {
        return masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
    }

    public static boolean isCompleted(NikahRequest n) {
        return NikahRequest.COMPLETED.equals(n.getStatus());
    }

    /** Certificates are issued and downloaded only for a COMPLETED nikah. */
    public static void requireCompleted(NikahRequest n) {
        if (!isCompleted(n)) {
            throw new ApiException(HttpStatus.CONFLICT, "Mark the nikah as completed to issue or download the certificate.");
        }
    }

    /** Public: only valid/void, masjid, date and first names. Unknown or malformed numbers look the same (404). */
    public CertificateDto.Verification verify(String number) {
        if (!CertificateNumbers.isValid(number)) throw notFound();
        Certificate c = certs.findByCertNumber(number).orElseThrow(CertificateService::notFound);
        Masjid m = masjids.findById(c.getMasjidId()).orElseThrow(CertificateService::notFound);
        NikahRequest n = nikahs.findById(c.getRequestId()).orElseThrow(CertificateService::notFound);
        boolean ok = Certificate.VALID.equals(c.getStatus()) && isCompleted(n);
        return new CertificateDto.Verification(ok, ok ? Certificate.VALID : Certificate.VOID, m.getName(),
                m.getCity(), m.getState(), c.getNikahDate(), firstName(n.getGroomName()), firstName(n.getBrideName()));
    }

    static String firstName(String full) {
        if (full == null || full.isBlank()) return "";
        return full.trim().split("\\s+")[0];
    }

    private Certificate create(AppUser imam, NikahRequest n) {
        LocalDate date = n.getScheduledAt() != null ? n.getScheduledAt().toLocalDate() : LocalDate.now();
        int year = LocalDate.now().getYear();
        // Atomic per masjid and year: the upsert row-locks the counter until this transaction ends.
        jdbc.sql("""
                INSERT INTO certificate_sequences (masjid_id, cert_year, last_seq) VALUES (:m, :y, 1)
                ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""")
                .param("m", n.getMasjidId()).param("y", year).update();
        int seq = jdbc.sql("SELECT last_seq FROM certificate_sequences WHERE masjid_id = :m AND cert_year = :y")
                .param("m", n.getMasjidId()).param("y", year).query(Integer.class).single();
        Certificate c = new Certificate();
        c.setRequestId(n.getId());
        c.setMasjidId(n.getMasjidId());
        c.setCertNumber(CertificateNumbers.format(n.getMasjidId(), year, seq));
        c.setNikahDate(date);
        c.setIssuedBy(imam.getId());
        return certs.saveAndFlush(c);
    }

    private Certificate latest(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        findNikah(masjidId, nikahId);
        return certs.findFirstByRequestIdAndMasjidIdOrderByIdDesc(nikahId, masjidId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No certificate has been issued for this nikah."));
    }

    private NikahRequest findNikah(Long masjidId, Long nikahId) {
        return nikahs.findByIdAndMasjidId(nikahId, masjidId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        return imam.getMasjidId();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "No certificate found with this number.");
    }

    private String verifyUrl(String number) {
        String base = siteUrl == null ? "" : siteUrl.replaceAll("/+$", "");
        return base + "/verify/" + number;
    }

    private byte[] arabicFont() {
        if (!arabicFontLoaded) {
            arabicFont = CertificatePdf.loadArabicFont(arabicFontPath);
            arabicFontLoaded = true;
        }
        return arabicFont;
    }

    private CertificateDto toDto(Certificate c) {
        return new CertificateDto(c.getId(), c.getCertNumber(), c.getStatus(), c.getNikahDate(), c.getIssuedAt(),
                c.getVoidReason(), c.getVoidedAt(), verifyUrl(c.getCertNumber()));
    }
}
