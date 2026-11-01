package org.nikahgator.communityservice;

import java.util.List;

import org.nikahgator.communityservice.ServiceCertDtos.*;
import org.nikahgator.entity.AppUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only; the service guard also requires the community service to be enabled. */
@RestController
@RequestMapping("/api/imam/svc/community-service")
public class ServiceCertController {
    private final ServiceCertService service;

    public ServiceCertController(ServiceCertService service) {
        this.service = service;
    }

    @GetMapping
    public List<View> list(@AuthenticationPrincipal AppUser imam) {
        return service.list(imam);
    }

    @PostMapping
    public View create(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Request body) {
        return service.create(imam, body);
    }

    @GetMapping("/{id}")
    public View get(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.get(imam, id);
    }

    @PutMapping("/{id}")
    public View update(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Request body) {
        return service.update(imam, id, body);
    }

    @PostMapping("/{id}/void")
    public View voidIt(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody VoidRequest body) {
        return service.voidCert(imam, id, body.reason());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        Pdf pdf = service.pdf(imam, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.bytes());
    }
}
