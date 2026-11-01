package org.nikahgator.divorce;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.nikahgator.divorce.DivorceDtos.Input;
import org.nikahgator.divorce.DivorceDtos.View;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Divorce records. Every call is limited to the imam's own masjid; a record or nikah of another masjid is a 404.
 * Linking a nikah never changes that nikah.
 */
@Service
public class DivorceService implements org.nikahgator.mail.DocumentSource {
    private final DivorceRepository repo;
    private final NikahRequestRepository nikahs;
    private final MasjidRepository masjids;
    private final Letterhead letterhead;
    private final JdbcClient jdbc;

    public DivorceService(DivorceRepository repo, NikahRequestRepository nikahs, MasjidRepository masjids, Letterhead letterhead,
            JdbcClient jdbc) {
        this.repo = repo;
        this.nikahs = nikahs;
        this.masjids = masjids;
        this.letterhead = letterhead;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<View> list(AppUser imam) {
        return repo.findByMasjidIdOrderByDivorceDateDescIdDesc(masjidOf(imam)).stream().map(DivorceService::view).toList();
    }

    @Transactional(readOnly = true)
    public View get(AppUser imam, Long id) {
        return view(find(imam, id));
    }

    @Transactional(readOnly = true)
    public Optional<View> byNikah(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        nikahs.findByIdAndMasjidId(nikahId, masjidId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
        return repo.findByNikahIdAndMasjidIdAndVoidedFalse(nikahId, masjidId).map(DivorceService::view);
    }

    @Transactional
    public View create(AppUser imam, Input in) {
        Long masjidId = masjidOf(imam);
        LocalDate today = today(masjidId);
        NikahRequest nikah = null;
        if (in.nikahId() != null) {
            nikah = nikahs.findByIdAndMasjidId(in.nikahId(), masjidId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
            if (repo.findByNikahIdAndMasjidIdAndVoidedFalse(nikah.getId(), masjidId).isPresent()) {
                throw new ApiException(HttpStatus.CONFLICT, "A divorce is already recorded for this nikah. Void it first to record another.");
            }
            in = withNikahDefaults(in, nikah);
        }
        Input c = DivorceRules.normalize(in, today);
        Divorce d = new Divorce();
        d.setMasjidId(masjidId);
        d.setNikahId(nikah == null ? null : nikah.getId());
        d.setCreatedBy(imam.getId());
        d.setRecordNumber(nextNumber(masjidId, today.getYear()));
        apply(d, c, imam);
        return view(repo.save(d));
    }

    /** Edits keep the number and the nikah link. */
    @Transactional
    public View update(AppUser imam, Long id, Input in) {
        Divorce d = find(imam, id);
        requireActive(d);
        apply(d, DivorceRules.normalize(in, today(d.getMasjidId())), imam);
        return view(repo.save(d));
    }

    @Transactional
    public View voidRecord(AppUser imam, Long id, String reason) {
        Divorce d = find(imam, id);
        requireActive(d);
        d.setVoided(true);
        d.setVoidReason(DivorceRules.clean(reason));
        d.setVoidedAt(LocalDateTime.now());
        d.setVoidedBy(imam.getId());
        return view(repo.save(d));
    }

    public record Pdf(String filename, byte[] bytes) {
    }

    @Transactional(readOnly = true)
    public Pdf pdf(AppUser imam, Long id) {
        Divorce d = find(imam, id);
        byte[] bytes = DivorcePdf.render(d, letterhead.forMasjid(d.getMasjidId()), today(d.getMasjidId()));
        return new Pdf(d.getRecordNumber() + ".pdf", bytes);
    }

    @Override
    public java.util.Set<String> documentTypes() {
        return java.util.Set.of("divorce");
    }

    @Override
    @Transactional(readOnly = true)
    public Doc load(AppUser imam, String type, Long id) {
        Divorce d = find(imam, id);
        if (d.isVoided()) {
            throw new ApiException(HttpStatus.CONFLICT, "This record is void and cannot be e-mailed.");
        }
        Pdf pdf = pdf(imam, id);
        // divorce records are private: the screen asks the imam to confirm before sending
        return new Doc(d.getMasjidId(), "Divorce record", d.getRecordNumber(), pdf.filename(), pdf.bytes(), true);
    }

    // ---- helpers ----

    /** The nikah fills in whatever the imam left blank. */
    static Input withNikahDefaults(Input in, NikahRequest n) {
        LocalDate married = in.marriageDate() != null ? in.marriageDate() : n.getScheduledAt() == null ? null : n.getScheduledAt().toLocalDate();
        return new Input(in.nikahId(), in.divorceType(), in.status(),
                or(in.groomName(), n.getGroomName()), or(in.brideName(), n.getBrideName()),
                or(in.groomFatherName(), n.getGroomFatherName()), or(in.brideFatherName(), n.getBrideFatherName()),
                married, in.marriagePlace(), in.divorceDate(), in.talaqNumber(), in.iddahEndDate(), in.revokedDate(),
                in.settlementNotes(), in.reconciliationNotes(), in.witness1Name(), in.witness2Name(), in.privateNotes());
    }

    private static String or(String a, String b) {
        return a == null || a.isBlank() ? b : a;
    }

    /** Atomic per masjid and year: the upsert row-locks the counter until the transaction ends. */
    private String nextNumber(Long masjidId, int year) {
        jdbc.sql("""
                INSERT INTO divorce_sequences (masjid_id, rec_year, last_seq) VALUES (:m, :y, 1)
                ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""")
                .param("m", masjidId).param("y", year).update();
        int seq = jdbc.sql("SELECT last_seq FROM divorce_sequences WHERE masjid_id = :m AND rec_year = :y")
                .param("m", masjidId).param("y", year).query(Integer.class).single();
        return DivorceNumbers.format(masjidId, year, seq);
    }

    private static void apply(Divorce d, Input c, AppUser imam) {
        d.setDivorceType(DivorceType.valueOf(c.divorceType()));
        d.setStatus(c.status());
        d.setGroomName(c.groomName());
        d.setBrideName(c.brideName());
        d.setGroomFatherName(c.groomFatherName());
        d.setBrideFatherName(c.brideFatherName());
        d.setMarriageDate(c.marriageDate());
        d.setMarriagePlace(c.marriagePlace());
        d.setDivorceDate(c.divorceDate());
        d.setTalaqNumber(c.talaqNumber());
        d.setIddahEndDate(c.iddahEndDate());
        d.setRevokedDate(c.revokedDate());
        d.setSettlementNotes(c.settlementNotes());
        d.setReconciliationNotes(c.reconciliationNotes());
        d.setWitness1Name(c.witness1Name());
        d.setWitness2Name(c.witness2Name());
        d.setPrivateNotes(c.privateNotes());
        if (d.getImamName() == null) d.setImamName(imam.getFullName());
    }

    private Divorce find(AppUser imam, Long id) {
        return repo.findByIdAndMasjidId(id, masjidOf(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Record not found"));
    }

    private static void requireActive(Divorce d) {
        if (d.isVoided()) throw new ApiException(HttpStatus.CONFLICT, "This record is void and cannot be changed.");
    }

    static View view(Divorce d) {
        return new View(d.getId(), d.getNikahId(), d.getRecordNumber(), d.getDivorceType().name(), d.getStatus(),
                d.getGroomName(), d.getBrideName(), d.getGroomFatherName(), d.getBrideFatherName(), d.getMarriageDate(),
                d.getMarriagePlace(), d.getDivorceDate(), d.getTalaqNumber(), d.getIddahEndDate(), d.getRevokedDate(),
                d.getSettlementNotes(), d.getReconciliationNotes(), d.getWitness1Name(), d.getWitness2Name(), d.getPrivateNotes(),
                d.getImamName(), d.isVoided(), d.getVoidReason(), d.getVoidedAt(), d.getCreatedAt());
    }

    private LocalDate today(Long masjidId) {
        String tz = masjids.findById(masjidId).map(Masjid::getTimezone).orElse(null);
        try {
            return LocalDate.now(ZoneId.of(tz));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }
}
