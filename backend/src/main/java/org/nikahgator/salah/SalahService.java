package org.nikahgator.salah;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.nikahgator.catalog.ServiceEntitlements;
import org.nikahgator.catalog.ServiceKey;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.salah.SalahDtos.*;
import org.nikahgator.salah.SalahRanges.Range;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** The masjid's timetable: periods of dates with one set of times each, plus how it is published. */
@Service
public class SalahService {
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> UNITS = Set.of("DAYS", "WEEKS", "MONTHS");

    private final SalahPeriodRepository periods;
    private final SalahSettingsRepository settingsRepo;
    private final MasjidRepository masjids;
    private final AppUserRepository users;
    private final ServiceEntitlements entitlements;

    public SalahService(SalahPeriodRepository periods, SalahSettingsRepository settingsRepo, MasjidRepository masjids,
                        AppUserRepository users, ServiceEntitlements entitlements) {
        this.periods = periods;
        this.settingsRepo = settingsRepo;
        this.masjids = masjids;
        this.users = users;
        this.entitlements = entitlements;
    }

    // ---------------- imam ----------------

    @Transactional(readOnly = true)
    public Overview overview(AppUser user) {
        Long mid = user.getMasjidId();
        Masjid masjid = masjid(mid);
        SalahSettings s = settings(mid);
        LocalDate today = today(masjid);
        List<SalahPeriod> all = periods.findByMasjidIdOrderByStartDate(mid);
        LocalDate covered = all.stream().map(SalahPeriod::getEndDate).max(LocalDate::compareTo).orElse(null);
        SalahPeriod last = all.isEmpty() ? null : all.get(all.size() - 1);
        LocalDate nextStart = covered == null || covered.isBefore(today) ? today : covered.plusDays(1);
        LocalDate nextEnd = SalahRanges.suggestedEnd(nextStart, s.getChangeUnit(), s.getChangeEvery());
        boolean todayCovered = all.stream().anyMatch(p -> !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today));
        Long daysLeft = covered == null ? null : java.time.temporal.ChronoUnit.DAYS.between(today, covered);
        return new Overview(settingsDto(s), all.stream().map(p -> dto(p, today)).toList(), masjid.getSlug(), masjid.getTimezone(),
                today, covered, daysLeft, todayCovered, nextStart, nextEnd, last == null ? null : dto(last, today));
    }

    @Transactional
    public SettingsDto saveSettings(AppUser user, SettingsDto req) {
        if (!UNITS.contains(req.changeUnit())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose days, weeks or months.");
        }
        if (req.changeEvery() < 1 || req.changeEvery() > 366) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "\"Every\" must be between 1 and 366.");
        }
        if (req.publicDays() < 1 || req.publicDays() > 31) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The link can show between 1 and 31 days.");
        }
        SalahSettings s = settings(user.getMasjidId());
        s.setChangeUnit(req.changeUnit());
        s.setChangeEvery(req.changeEvery());
        s.setPublicEnabled(req.publicEnabled());
        s.setPublicDays(req.publicDays());
        s.setShowSunrise(req.showSunrise());
        return settingsDto(settingsRepo.save(s));
    }

    @Transactional
    public PeriodDto create(AppUser user, PeriodRequest req) {
        SalahPeriod p = new SalahPeriod();
        p.setMasjidId(user.getMasjidId());
        apply(p, req);
        carve(user.getMasjidId(), p.getStartDate(), p.getEndDate(), null);
        return dto(periods.save(p), today(masjid(user.getMasjidId())));
    }

    @Transactional
    public PeriodDto update(AppUser user, Long id, PeriodRequest req) {
        SalahPeriod p = periods.findByIdAndMasjidId(id, user.getMasjidId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Period not found"));
        apply(p, req);
        carve(user.getMasjidId(), p.getStartDate(), p.getEndDate(), p.getId());
        return dto(periods.save(p), today(masjid(user.getMasjidId())));
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        SalahPeriod p = periods.findByIdAndMasjidId(id, user.getMasjidId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Period not found"));
        periods.delete(p);
    }

    /** Reads a CSV; with {@code commit} it also saves the rows, replacing the days they cover. */
    @Transactional
    public ImportResult importCsv(AppUser user, MultipartFile file, boolean commit) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a CSV file.");
        }
        if (file.getSize() > 1_000_000) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "The file is larger than 1 MB.");
        }
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file could not be read.");
        }
        SalahCsv.Result parsed = SalahCsv.parse(text);
        List<ImportRow> rows = parsed.rows().stream().map(r -> new ImportRow(r.line(), r.start(), r.end(), hhmm(r.fajr()),
                hhmm(r.sunrise()), hhmm(r.dhuhr()), hhmm(r.asr()), hhmm(r.maghrib()), hhmm(r.isha()), hhmm(r.jumuah()), r.notes())).toList();
        if (!parsed.errors().isEmpty()) {
            return new ImportResult(false, rows.size(), 0, rows, parsed.errors());
        }
        Long mid = user.getMasjidId();
        int replaced = 0;
        for (SalahCsv.Row r : parsed.rows()) {
            replaced += periods.findByMasjidIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDate(mid, r.start(), r.end()).size();
        }
        if (!commit) {
            return new ImportResult(false, rows.size(), replaced, rows, List.of());
        }
        for (SalahCsv.Row r : parsed.rows()) {
            carve(mid, r.start(), r.end(), null);
            SalahPeriod p = new SalahPeriod();
            p.setMasjidId(mid);
            p.setStartDate(r.start());
            p.setEndDate(r.end());
            p.setFajr(r.fajr());
            p.setSunrise(r.sunrise());
            p.setDhuhr(r.dhuhr());
            p.setAsr(r.asr());
            p.setMaghrib(r.maghrib());
            p.setIsha(r.isha());
            p.setJumuah(r.jumuah());
            p.setNotes(r.notes());
            periods.save(p);
        }
        return new ImportResult(true, rows.size(), replaced, rows, List.of());
    }

    public static String templateCsv() {
        return "start date,end date,fajr,sunrise,dhuhr,asr,maghrib,isha,jumuah,notes\n"
                + "2026-11-01,2026-11-10,5:41 AM,6:58 AM,12:05 PM,2:45 PM,5:10 PM,6:30 PM,1:15 PM,\n"
                + "2026-11-11,2026-11-20,5:48 AM,7:04 AM,12:07 PM,2:42 PM,5:05 PM,6:26 PM,1:15 PM,\n"
                + "# One row per range of days. For daily times use a single \"date\" column instead of start/end.\n";
    }

    // ---------------- public ----------------

    /** The link people open: today and the next days the imam configured. 404 when it is off or not available. */
    @Transactional(readOnly = true)
    public PublicTimes publicTimes(String slug) {
        Masjid masjid = masjids.findBySlug(slug == null ? "" : slug.toLowerCase(Locale.ROOT))
                .filter(Masjid::isActive).orElseThrow(this::notAvailable);
        AppUser imam = users.findFirstByMasjidIdAndRole(masjid.getId(), AppUser.IMAM).orElseThrow(this::notAvailable);
        if (!entitlements.enabledFor(imam).contains(ServiceKey.SALAH_TIMES)) {
            throw notAvailable();
        }
        SalahSettings s = settings(masjid.getId());
        if (!s.isPublicEnabled()) {
            throw notAvailable();
        }
        ZoneId zone = zone(masjid);
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = now.toLocalDate();
        LocalDate last = today.plusDays(Math.max(1, s.getPublicDays()) - 1L);
        List<SalahPeriod> found = periods.findByMasjidIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDate(masjid.getId(), today, last);
        List<PublicDay> days = new ArrayList<>();
        for (LocalDate d = today; !d.isAfter(last); d = d.plusDays(1)) {
            final LocalDate day = d;
            SalahPeriod p = found.stream().filter(x -> !x.getStartDate().isAfter(day) && !x.getEndDate().isBefore(day)).findFirst().orElse(null);
            String weekday = d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            boolean friday = d.getDayOfWeek() == DayOfWeek.FRIDAY;
            days.add(p == null
                    ? new PublicDay(d, weekday, d.equals(today), false, null, null, null, null, null, null, null, null)
                    : new PublicDay(d, weekday, d.equals(today), true, hhmm(p.getFajr()), s.isShowSunrise() ? hhmm(p.getSunrise()) : null,
                            hhmm(p.getDhuhr()), hhmm(p.getAsr()), hhmm(p.getMaghrib()), hhmm(p.getIsha()),
                            friday ? hhmm(p.getJumuah()) : null, p.getNotes()));
        }
        String address = java.util.stream.Stream.of(masjid.getAddressLine(), masjid.getCity(), masjid.getState(), masjid.getZip())
                .filter(x -> x != null && !x.isBlank()).collect(java.util.stream.Collectors.joining(", "));
        return new PublicTimes(masjid.getName(), address, masjid.getPhone(), zone.getId(), today, now.format(HHMM), s.isShowSunrise(), days);
    }

    // ---------------- helpers ----------------

    private ApiException notAvailable() {
        return new ApiException(HttpStatus.NOT_FOUND, "Prayer times are not available.");
    }

    /** Gives [start, end] to the period being saved: other periods lose those days (split or removed). */
    private void carve(Long masjidId, LocalDate start, LocalDate end, Long exceptId) {
        for (SalahPeriod old : periods.findByMasjidIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDate(masjidId, start, end)) {
            if (old.getId().equals(exceptId)) {
                continue;
            }
            List<Range> keep = SalahRanges.remainder(old.getStartDate(), old.getEndDate(), start, end);
            if (keep.isEmpty()) {
                periods.delete(old);
                continue;
            }
            old.setStartDate(keep.get(0).start());
            old.setEndDate(keep.get(0).end());
            periods.save(old);
            if (keep.size() > 1) { // the new period landed in the middle: keep the tail as its own period
                SalahPeriod tail = new SalahPeriod();
                tail.setMasjidId(masjidId);
                tail.setStartDate(keep.get(1).start());
                tail.setEndDate(keep.get(1).end());
                tail.setFajr(old.getFajr());
                tail.setSunrise(old.getSunrise());
                tail.setDhuhr(old.getDhuhr());
                tail.setAsr(old.getAsr());
                tail.setMaghrib(old.getMaghrib());
                tail.setIsha(old.getIsha());
                tail.setJumuah(old.getJumuah());
                tail.setNotes(old.getNotes());
                periods.save(tail);
            }
        }
        periods.flush();
    }

    private void apply(SalahPeriod p, PeriodRequest req) {
        if (req.endDate().isBefore(req.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The end date is before the start date.");
        }
        if (SalahRanges.days(req.startDate(), req.endDate()) > 366) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A period can be at most a year long.");
        }
        LocalTime fajr = time("Fajr", req.fajr(), true), sunrise = time("Sunrise", req.sunrise(), false),
                dhuhr = time("Dhuhr", req.dhuhr(), true), asr = time("Asr", req.asr(), true),
                maghrib = time("Maghrib", req.maghrib(), true), isha = time("Isha", req.isha(), true),
                jumuah = time("Jumuah", req.jumuah(), false);
        String order = SalahCsv.orderProblem(fajr, sunrise, dhuhr, asr, maghrib, isha);
        if (order != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The times are not in order (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha).");
        }
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setFajr(fajr);
        p.setSunrise(sunrise);
        p.setDhuhr(dhuhr);
        p.setAsr(asr);
        p.setMaghrib(maghrib);
        p.setIsha(isha);
        p.setJumuah(jumuah);
        p.setNotes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim());
    }

    private static LocalTime time(String name, String s, boolean required) {
        if (s == null || s.isBlank()) {
            if (required) {
                throw new ApiException(HttpStatus.BAD_REQUEST, name + " time is required.");
            }
            return null;
        }
        try {
            return LocalTime.parse(s.trim().length() == 4 ? "0" + s.trim() : s.trim());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, name + " time must look like 05:41.");
        }
    }

    private SalahSettings settings(Long masjidId) {
        return settingsRepo.findById(masjidId).orElseGet(() -> {
            SalahSettings s = new SalahSettings();
            s.setMasjidId(masjidId);
            return s;
        });
    }

    private Masjid masjid(Long id) {
        return masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
    }

    private static ZoneId zone(Masjid m) {
        try {
            return ZoneId.of(m.getTimezone());
        } catch (Exception e) {
            return ZoneId.of("America/New_York");
        }
    }

    private static LocalDate today(Masjid m) {
        return LocalDate.now(zone(m));
    }

    private static String hhmm(LocalTime t) {
        return t == null ? null : t.format(HHMM);
    }

    private static SettingsDto settingsDto(SalahSettings s) {
        return new SettingsDto(s.getChangeUnit(), s.getChangeEvery(), s.isPublicEnabled(), s.getPublicDays(), s.isShowSunrise());
    }

    private static PeriodDto dto(SalahPeriod p, LocalDate today) {
        return new PeriodDto(p.getId(), p.getStartDate(), p.getEndDate(), hhmm(p.getFajr()), hhmm(p.getSunrise()), hhmm(p.getDhuhr()),
                hhmm(p.getAsr()), hhmm(p.getMaghrib()), hhmm(p.getIsha()), hhmm(p.getJumuah()), p.getNotes(),
                !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today));
    }
}
