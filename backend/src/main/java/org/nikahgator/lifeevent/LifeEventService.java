package org.nikahgator.lifeevent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.nikahgator.certificate.CertificateTemplate;
import org.nikahgator.certificate.CertificateTemplateService;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.lifeevent.LifeEventDtos.Input;
import org.nikahgator.lifeevent.LifeEventDtos.View;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.service.CertificatePdf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Janazah, shahada and baby-naming records. Every call is limited to the imam's own masjid and the service's type;
 * a record of another masjid (or another service) is a 404.
 */
@Service
public class LifeEventService {
    private final LifeEventRepository repo;
    private final MasjidRepository masjids;
    private final AppUserRepository users;
    private final CertificateTemplateService templates;
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    @Value("${app.certificate.arabic-font:}")
    private String arabicFontPath;
    private volatile byte[] arabicFont;
    private volatile boolean arabicFontLoaded;

    public LifeEventService(LifeEventRepository repo, MasjidRepository masjids, AppUserRepository users,
            CertificateTemplateService templates, JdbcClient jdbc, TransactionTemplate tx) {
        this.repo = repo;
        this.masjids = masjids;
        this.users = users;
        this.templates = templates;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public List<View> list(AppUser imam, LifeEventType type) {
        return repo.findByMasjidIdAndEventTypeOrderByEventDateDescIdDesc(masjidOf(imam), type).stream().map(LifeEventService::view).toList();
    }

    public View get(AppUser imam, LifeEventType type, Long id) {
        return view(find(imam, type, id));
    }

    public View create(AppUser imam, LifeEventType type, Input in) {
        Long masjidId = masjidOf(imam);
        Input c = LifeEventRules.normalize(type, in, LocalDate.now());
        Long id = tx.execute(s -> {
            LifeEvent e = new LifeEvent();
            e.setMasjidId(masjidId);
            e.setEventType(type);
            e.setCreatedBy(imam.getId());
            e.setCertNumber(nextNumber(type, masjidId));
            apply(e, c, imam);
            return repo.saveAndFlush(e).getId();
        });
        // re-read so database defaults (created_at) are filled in
        return view(repo.findById(id).orElseThrow());
    }

    public View update(AppUser imam, LifeEventType type, Long id, Input in) {
        LifeEvent e = find(imam, type, id);
        requireValid(e);
        apply(e, LifeEventRules.normalize(type, in, LocalDate.now()), imam);
        return view(repo.save(e));
    }

    public View voidRecord(AppUser imam, LifeEventType type, Long id, String reason) {
        LifeEvent e = find(imam, type, id);
        requireValid(e);
        e.setStatus(LifeEvent.VOID);
        e.setVoidReason(reason.trim());
        e.setVoidedAt(LocalDateTime.now());
        e.setVoidedBy(imam.getId());
        return view(repo.save(e));
    }

    public record Pdf(String filename, byte[] bytes) {
    }

    public Pdf pdf(AppUser imam, LifeEventType type, Long id) {
        LifeEvent e = find(imam, type, id);
        Long masjidId = e.getMasjidId();
        Masjid m = masjids.findById(masjidId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        CertificateTemplate t = templates.forMasjid(masjidId);
        var head = templates.content(t, m, java.util.Map.of(), false);
        var lh = new LifeEventPdf.Letterhead(head.masjidName(), head.addressText(), head.phoneText(), head.accentColor(), t.getPaperSize());
        if (e.getImamName() == null) e.setImamName(users.findById(e.getCreatedBy()).map(AppUser::getFullName).orElse(imam.getFullName()));
        byte[] bytes = LifeEventPdf.render(e, lh, logoBytes(masjidId), t.isBismillahEnabled() ? arabicFont() : null, LocalDate.now());
        return new Pdf(e.getCertNumber() + ".pdf", bytes);
    }

    // ---- helpers ----

    /** Atomic per masjid, type and year: the upsert row-locks the counter until the transaction ends. */
    private String nextNumber(LifeEventType type, Long masjidId) {
        int year = LocalDate.now().getYear();
        jdbc.sql("""
                INSERT INTO life_event_sequences (masjid_id, event_type, cert_year, last_seq) VALUES (:m, :t, :y, 1)
                ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""")
                .param("m", masjidId).param("t", type.name()).param("y", year).update();
        int seq = jdbc.sql("SELECT last_seq FROM life_event_sequences WHERE masjid_id = :m AND event_type = :t AND cert_year = :y")
                .param("m", masjidId).param("t", type.name()).param("y", year).query(Integer.class).single();
        return LifeEventNumbers.format(type, masjidId, year, seq);
    }

    private static void apply(LifeEvent e, Input c, AppUser imam) {
        e.setPersonName(c.personName());
        e.setAltName(c.altName());
        e.setEventDate(c.eventDate());
        e.setSecondaryDate(c.secondaryDate());
        e.setSecondaryTime(c.secondaryTime());
        e.setPlace(c.place());
        e.setBurialDate(c.burialDate());
        e.setBurialTime(c.burialTime());
        e.setBurialPlace(c.burialPlace());
        e.setCemetery(c.cemetery());
        e.setContactName(c.contactName());
        e.setContactPhone(c.contactPhone());
        e.setParents(c.parents());
        e.setWitnesses(c.witnesses());
        e.setImamName(c.imamName() != null ? c.imamName() : imam.getFullName());
        e.setNotes(c.notes());
    }

    private LifeEvent find(AppUser imam, LifeEventType type, Long id) {
        return repo.findByIdAndMasjidIdAndEventType(id, masjidOf(imam), type)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Record not found"));
    }

    private static void requireValid(LifeEvent e) {
        if (!LifeEvent.VALID.equals(e.getStatus())) throw new ApiException(HttpStatus.CONFLICT, "This record is void and cannot be changed.");
    }

    static View view(LifeEvent e) {
        return new View(e.getId(), e.getCertNumber(), e.getStatus(), e.getPersonName(), e.getAltName(), e.getEventDate(),
                e.getSecondaryDate(), e.getSecondaryTime(), e.getPlace(), e.getBurialDate(), e.getBurialTime(), e.getBurialPlace(),
                e.getCemetery(), e.getContactName(), e.getContactPhone(), e.getParents(), e.getWitnesses(), e.getImamName(),
                e.getNotes(), e.getVoidReason(), e.getVoidedAt(), e.getCreatedAt());
    }

    private byte[] logoBytes(Long masjidId) {
        try {
            java.nio.file.Path p = templates.logoFile(masjidId);
            return p == null || !java.nio.file.Files.isRegularFile(p) ? null : java.nio.file.Files.readAllBytes(p);
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    private byte[] arabicFont() {
        if (!arabicFontLoaded) {
            arabicFont = CertificatePdf.loadArabicFont(arabicFontPath);
            arabicFontLoaded = true;
        }
        return arabicFont;
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }
}
