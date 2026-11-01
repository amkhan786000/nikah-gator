package org.nikahgator.communityservice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.nikahgator.communityservice.ServiceCertDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Community service certificates of the imam's own masjid. Another masjid's record looks the same as a missing one (404). */
@Service
public class ServiceCertService {
    private final ServiceCertRepository certs;
    private final MasjidRepository masjids;
    private final Letterhead letterhead;
    private final JdbcClient jdbc;

    public ServiceCertService(ServiceCertRepository certs, MasjidRepository masjids, Letterhead letterhead, JdbcClient jdbc) {
        this.certs = certs;
        this.masjids = masjids;
        this.letterhead = letterhead;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<View> list(AppUser imam) {
        return certs.findByMasjidIdOrderByIdDesc(masjidOf(imam)).stream().map(ServiceCertService::view).toList();
    }

    @Transactional(readOnly = true)
    public View get(AppUser imam, Long id) {
        return view(find(imam, id));
    }

    @Transactional
    public View create(AppUser imam, Request in) {
        Long masjidId = masjidOf(imam);
        LocalDate today = today(masjidId);
        Request r = ServiceCertRules.normalize(in, today);
        ServiceCert c = new ServiceCert();
        c.setMasjidId(masjidId);
        c.setCreatedBy(imam.getId());
        c.setIssueDate(today);
        c.setCertNumber(nextNumber(masjidId, today.getYear()));
        apply(c, r);
        return view(certs.save(c));
    }

    /** Edits keep the certificate number. */
    @Transactional
    public View update(AppUser imam, Long id, Request in) {
        ServiceCert c = find(imam, id);
        requireValid(c);
        apply(c, ServiceCertRules.normalize(in, today(c.getMasjidId())));
        return view(certs.save(c));
    }

    @Transactional
    public View voidCert(AppUser imam, Long id, String reason) {
        ServiceCert c = find(imam, id);
        requireValid(c);
        c.setStatus(ServiceCert.VOID);
        c.setVoidReason(ServiceCertRules.clean(reason));
        c.setVoidedAt(LocalDateTime.now());
        c.setVoidedBy(imam.getId());
        return view(certs.save(c));
    }

    @Transactional(readOnly = true)
    public Pdf pdf(AppUser imam, Long id) {
        ServiceCert c = find(imam, id);
        byte[] bytes = ServiceCertPdf.render(c, letterhead.forMasjid(c.getMasjidId()), imam.getFullName());
        return new Pdf(c.getCertNumber() + ".pdf", bytes);
    }

    // ---- helpers ----

    /** Atomic per masjid and year: the upsert row-locks the counter until this transaction ends. */
    private String nextNumber(Long masjidId, int year) {
        jdbc.sql("""
                INSERT INTO community_service_sequences (masjid_id, cert_year, last_seq) VALUES (:m, :y, 1)
                ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""")
                .param("m", masjidId).param("y", year).update();
        int seq = jdbc.sql("SELECT last_seq FROM community_service_sequences WHERE masjid_id = :m AND cert_year = :y")
                .param("m", masjidId).param("y", year).query(Integer.class).single();
        return ServiceCertNumbers.format(masjidId, year, seq);
    }

    private ServiceCert find(AppUser imam, Long id) {
        return certs.findByIdAndMasjidId(id, masjidOf(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Certificate not found"));
    }

    private static void requireValid(ServiceCert c) {
        if (!ServiceCert.VALID.equals(c.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This certificate has been voided and can no longer be changed.");
        }
    }

    private static void apply(ServiceCert c, Request r) {
        c.setPersonName(r.personName());
        c.setPersonType(r.personType());
        c.setOrganization(r.organization());
        c.setActivities(r.activities());
        c.setStartDate(r.startDate());
        c.setEndDate(r.endDate());
        c.setHours(r.hours());
        c.setSupervisor(r.supervisor());
        c.setNotes(r.notes());
    }

    static View view(ServiceCert c) {
        return new View(c.getId(), c.getCertNumber(), c.getPersonName(), c.getPersonType(), c.getOrganization(), c.getActivities(),
                c.getStartDate(), c.getEndDate(), c.getHours(), c.getSupervisor(), c.getNotes(), c.getIssueDate(), c.getStatus(),
                c.getVoidReason(), c.getVoidedAt());
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
