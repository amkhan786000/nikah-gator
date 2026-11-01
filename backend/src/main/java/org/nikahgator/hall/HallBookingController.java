package org.nikahgator.hall;

import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.hall.HallDtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (SecurityConfig) and switched on per masjid (ServiceGuard). */
@RestController
@RequestMapping("/api/imam/svc/hall-booking")
public class HallBookingController {
    private final HallBookingService service;

    public HallBookingController(HallBookingService service) {
        this.service = service;
    }

    /** Optional ?month=yyyy-MM limits the list to bookings starting in that month. */
    @GetMapping
    public List<View> list(@AuthenticationPrincipal AppUser imam, @RequestParam(required = false) String month) {
        return service.list(imam, month);
    }

    @GetMapping("/{id}")
    public View get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PostMapping
    public View create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Input body) {
        return service.create(imam, body);
    }

    @PutMapping("/{id}")
    public View update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Input body) {
        return service.update(imam, id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        service.delete(imam, id);
    }
}
