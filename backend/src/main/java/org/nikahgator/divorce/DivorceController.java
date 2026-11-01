package org.nikahgator.divorce;

import java.util.List;

import org.nikahgator.divorce.DivorceDtos.Input;
import org.nikahgator.divorce.DivorceDtos.View;
import org.nikahgator.divorce.DivorceDtos.VoidRequest;
import org.nikahgator.entity.AppUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (SecurityConfig); ServiceGuard requires the divorce service to be on. Sensitive: nothing here is public. */
@RestController
@RequestMapping("/api/imam/svc/divorce")
public class DivorceController {
    private final DivorceService service;

    public DivorceController(DivorceService service) {
        this.service = service;
    }

    @GetMapping
    public List<View> list(@AuthenticationPrincipal AppUser imam) {
        return service.list(imam);
    }

    @PostMapping
    public View create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Input body) {
        return service.create(imam, body);
    }

    /** The non-void divorce recorded for a nikah: 204 when there is none, 404 when the nikah is not of this masjid. */
    @GetMapping("/by-nikah/{nikahId}")
    public ResponseEntity<View> byNikah(@AuthenticationPrincipal AppUser imam, @PathVariable Long nikahId) {
        return service.byNikah(imam, nikahId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public View get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PutMapping("/{id}")
    public View update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Input body) {
        return service.update(imam, id, body);
    }

    @PostMapping("/{id}/void")
    public View voidIt(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody VoidRequest body) {
        return service.voidRecord(imam, id, body.reason());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        DivorceService.Pdf pdf = service.pdf(imam, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.bytes());
    }
}
