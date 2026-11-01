package org.nikahgator.counseling;

import java.util.List;

import org.nikahgator.counseling.CounselingDtos.*;
import org.nikahgator.entity.AppUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (SecurityConfig) and switched on per masjid (ServiceGuard). Responses are never cached. */
@RestController
@RequestMapping("/api/imam/svc/counseling")
public class CounselingController {
    private final CounselingService service;

    public CounselingController(CounselingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Row>> list(@AuthenticationPrincipal AppUser imam) {
        return noStore(service.list(imam));
    }

    @GetMapping("/follow-ups")
    public ResponseEntity<List<Row>> followUps(@AuthenticationPrincipal AppUser imam) {
        return noStore(service.upcomingFollowUps(imam));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Detail> get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return noStore(service.get(imam, id));
    }

    @PostMapping
    public Detail create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Input body) {
        return service.create(imam, body);
    }

    @PutMapping("/{id}")
    public Detail update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Input body) {
        return service.update(imam, id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        service.delete(imam, id);
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
