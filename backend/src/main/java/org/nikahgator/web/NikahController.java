package org.nikahgator.web;

import java.util.List;

import org.nikahgator.dto.NikahDto;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.NikahService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig): nikahs of the imam's own masjid. */
@RestController
@RequestMapping("/api/imam/nikahs")
public class NikahController {

    private final NikahService service;

    public NikahController(NikahService service) {
        this.service = service;
    }

    @GetMapping
    public List<NikahDto> list(@AuthenticationPrincipal AppUser imam) {
        return service.list(imam);
    }

    @GetMapping("/{id}")
    public NikahDto get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PostMapping
    public NikahDto create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody NikahDto body) {
        return service.create(imam, body);
    }

    @PutMapping("/{id}")
    public NikahDto update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody NikahDto body) {
        return service.update(imam, id, body);
    }
}
