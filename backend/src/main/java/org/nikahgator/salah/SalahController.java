package org.nikahgator.salah;

import org.nikahgator.entity.AppUser;
import org.nikahgator.salah.SalahDtos.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/** The imam's side (ServiceGuard enforces that the service is on and, for staff, View/Manage). */
@RestController
@RequestMapping("/api/imam/svc/salah-times")
public class SalahController {

    private final SalahService service;

    public SalahController(SalahService service) {
        this.service = service;
    }

    @GetMapping
    public Overview overview(@AuthenticationPrincipal AppUser user) {
        return service.overview(user);
    }

    @PutMapping("/settings")
    public SettingsDto saveSettings(@AuthenticationPrincipal AppUser user, @RequestBody SettingsDto req) {
        return service.saveSettings(user, req);
    }

    @PostMapping("/periods")
    public PeriodDto create(@AuthenticationPrincipal AppUser user, @Valid @RequestBody PeriodRequest req) {
        return service.create(user, req);
    }

    @PutMapping("/periods/{id}")
    public PeriodDto update(@AuthenticationPrincipal AppUser user, @PathVariable Long id, @Valid @RequestBody PeriodRequest req) {
        return service.update(user, id, req);
    }

    @DeleteMapping("/periods/{id}")
    public void delete(@AuthenticationPrincipal AppUser user, @PathVariable Long id) {
        service.delete(user, id);
    }

    /** {@code commit=false} only checks the file and says what would change. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@AuthenticationPrincipal AppUser user, @RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "commit", defaultValue = "false") boolean commit) {
        return service.importCsv(user, file, commit);
    }

    @GetMapping("/template.csv")
    public ResponseEntity<String> template() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("salah-times-template.csv").build().toString())
                .body(SalahService.templateCsv());
    }
}
