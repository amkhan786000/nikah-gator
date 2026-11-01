package org.nikahgator.letters;

import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.letters.LetterDtos.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig); ServiceGuard also requires the General letters service to be enabled. */
@RestController
@RequestMapping("/api/imam/svc/letters")
public class LetterController {

    private final LetterService service;

    public LetterController(LetterService service) {
        this.service = service;
    }

    @GetMapping("/types")
    public Catalog types() {
        return service.catalog();
    }

    @PostMapping("/preview")
    public Preview preview(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody PreviewRequest body) {
        return service.preview(imam, body);
    }

    @GetMapping
    public List<View> list(@AuthenticationPrincipal AppUser imam) {
        return service.list(imam);
    }

    @PostMapping
    public View create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Save body) {
        return service.create(imam, body);
    }

    @GetMapping("/{id}")
    public View get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PutMapping("/{id}")
    public View update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Save body) {
        return service.update(imam, id, body);
    }

    @PostMapping("/{id}/void")
    public View voidIt(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody VoidRequest body) {
        return service.voidLetter(imam, id, body.reason());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        LetterService.Pdf pdf = service.pdf(imam, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.bytes());
    }
}
