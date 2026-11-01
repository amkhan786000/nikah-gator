package org.nikahgator.certificate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.nikahgator.certificate.CertificateDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig): the certificate design of the imam's own masjid. */
@RestController
@RequestMapping("/api/imam/certificate-template")
public class CertificateTemplateController {

    private final CertificateTemplateService service;

    public CertificateTemplateController(CertificateTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public TemplateView get(@AuthenticationPrincipal AppUser imam) {
        return service.view(imam);
    }

    @PutMapping
    public TemplateView save(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody Draft body) {
        return service.save(imam, body);
    }

    @PostMapping("/reset")
    public TemplateView reset(@AuthenticationPrincipal AppUser imam) {
        return service.reset(imam);
    }

    @PostMapping("/preview")
    public Content preview(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody PreviewRequest body) {
        return service.preview(imam, body);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemplateView uploadLogo(@AuthenticationPrincipal AppUser imam, @RequestParam("file") MultipartFile file) {
        return service.saveLogo(imam, file);
    }

    @DeleteMapping("/logo")
    public TemplateView deleteLogo(@AuthenticationPrincipal AppUser imam) {
        return service.removeLogo(imam);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo(@AuthenticationPrincipal AppUser imam) {
        Path p = service.logoFor(imam);
        try {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(CertificateTemplateService.contentType(p)))
                    .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(Files.readAllBytes(p));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No logo uploaded.");
        }
    }
}
