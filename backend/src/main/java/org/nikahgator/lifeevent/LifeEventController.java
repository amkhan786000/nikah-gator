package org.nikahgator.lifeevent;

import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.lifeevent.LifeEventDtos.Input;
import org.nikahgator.lifeevent.LifeEventDtos.View;
import org.nikahgator.lifeevent.LifeEventDtos.VoidRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Janazah, shahada and baby-naming services share one API shape: /api/imam/svc/{janazah|shahada|baby-naming}/...
 * Imam only (SecurityConfig); ServiceGuard checks the service is switched on. Nothing here is public.
 */
@RestController
@RequestMapping("/api/imam/svc/{slug:janazah|shahada|baby-naming}")
public class LifeEventController {
    private final LifeEventService service;

    public LifeEventController(LifeEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<View> list(@AuthenticationPrincipal AppUser imam, @PathVariable String slug) {
        return service.list(imam, type(slug));
    }

    @PostMapping
    public View create(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @Valid @RequestBody Input body) {
        return service.create(imam, type(slug), body);
    }

    @GetMapping("/{id}")
    public View get(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @PathVariable Long id) {
        return service.get(imam, type(slug), id);
    }

    @PutMapping("/{id}")
    public View update(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @PathVariable Long id,
            @Valid @RequestBody Input body) {
        return service.update(imam, type(slug), id, body);
    }

    @PostMapping("/{id}/void")
    public View voidIt(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @PathVariable Long id,
            @Valid @RequestBody VoidRequest body) {
        return service.voidRecord(imam, type(slug), id, body.reason());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUser imam, @PathVariable String slug, @PathVariable Long id) {
        LifeEventService.Pdf pdf = service.pdf(imam, type(slug), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.bytes());
    }

    private static LifeEventType type(String slug) {
        return LifeEventType.bySlug(slug).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown service"));
    }
}
