package org.nikahgator.web;

import org.nikahgator.dto.CertificateContent;
import org.nikahgator.dto.CertificateDto;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.CertificateService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig): certificates of nikahs in the imam's own masjid. */
@RestController
@RequestMapping("/api/imam/nikahs/{id}/certificate")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    @PostMapping
    public CertificateDto issue(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.issue(imam, id);
    }

    @GetMapping
    public CertificateDto get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    /** Resolved certificate text for the on-screen preview (watermarked until the nikah is completed). */
    @GetMapping("/content")
    public CertificateContent content(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.content(imam, id);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        CertificateService.Pdf pdf = service.pdf(imam, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.bytes());
    }

    @PostMapping("/void")
    public CertificateDto voidIt(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody CertificateDto.VoidRequest body) {
        return service.voidCertificate(imam, id, body.reason());
    }
}
