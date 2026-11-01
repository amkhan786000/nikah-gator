package org.nikahgator.catalog;

import java.util.List;
import java.util.Map;

import org.nikahgator.catalog.ServiceEntitlements.EnabledService;
import org.nikahgator.catalog.ServiceEntitlements.ServiceView;
import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ServiceController {

    private final ServiceEntitlements entitlements;

    public ServiceController(ServiceEntitlements entitlements) {
        this.entitlements = entitlements;
    }

    /** The imam's menu: only the services that are switched on. */
    @GetMapping("/api/imam/catalog")
    public List<EnabledService> mine(@AuthenticationPrincipal AppUser imam) {
        return entitlements.menuFor(imam);
    }

    @GetMapping("/api/admin/masjids/{id}/services")
    public List<ServiceView> forMasjid(@PathVariable Long id) {
        return entitlements.viewFor(id);
    }

    @PutMapping("/api/admin/masjids/{id}/services/{key}")
    public List<ServiceView> setMasjid(@PathVariable Long id, @PathVariable String key, @RequestBody Map<String, Boolean> body) {
        return entitlements.setForMasjid(id, parse(key), Boolean.TRUE.equals(body.get("enabled")));
    }

    /** {"enabled": null} clears the imam's own override. */
    @PutMapping("/api/admin/masjids/{id}/services/{key}/imam")
    public List<ServiceView> setImam(@PathVariable Long id, @PathVariable String key, @RequestBody Map<String, Boolean> body) {
        return entitlements.setForImam(id, parse(key), body.get("enabled"));
    }

    private static ServiceKey parse(String key) {
        try {
            return ServiceKey.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Unknown service");
        }
    }
}
