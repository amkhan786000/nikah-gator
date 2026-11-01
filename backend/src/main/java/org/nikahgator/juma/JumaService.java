package org.nikahgator.juma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.juma.JumaDtos.*;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Juma letters of the imam's own masjid. Another masjid's letter looks the same as a missing one (404). */
@Service
public class JumaService {
    private final JumaLetterRepository letters;
    private final MasjidRepository masjids;
    private final Letterhead letterhead;

    public JumaService(JumaLetterRepository letters, MasjidRepository masjids, Letterhead letterhead) {
        this.letters = letters;
        this.masjids = masjids;
        this.letterhead = letterhead;
    }

    @Transactional(readOnly = true)
    public List<View> list(AppUser imam) {
        return letters.findByMasjidIdOrderByIdDesc(masjidOf(imam)).stream().map(JumaService::view).toList();
    }

    @Transactional(readOnly = true)
    public View get(AppUser imam, Long id) {
        return view(find(imam, id));
    }

    @Transactional
    public View create(AppUser imam, Request in) {
        Long masjidId = masjidOf(imam);
        Request r = JumaRules.normalize(in, today(masjidId));
        JumaLetter l = new JumaLetter();
        l.setMasjidId(masjidId);
        l.setCreatedBy(imam.getId());
        apply(l, r);
        return view(letters.save(l));
    }

    @Transactional
    public View update(AppUser imam, Long id, Request in) {
        JumaLetter l = find(imam, id);
        requireActive(l);
        apply(l, JumaRules.normalize(in, today(l.getMasjidId())));
        return view(letters.save(l));
    }

    @Transactional
    public View voidLetter(AppUser imam, Long id, String reason) {
        JumaLetter l = find(imam, id);
        requireActive(l);
        l.setStatus(JumaLetter.VOID);
        l.setVoidReason(JumaRules.clean(reason));
        l.setVoidedAt(LocalDateTime.now());
        l.setVoidedBy(imam.getId());
        return view(letters.save(l));
    }

    @Transactional(readOnly = true)
    public Pdf pdf(AppUser imam, Long id) {
        JumaLetter l = find(imam, id);
        byte[] bytes = JumaPdf.render(l, letterhead.forMasjid(l.getMasjidId()), imam.getFullName());
        return new Pdf("juma-letter-" + l.getId() + ".pdf", bytes);
    }

    // ---- helpers ----

    private JumaLetter find(AppUser imam, Long id) {
        return letters.findByIdAndMasjidId(id, masjidOf(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Letter not found"));
    }

    private static void requireActive(JumaLetter l) {
        if (!JumaLetter.ACTIVE.equals(l.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This letter has been voided and can no longer be changed.");
        }
    }

    private static void apply(JumaLetter l, Request r) {
        l.setSchoolName(r.schoolName());
        l.setSchoolAddress(r.schoolAddress());
        l.setSchoolContact(r.schoolContact());
        l.setAddressee(r.addressee());
        l.setLetterDate(r.letterDate());
        l.setStanding(r.standing());
        l.setStartDate(r.startDate());
        l.setEndDate(r.endDate());
        l.setWindowStart(r.windowStart());
        l.setWindowEnd(r.windowEnd());
        l.setNote(r.note());
        l.getStudents().clear();
        r.students().forEach(s -> l.getStudents().add(new JumaLetter.Student(s.name(), s.grade())));
    }

    static View view(JumaLetter l) {
        return new View(l.getId(), l.getSchoolName(), l.getSchoolAddress(), l.getSchoolContact(), l.getAddressee(), l.getLetterDate(),
                l.isStanding(), l.getStartDate(), l.getEndDate(), l.getWindowStart(), l.getWindowEnd(), l.getNote(), l.getStatus(),
                l.getVoidReason(), l.getVoidedAt(),
                l.getStudents().stream().map(s -> new StudentDto(s.getName(), s.getGrade())).toList());
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
