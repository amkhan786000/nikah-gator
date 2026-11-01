package org.nikahgator.counseling;

import java.time.LocalDate;
import java.util.List;

import org.nikahgator.counseling.CounselingDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Every query is limited to the imam's own masjid; another masjid's session looks like it does not exist. */
@Service
public class CounselingService {
    private final CounselingRepository repo;

    public CounselingService(CounselingRepository repo) {
        this.repo = repo;
    }

    public List<Row> list(AppUser imam) {
        return repo.findByMasjidIdOrderBySessionAtDescIdDesc(masjidOf(imam)).stream().map(CounselingService::row).toList();
    }

    public List<Row> upcomingFollowUps(AppUser imam) {
        return repo.findByMasjidIdAndFollowUpOnGreaterThanEqualAndStatusNotOrderByFollowUpOnAsc(
                masjidOf(imam), LocalDate.now(), "CANCELLED").stream().map(CounselingService::row).toList();
    }

    public Detail get(AppUser imam, Long id) {
        return detail(find(imam, id));
    }

    public Detail create(AppUser imam, Input in) {
        CounselingSession s = new CounselingSession();
        s.setMasjidId(masjidOf(imam));
        s.setImamId(imam.getId());
        apply(s, in);
        return detail(repo.save(s));
    }

    public Detail update(AppUser imam, Long id, Input in) {
        CounselingSession s = find(imam, id);
        apply(s, in);
        return detail(repo.save(s));
    }

    public void delete(AppUser imam, Long id) {
        repo.delete(find(imam, id));
    }

    private CounselingSession find(AppUser imam, Long id) {
        return repo.findByIdAndMasjidId(id, masjidOf(imam))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));
    }

    static void apply(CounselingSession s, Input in) {
        String type = in.sessionType().trim().toUpperCase();
        if (!CounselingSession.TYPES.contains(type)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown session type.");
        String status = in.status() == null || in.status().isBlank() ? "SCHEDULED" : in.status().trim().toUpperCase();
        if (!CounselingSession.STATUSES.contains(status)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status.");
        s.setClientName(in.clientName().trim());
        s.setSessionType(type);
        s.setSessionAt(in.sessionAt());
        s.setDurationMin(in.durationMin());
        s.setNotes(in.notes() == null || in.notes().isBlank() ? null : in.notes());
        s.setFollowUpOn(in.followUpOn());
        s.setStatus(status);
    }

    private static Row row(CounselingSession s) {
        return new Row(s.getId(), s.getClientName(), s.getSessionType(), s.getSessionAt(), s.getDurationMin(),
                s.getFollowUpOn(), s.getStatus());
    }

    private static Detail detail(CounselingSession s) {
        return new Detail(s.getId(), s.getClientName(), s.getSessionType(), s.getSessionAt(), s.getDurationMin(),
                s.getNotes(), s.getFollowUpOn(), s.getStatus());
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }
}
