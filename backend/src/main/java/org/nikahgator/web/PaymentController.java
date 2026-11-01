package org.nikahgator.web;

import java.util.List;

import org.nikahgator.dto.PaymentDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/** Imam only (see SecurityConfig): fees, payments and settings of the imam's own masjid. */
@RestController
@RequestMapping("/api/imam")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/masjid")
    public MasjidSettings masjid(@AuthenticationPrincipal AppUser imam) {
        return service.masjid(imam);
    }

    @PutMapping("/masjid")
    public MasjidSettings updateMasjid(@AuthenticationPrincipal AppUser imam, @Valid @RequestBody MasjidSettings body) {
        return service.updateMasjid(imam, body);
    }

    @GetMapping("/payments/statuses")
    public List<StatusRow> statuses(@AuthenticationPrincipal AppUser imam) {
        return service.statuses(imam);
    }

    @GetMapping("/nikahs/{id}/payments")
    public Summary summary(@AuthenticationPrincipal AppUser imam, @PathVariable Long id) {
        return service.summary(imam, id);
    }

    @PostMapping("/nikahs/{id}/payments")
    public PaymentView record(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody RecordPayment body) {
        return service.record(imam, id, body);
    }

    @PutMapping("/nikahs/{id}/payments/waiver")
    public Summary waive(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @Valid @RequestBody Waiver body) {
        return service.waive(imam, id, body);
    }

    @PostMapping("/nikahs/{id}/payments/{paymentId}/refund")
    public PaymentView refund(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long paymentId,
                              @Valid @RequestBody Reason body) {
        return service.refund(imam, id, paymentId, body);
    }

    @PostMapping("/nikahs/{id}/payments/{paymentId}/void")
    public PaymentView voidPayment(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long paymentId,
                                   @Valid @RequestBody Reason body) {
        return service.voidPayment(imam, id, paymentId, body);
    }

    @GetMapping("/nikahs/{id}/payments/{paymentId}/receipt")
    public Receipt receipt(@AuthenticationPrincipal AppUser imam, @PathVariable Long id, @PathVariable Long paymentId) {
        return service.receipt(imam, id, paymentId);
    }
}
