package org.nikahgator.service;

import java.util.List;

import org.nikahgator.dto.NikahDto;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Nikahs registered by an imam. Every query is limited to the imam's own masjid. */
@Service
public class NikahService {

    private final NikahRequestRepository nikahs;

    @org.springframework.beans.factory.annotation.Autowired // feature:reminders
    private NikahReminderService reminders;

    public NikahService(NikahRequestRepository nikahs) {
        this.nikahs = nikahs;
    }

    public List<NikahDto> list(AppUser imam) {
        return nikahs.findByMasjidIdOrderByScheduledAtDescIdDesc(masjidOf(imam)).stream().map(NikahService::toDto).toList();
    }

    public NikahDto get(AppUser imam, Long id) {
        return toDto(find(imam, id));
    }

    public NikahDto create(AppUser imam, NikahDto in) {
        NikahRequest n = new NikahRequest();
        n.setMasjidId(masjidOf(imam));
        n.setRequestedBy(imam.getId());
        n.setImamId(imam.getId());
        apply(n, in);
        NikahRequest saved = nikahs.save(n);
        reminders.sync(saved, true, false); // feature:reminders
        return toDto(saved);
    }

    public NikahDto update(AppUser imam, Long id, NikahDto in) {
        NikahRequest n = find(imam, id);
        java.time.LocalDateTime oldAt = n.getScheduledAt(); // feature:reminders
        apply(n, in);
        if (in.status() != null && java.util.List.of(NikahRequest.REGISTERED, NikahRequest.COMPLETED, NikahRequest.CANCELLED).contains(in.status())) {
            n.setStatus(in.status());
        }
        NikahRequest saved = nikahs.save(n);
        reminders.sync(saved, !java.util.Objects.equals(oldAt, saved.getScheduledAt()), true); // feature:reminders
        return toDto(saved);
    }

    private NikahRequest find(AppUser imam, Long id) {
        return nikahs.findByIdAndMasjidId(id, masjidOf(imam))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        return imam.getMasjidId();
    }

    private static void apply(NikahRequest n, NikahDto in) {
        n.setGroomName(in.groomName().trim());
        n.setGroomFatherName(blank(in.groomFatherName()));
        n.setGroomEmail(blank(in.groomEmail()));
        n.setGroomPhone(blank(in.groomPhone()));
        n.setBrideName(in.brideName().trim());
        n.setBrideFatherName(blank(in.brideFatherName()));
        n.setBrideEmail(blank(in.brideEmail()));
        n.setBridePhone(blank(in.bridePhone()));
        n.setWaliName(blank(in.waliName()));
        n.setWaliRelation(blank(in.waliRelation()));
        n.setWitness1Name(blank(in.witness1Name()));
        n.setWitness2Name(blank(in.witness2Name()));
        n.setMahrDescription(blank(in.mahrDescription()));
        n.setScheduledAt(in.nikahAt());
        n.setNotes(blank(in.notes()));
    }

    private static NikahDto toDto(NikahRequest n) {
        return new NikahDto(n.getId(), n.getStatus(), n.getGroomName(), n.getGroomFatherName(), n.getGroomEmail(),
                n.getGroomPhone(), n.getBrideName(), n.getBrideFatherName(), n.getBrideEmail(), n.getBridePhone(),
                n.getWaliName(), n.getWaliRelation(), n.getWitness1Name(), n.getWitness2Name(), n.getMahrDescription(),
                n.getScheduledAt(), n.getNotes(), n.getCreatedAt());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
