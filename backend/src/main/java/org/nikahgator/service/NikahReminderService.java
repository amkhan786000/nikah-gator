package org.nikahgator.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.nikahgator.dto.ReminderDtos.NotificationView;
import org.nikahgator.dto.ReminderDtos.ReminderSettings;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.MasjidReminderSettings;
import org.nikahgator.entity.Notification;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidReminderSettingsRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahReminderQueries;
import org.nikahgator.repository.NotificationRepository;
import org.nikahgator.service.ReminderMessages.Message;
import org.nikahgator.service.ReminderPlanner.Audience;
import org.nikahgator.service.ReminderPlanner.Planned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Schedules the reminder e-mails of a nikah and lets the imam follow, send and cancel them. Every access is limited to the imam's masjid. */
@Service
public class NikahReminderService {
    private static final Logger log = LoggerFactory.getLogger(NikahReminderService.class);
    private static final String CONFIRMATION = "CONFIRMATION";

    private final NotificationRepository notifications;
    private final NikahReminderQueries nikahs;
    private final MasjidRepository masjids;
    private final MasjidReminderSettingsRepository settings;
    private final AppUserRepository users;
    private final ReminderDispatcher dispatcher;

    public NikahReminderService(NotificationRepository notifications, NikahReminderQueries nikahs, MasjidRepository masjids,
                                MasjidReminderSettingsRepository settings, AppUserRepository users, ReminderDispatcher dispatcher) {
        this.notifications = notifications;
        this.nikahs = nikahs;
        this.masjids = masjids;
        this.settings = settings;
        this.users = users;
        this.dispatcher = dispatcher;
    }

    // ---- scheduling (called by NikahService after a nikah is saved) ----

    /**
     * Brings the pending reminders of a nikah in line with its current date, status and e-mail addresses.
     * Never throws: a problem here must not stop the imam from saving a nikah.
     *
     * @param confirm also e-mail the couple that the nikah is (re)scheduled
     */
    public void sync(NikahRequest n, boolean confirm, boolean rescheduled) {
        try {
            doSync(n, confirm, rescheduled);
        } catch (Exception e) {
            log.error("Could not schedule reminders for nikah {}: {}", n.getId(), e.getMessage(), e);
        }
    }

    private void doSync(NikahRequest n, boolean confirm, boolean rescheduled) {
        Masjid masjid = masjids.findById(n.getMasjidId()).orElse(null);
        if (masjid == null) {
            return;
        }
        MasjidReminderSettings cfg = settings.findById(masjid.getId()).orElseGet(MasjidReminderSettings::new);
        boolean active = NikahRequest.REGISTERED.equals(n.getStatus()) && n.getScheduledAt() != null;
        ZoneId zone = zone(masjid);
        Instant now = Instant.now();

        List<Wanted> wanted = new ArrayList<>();
        String imamEmail = null;
        String imamName = null;
        if (active) {
            AppUser imam = imamOf(n);
            if (imam != null) {
                imamEmail = imam.getEmail();
                imamName = imam.getFullName();
            }
            for (Planned p : ReminderPlanner.plan(n.getScheduledAt(), zone, now, cfg.isRemindersEnabled(), cfg.getReminderDaysBefore())) {
                if (p.audience() == Audience.COUPLE) {
                    Message msg = ReminderMessages.couple(masjid, n, p.daysBefore());
                    for (String to : coupleEmails(n)) {
                        wanted.add(new Wanted(p.kind(), to, msg, p.sendAt()));
                    }
                } else if (imamEmail != null && !imamEmail.isBlank()) {
                    wanted.add(new Wanted(p.kind(), imamEmail, ReminderMessages.imam(masjid, n, imamName), p.sendAt()));
                }
            }
        }

        List<Notification> existing = notifications.findByRequestId(n.getId());
        List<Notification> toSave = new ArrayList<>();
        // pending rows that no longer match are cancelled; matching ones are kept
        List<Wanted> missing = new ArrayList<>(wanted);
        for (Notification e : existing) {
            if (!Notification.PENDING.equals(e.getStatus())) {
                continue;
            }
            if (CONFIRMATION.equals(e.getKind())) {
                // an unsent confirmation is dropped when the nikah is off, or replaced by a newer one
                if (!active || confirm) {
                    e.setStatus(Notification.CANCELLED);
                    toSave.add(e);
                }
                continue;
            }
            Wanted match = missing.stream().filter(w -> w.matchesPending(e)).findFirst().orElse(null);
            if (match != null) {
                missing.remove(match);
            } else {
                e.setStatus(Notification.CANCELLED);
                toSave.add(e);
            }
        }
        // something already sent for the same kind, address and time is not sent again
        missing.removeIf(w -> existing.stream().anyMatch(e -> Notification.SENT.equals(e.getStatus()) && w.matchesSent(e)));
        for (Wanted w : missing) {
            toSave.add(row(n, w.kind, w.to, w.msg, w.sendAt));
        }

        List<Notification> confirmations = new ArrayList<>();
        if (confirm && active) {
            Message msg = ReminderMessages.confirmation(masjid, n, rescheduled);
            for (String to : coupleEmails(n)) {
                confirmations.add(row(n, CONFIRMATION, to, msg, now));
            }
        }
        toSave.addAll(confirmations);
        notifications.saveAll(toSave);

        // the confirmation goes out right away, without making the imam wait for the mail server
        for (Notification c : confirmations) {
            CompletableFuture.runAsync(() -> {
                try {
                    dispatcher.process(c.getId());
                } catch (Exception e) {
                    log.error("Confirmation {} failed: {}", c.getId(), e.getMessage());
                }
            });
        }
    }

    private record Wanted(String kind, String to, Message msg, Instant sendAt) {
        boolean matchesSent(Notification e) {
            return Objects.equals(kind, e.getKind()) && to.equalsIgnoreCase(e.getRecipient()) && sendAt.equals(utc(e.getSendAt()));
        }

        boolean matchesPending(Notification e) {
            return matchesSent(e) && Objects.equals(msg.subject(), e.getSubject()) && Objects.equals(msg.body(), e.getBody());
        }
    }

    private Notification row(NikahRequest n, String kind, String to, Message msg, Instant sendAt) {
        Notification r = new Notification();
        r.setMasjidId(n.getMasjidId());
        r.setRequestId(n.getId());
        r.setKind(kind);
        r.setChannel("EMAIL");
        r.setRecipient(to);
        r.setSubject(msg.subject());
        r.setBody(msg.body());
        r.setSendAt(LocalDateTime.ofInstant(sendAt, ZoneOffset.UTC));
        return r;
    }

    private AppUser imamOf(NikahRequest n) {
        AppUser imam = n.getImamId() == null ? null : users.findById(n.getImamId()).orElse(null);
        if (imam == null || !Objects.equals(imam.getMasjidId(), n.getMasjidId())) {
            imam = users.findFirstByMasjidIdAndRole(n.getMasjidId(), AppUser.IMAM).orElse(null);
        }
        return imam;
    }

    private static Set<String> coupleEmails(NikahRequest n) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> lower = new LinkedHashSet<>();
        for (String e : new String[] {n.getGroomEmail(), n.getBrideEmail()}) {
            if (e != null && !e.isBlank() && lower.add(e.trim().toLowerCase(Locale.ROOT))) {
                seen.add(e.trim());
            }
        }
        return seen;
    }

    static ZoneId zone(Masjid m) {
        try {
            return ZoneId.of(m.getTimezone());
        } catch (Exception e) {
            return ZoneId.of("America/New_York");
        }
    }

    private static Instant utc(LocalDateTime t) {
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }

    // ---- imam API ----

    public ReminderSettings getSettings(AppUser imam) {
        MasjidReminderSettings s = settings.findById(masjidOf(imam))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        return new ReminderSettings(s.isRemindersEnabled(), s.getReminderDaysBefore());
    }

    /** Saves the settings and re-plans the reminders of the masjid's upcoming nikahs. */
    public ReminderSettings saveSettings(AppUser imam, ReminderSettings in) {
        Long masjidId = masjidOf(imam);
        MasjidReminderSettings s = settings.findById(masjidId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        s.setRemindersEnabled(in.enabled());
        s.setReminderDaysBefore(in.daysBefore());
        settings.save(s);
        for (NikahRequest n : nikahs.findByMasjidIdAndStatusAndScheduledAtAfter(masjidId, NikahRequest.REGISTERED, LocalDateTime.now().minusDays(1))) {
            sync(n, false, false);
        }
        return getSettings(imam);
    }

    public List<NotificationView> list(AppUser imam, Long nikahId) {
        Long masjidId = masjidOf(imam);
        nikah(masjidId, nikahId);
        return notifications.findByRequestIdAndMasjidIdOrderBySendAtAscIdAsc(nikahId, masjidId).stream()
                .map(NikahReminderService::view).toList();
    }

    /** Sends a pending reminder now, or tries a failed one again. */
    public NotificationView sendNow(AppUser imam, Long nikahId, Long id) {
        Notification n = own(imam, nikahId, id);
        if (Notification.FAILED.equals(n.getStatus())) {
            n.setStatus(Notification.PENDING);
            n.setAttempts(0);
            notifications.save(n);
        } else if (!Notification.PENDING.equals(n.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This message was already sent or cancelled.");
        }
        dispatcher.process(id);
        return view(notifications.findById(id).orElseThrow());
    }

    public NotificationView cancel(AppUser imam, Long nikahId, Long id) {
        own(imam, nikahId, id);
        if (notifications.cancel(id, masjidOf(imam)) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a scheduled or failed message can be cancelled.");
        }
        return view(notifications.findById(id).orElseThrow());
    }

    private Notification own(AppUser imam, Long nikahId, Long id) {
        Long masjidId = masjidOf(imam);
        nikah(masjidId, nikahId);
        Notification n = notifications.findById(id).orElse(null);
        if (n == null || !Objects.equals(n.getMasjidId(), masjidId) || !Objects.equals(n.getRequestId(), nikahId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Message not found");
        }
        return n;
    }

    private NikahRequest nikah(Long masjidId, Long id) {
        return nikahs.findByIdAndMasjidId(id, masjidId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Nikah not found"));
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        }
        return imam.getMasjidId();
    }

    private static NotificationView view(Notification n) {
        return new NotificationView(n.getId(), n.getKind(), n.getChannel(), n.getRecipient(), n.getSubject(), n.getStatus(),
                utc(n.getSendAt()), utc(n.getSentAt()), n.getError(), n.getAttempts());
    }
}
