package org.nikahgator.staff;

import java.util.List;
import java.util.Map;

import org.nikahgator.entity.AppUser;
import org.nikahgator.staff.StaffDtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** The imam only (see SecurityConfig). */
@RestController
@RequestMapping("/api/imam/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<StaffView> list(@AuthenticationPrincipal AppUser imam) {
        return service.list(imam);
    }

    @GetMapping("/{id}")
    public StaffView get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PostMapping
    public StaffView create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody CreateStaff req) {
        return service.create(imam, req);
    }

    @PutMapping("/{id}")
    public StaffView update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody UpdateStaff req) {
        return service.update(imam, id, req);
    }

    @PatchMapping("/{id}/active")
    public StaffView setActive(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return service.setActive(imam, id, Boolean.TRUE.equals(body.get("active")));
    }

    @PostMapping("/{id}/resend-credentials")
    public StaffView resendCredentials(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.resendCredentials(imam, id);
    }
}
