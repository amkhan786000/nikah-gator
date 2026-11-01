package org.nikahgator.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.nikahgator.entity.Notification;
import org.nikahgator.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Sends due notifications. A row is claimed with one atomic UPDATE before sending, so it never goes out twice. */
@Service
public class ReminderDispatcher {
    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);
    /** Minutes to wait before the 2nd and 3rd attempt. */
    private static final int[] BACKOFF_MINUTES = {5, 15, 60};

    private final NotificationRepository notifications;
    private final EmailService email;

    @Value("${app.reminders.enabled:true}")
    private boolean enabled;

    @Value("${app.reminders.max-attempts:3}")
    private int maxAttempts;

    public ReminderDispatcher(NotificationRepository notifications, EmailService email) {
        this.notifications = notifications;
        this.email = email;
    }

    public static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    @Scheduled(fixedDelayString = "${app.reminders.interval-ms:300000}", initialDelayString = "${app.reminders.initial-delay-ms:30000}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            sendDue();
        } catch (Exception e) {
            log.error("Reminder run failed: {}", e.getMessage());
        }
    }

    /** @return how many e-mails were sent */
    public int sendDue() {
        notifications.releaseStale(utcNow().minusMinutes(15));
        List<Notification> due = notifications.findTop50ByChannelAndStatusAndSendAtLessThanEqualOrderBySendAtAsc(
                "EMAIL", Notification.PENDING, utcNow());
        int sent = 0;
        for (Notification n : due) {
            if (process(n.getId())) {
                sent++;
            }
        }
        if (!due.isEmpty()) {
            log.info("Reminders: {} due, {} sent", due.size(), sent);
        }
        return sent;
    }

    /** Claims and sends one notification. @return true when it was sent */
    public boolean process(Long id) {
        if (notifications.claim(id, utcNow()) != 1) {
            return false;
        }
        Notification n = notifications.findById(id).orElse(null);
        if (n == null) {
            return false;
        }
        boolean ok = false;
        String error = null;
        try {
            ok = email.send(n.getRecipient(), n.getSubject(), n.getBody());
            if (!ok) {
                error = "The e-mail could not be sent.";
            }
        } catch (Exception e) {
            error = "The e-mail could not be sent.";
            log.error("Reminder {} failed: {}", id, e.getMessage());
        }
        LocalDateTime now = utcNow();
        if (ok) {
            n.setStatus(Notification.SENT);
            n.setSentAt(now);
            n.setError(null);
        } else if (n.getAttempts() >= maxAttempts) {
            n.setStatus(Notification.FAILED);
            n.setError(error);
        } else {
            n.setStatus(Notification.PENDING);
            n.setSendAt(now.plusMinutes(BACKOFF_MINUTES[Math.min(n.getAttempts() - 1, BACKOFF_MINUTES.length - 1)]));
            n.setError(error + " Will try again.");
        }
        notifications.save(n);
        return ok;
    }
}
