package org.nikahgator.hall;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.hall.HallDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every query is limited to the imam's own masjid. Two CONFIRMED bookings of one space may never overlap. */
@Service
public class HallBookingService {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private final HallBookingRepository repo;

    public HallBookingService(HallBookingRepository repo) {
        this.repo = repo;
    }

    /** All bookings, or only those starting in the given month when {@code month} (yyyy-MM) is set. */
    public List<View> list(AppUser imam, String month) {
        Long masjid = masjidOf(imam);
        if (month == null || month.isBlank()) {
            return repo.findByMasjidIdOrderByStartsAtAscIdAsc(masjid).stream().map(HallBookingService::view).toList();
        }
        LocalDate first;
        try {
            first = LocalDate.parse(month + "-01");
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must look like 2026-12.");
        }
        return repo.findByMasjidIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAscIdAsc(
                masjid, first.atStartOfDay(), first.plusMonths(1).atStartOfDay()).stream().map(HallBookingService::view).toList();
    }

    public View get(AppUser imam, Long id) {
        return view(find(imam, id));
    }

    @Transactional
    public View create(AppUser imam, Input in) {
        HallBooking b = new HallBooking();
        b.setMasjidId(masjidOf(imam));
        apply(b, in);
        ensureNoConflict(b);
        return view(repo.save(b));
    }

    @Transactional
    public View update(AppUser imam, Long id, Input in) {
        HallBooking b = find(imam, id);
        apply(b, in);
        ensureNoConflict(b);
        return view(repo.save(b));
    }

    public void delete(AppUser imam, Long id) {
        repo.delete(find(imam, id));
    }

    private void ensureNoConflict(HallBooking b) {
        if (!HallBooking.CONFIRMED.equals(b.getStatus())) return;
        HallBooking clash = HallOverlap.firstConflict(b,
                repo.findConfirmedInRange(b.getMasjidId(), b.getSpace(), b.getStartsAt(), b.getEndsAt()));
        if (clash != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot confirm: \"" + clash.getTitle() + "\" is already confirmed for "
                    + clash.getSpace() + " from " + WHEN.format(clash.getStartsAt()) + " to " + WHEN.format(clash.getEndsAt()) + ".");
        }
    }

    private HallBooking find(AppUser imam, Long id) {
        return repo.findByIdAndMasjidId(id, masjidOf(imam)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
    }

    static void apply(HallBooking b, Input in) {
        if (!in.endsAt().isAfter(in.startsAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "The end must be after the start.");
        String status = in.status() == null || in.status().isBlank() ? HallBooking.REQUESTED : in.status().trim().toUpperCase();
        if (!HallBooking.STATUSES.contains(status)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status.");
        b.setTitle(in.title().trim());
        b.setContactName(in.contactName().trim());
        b.setContactPhone(blankToNull(in.contactPhone()));
        b.setContactEmail(blankToNull(in.contactEmail()));
        b.setStartsAt(in.startsAt());
        b.setEndsAt(in.endsAt());
        b.setSpace(in.space() == null || in.space().isBlank() ? HallBooking.DEFAULT_SPACE : in.space().trim());
        b.setExpectedGuests(in.expectedGuests());
        b.setStatus(status);
        b.setFeeText(blankToNull(in.feeText()));
        b.setNotes(blankToNull(in.notes()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static View view(HallBooking b) {
        return new View(b.getId(), b.getTitle(), b.getContactName(), b.getContactPhone(), b.getContactEmail(), b.getStartsAt(),
                b.getEndsAt(), b.getSpace(), b.getExpectedGuests(), b.getStatus(), b.getFeeText(), b.getNotes());
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }
}
