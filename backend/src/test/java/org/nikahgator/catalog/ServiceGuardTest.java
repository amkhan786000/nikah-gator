package org.nikahgator.catalog;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.nikahgator.entity.AppUser;

class ServiceGuardTest {

    private static final Map<ServiceKey, AccessLevel> VIEW_NIKAH = Map.of(ServiceKey.NIKAH, AccessLevel.VIEW);
    private static final Map<ServiceKey, AccessLevel> MANAGE_NIKAH = Map.of(ServiceKey.NIKAH, AccessLevel.MANAGE);

    @Test
    void viewOnlyStaffCanReadButNotChange() {
        assertNull(ServiceGuard.refusal(AppUser.STAFF, "GET", "/api/imam/nikahs", VIEW_NIKAH));
        assertEquals("VIEW_ONLY", ServiceGuard.refusal(AppUser.STAFF, "POST", "/api/imam/nikahs", VIEW_NIKAH));
        assertEquals("VIEW_ONLY", ServiceGuard.refusal(AppUser.STAFF, "DELETE", "/api/imam/nikahs/1/documents/2", VIEW_NIKAH));
    }

    @Test
    void managingStaffCanChangeServicesButNotTheMasjidSettings() {
        assertNull(ServiceGuard.refusal(AppUser.STAFF, "POST", "/api/imam/nikahs", MANAGE_NIKAH));
        assertNull(ServiceGuard.refusal(AppUser.STAFF, "GET", "/api/imam/masjid", MANAGE_NIKAH));
        assertEquals("IMAM_ONLY", ServiceGuard.refusal(AppUser.STAFF, "PUT", "/api/imam/masjid", MANAGE_NIKAH));
        assertEquals("IMAM_ONLY", ServiceGuard.refusal(AppUser.STAFF, "PUT", "/api/imam/certificate-template", MANAGE_NIKAH));
    }

    @Test
    void sendingADocumentByEmailIsAChangeSoViewOnlyStaffAreRefused() {
        var view = Map.of(ServiceKey.JUMA_LETTER, AccessLevel.VIEW);
        var manage = Map.of(ServiceKey.JUMA_LETTER, AccessLevel.MANAGE);
        assertEquals("VIEW_ONLY", ServiceGuard.refusal(AppUser.STAFF, "POST", "/api/imam/svc/juma/3/email", view));
        assertNull(ServiceGuard.refusal(AppUser.STAFF, "POST", "/api/imam/svc/juma/3/email", manage));
        assertEquals("VIEW_ONLY", ServiceGuard.refusal(AppUser.STAFF, "POST", "/api/imam/nikahs/3/certificate/email", VIEW_NIKAH));
    }

    @Test
    void ungrantedOrDisabledServicesAreRefused() {
        assertEquals("SERVICE_DISABLED", ServiceGuard.refusal(AppUser.STAFF, "GET", "/api/imam/svc/juma", MANAGE_NIKAH));
        assertEquals("SERVICE_DISABLED", ServiceGuard.refusal(AppUser.IMAM, "GET", "/api/imam/svc/juma", Map.of()));
    }

    @Test
    void imamWithAccessIsNeverRestrictedByTheStaffRules() {
        assertNull(ServiceGuard.refusal(AppUser.IMAM, "PUT", "/api/imam/masjid", MANAGE_NIKAH));
    }

    @Test
    void nonServicePathsPass() {
        assertNull(ServiceGuard.refusal(AppUser.STAFF, "GET", "/api/imam/catalog", Map.of()));
    }
}
