package org.nikahgator.audit;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.nikahgator.audit.ActivityLogService.Filter;
import org.nikahgator.audit.ActivityLogService.FilterChoices;
import org.nikahgator.audit.ActivityLogService.PageResult;
import org.nikahgator.entity.AppUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only views of the activity log. The imam sees only their own masjid; the platform admin sees every masjid.
 * Staff cannot reach the imam endpoints at all (see SecurityConfig). Nothing here changes or deletes an entry.
 */
@RestController
public class ActivityController {
    private static final int EXPORT_MAX = 20_000;

    private final ActivityLogService service;

    public ActivityController(ActivityLogService service) {
        this.service = service;
    }

    // ---- the imam's masjid ---------------------------------------------------------------------------

    @GetMapping("/api/imam/activity")
    public PageResult imamList(@AuthenticationPrincipal AppUser imam, @RequestParam(required = false) LocalDate from,
                               @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long userId,
                               @RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
                               @RequestParam(required = false) Long entityId, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size) {
        return service.search(new Filter(masjidOf(imam), from, to, userId, action, entityType, entityId), page, size);
    }

    @GetMapping("/api/imam/activity/filters")
    public FilterChoices imamFilters(@AuthenticationPrincipal AppUser imam) {
        return service.choices(masjidOf(imam));
    }

    @GetMapping("/api/imam/activity/export")
    public ResponseEntity<byte[]> imamExport(@AuthenticationPrincipal AppUser imam, @RequestParam(required = false) LocalDate from,
                                             @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
                                             @RequestParam(required = false) Long entityId) {
        return csv(new Filter(masjidOf(imam), from, to, userId, action, entityType, entityId));
    }

    // ---- the platform admin: every masjid ------------------------------------------------------------

    @GetMapping("/api/admin/activity")
    public PageResult adminList(@RequestParam(required = false) Long masjidId, @RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long userId,
                                @RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
                                @RequestParam(required = false) Long entityId, @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "25") int size) {
        return service.search(new Filter(masjidId, from, to, userId, action, entityType, entityId), page, size);
    }

    @GetMapping("/api/admin/activity/filters")
    public FilterChoices adminFilters(@RequestParam(required = false) Long masjidId) {
        return service.choices(masjidId);
    }

    @GetMapping("/api/admin/activity/export")
    public ResponseEntity<byte[]> adminExport(@RequestParam(required = false) Long masjidId, @RequestParam(required = false) LocalDate from,
                                              @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
                                              @RequestParam(required = false) Long entityId) {
        return csv(new Filter(masjidId, from, to, userId, action, entityType, entityId));
    }

    private ResponseEntity<byte[]> csv(Filter f) {
        byte[] body = ("﻿" + ActivityLogService.toCsv(service.export(f, EXPORT_MAX))).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("activity-log.csv").build().toString())
                .body(body);
    }

    /** An imam without a masjid would otherwise see every masjid: refuse by scoping to an id that matches nothing. */
    private static Long masjidOf(AppUser imam) {
        return imam.getMasjidId() != null ? imam.getMasjidId() : -1L;
    }
}
