package org.nikahgator.web;

import org.nikahgator.dto.CertificateDto;
import org.nikahgator.service.CertificateService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public (no sign-in): confirm that a certificate number exists and is still valid. Minimal data only. */
@RestController
@RequestMapping("/api/public/certificates")
public class PublicCertificateController {

    private final CertificateService service;

    public PublicCertificateController(CertificateService service) {
        this.service = service;
    }

    @GetMapping("/{number}")
    public ResponseEntity<CertificateDto.Verification> verify(@PathVariable String number) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.verify(number));
    }
}
