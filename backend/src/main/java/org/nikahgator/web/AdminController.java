package org.nikahgator.web;

import java.util.List;
import java.util.Map;

import org.nikahgator.dto.AdminDtos.*;
import org.nikahgator.service.MasjidAdminService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Platform admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/masjids")
public class AdminController {

    private final MasjidAdminService service;

    public AdminController(MasjidAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<MasjidView> list() {
        return service.list();
    }

    @PostMapping
    public MasjidView create(@Valid @RequestBody CreateMasjidRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}/active")
    public MasjidView setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return service.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @PostMapping("/{id}/resend-credentials")
    public MasjidView resendCredentials(@PathVariable Long id) {
        return service.resendCredentials(id);
    }
}
