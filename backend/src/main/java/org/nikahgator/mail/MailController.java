package org.nikahgator.mail;

import java.time.LocalDate;

import org.nikahgator.entity.AppUser;
import org.nikahgator.mail.MailDtos.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Sending a document by e-mail (imam, or staff with Manage on the service: ServiceGuard treats POST as a change),
 * and the e-mail log. The log is imam-only and the platform admin's copy is read-only; see SecurityConfig.
 */
@RestController
public class MailController {
    private final DocumentMailer mailer;
    private final EmailLogService logs;

    public MailController(DocumentMailer mailer, EmailLogService logs) {
        this.mailer = mailer;
        this.logs = logs;
    }

    @PostMapping("/api/imam/nikahs/{id}/certificate/email")
    public Outcome emailNikahCertificate(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody SendRequest body) {
        return mailer.send(imam, "nikah-certificate", id, body.to(), body.message());
    }

    @PostMapping("/api/imam/svc/{slug:juma|community-service|janazah|shahada|baby-naming|letters|divorce}/{id}/email")
    public Outcome emailDocument(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @PathVariable Long id,
            @Valid @RequestBody SendRequest body) {
        return mailer.send(imam, slug, id, body.to(), body.message());
    }

    @GetMapping("/api/imam/email-log")
    public LogPage imamLog(@AuthenticationPrincipal AppUser imam,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String recipient, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return logs.imamList(imam, from, to, recipient, kind, status, page, size);
    }

    @GetMapping("/api/imam/email-log/{id}")
    public LogDetail imamLogEntry(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return logs.imamGet(imam, id);
    }

    @PostMapping("/api/imam/email-log/{id}/resend")
    public Outcome resend(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return mailer.resend(imam, id);
    }

    @GetMapping("/api/admin/email-log")
    public LogPage adminLog(@RequestParam(required = false) Long masjidId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String recipient, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return logs.adminList(masjidId, from, to, recipient, kind, status, page, size);
    }

    @GetMapping("/api/admin/email-log/{id}")
    public LogDetail adminLogEntry(@PathVariable Long id) {
        return logs.adminGet(id);
    }
}
