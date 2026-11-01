package org.nikahgator.salah;

import java.util.concurrent.TimeUnit;

import org.nikahgator.salah.SalahDtos.PublicTimes;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public (no sign-in): the shared link. Only prayer times, the masjid's name, address and phone. */
@RestController
@RequestMapping("/api/public/salah")
public class PublicSalahController {

    private final SalahService service;

    public PublicSalahController(SalahService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicTimes> times(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic()).body(service.publicTimes(slug));
    }
}
