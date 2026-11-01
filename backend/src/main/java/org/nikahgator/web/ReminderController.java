package org.nikahgator.web;

import java.util.List;

import org.nikahgator.dto.ReminderDtos.NotificationView;
import org.nikahgator.dto.ReminderDtos.ReminderSettings;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.NikahReminderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig, /api/imam/**): reminders of the imam's own masjid. */
@RestController
@RequestMapping("/api/imam")
public class ReminderController {

    private final NikahReminderService service;

    public ReminderController(NikahReminderService service) {
        this.service = service;
    }

    @GetMapping("/masjid/reminders")
    public ReminderSettings settings(@AuthenticationPrincipal AppUser imam) {
        return service.getSettings(imam);
    }

    @PutMapping("/masjid/reminders")
    public ReminderSettings saveSettings(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody ReminderSettings body) {
        return service.saveSettings(imam, body);
    }

    @GetMapping("/nikahs/{id}/notifications")
    public List<NotificationView> list(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.list(imam, id);
    }

    @PostMapping("/nikahs/{id}/notifications/{nid}/send-now")
    public NotificationView sendNow(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long nid) {
        return service.sendNow(imam, id, nid);
    }

    @PostMapping("/nikahs/{id}/notifications/{nid}/cancel")
    public NotificationView cancel(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long nid) {
        return service.cancel(imam, id, nid);
    }
}
