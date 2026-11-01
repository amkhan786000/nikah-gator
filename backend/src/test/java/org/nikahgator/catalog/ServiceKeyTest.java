package org.nikahgator.catalog;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServiceKeyTest {

    @Test
    void nikahEndpointsBelongToTheNikahService() {
        for (String p : new String[] { "/api/imam/nikahs", "/api/imam/nikahs/5/certificate/pdf", "/api/imam/payments/statuses",
                "/api/imam/checklist-progress", "/api/imam/masjid/reminders", "/api/imam/certificate-template/logo" }) {
            assertEquals(ServiceKey.NIKAH, ServiceKey.forPath(p).orElseThrow(), p);
        }
    }

    @Test
    void newServicesAreMappedBySlug() {
        assertEquals(ServiceKey.JUMA_LETTER, ServiceKey.forPath("/api/imam/svc/juma").orElseThrow());
        assertEquals(ServiceKey.COMMUNITY_SERVICE, ServiceKey.forPath("/api/imam/svc/community-service/12/pdf").orElseThrow());
    }

    @Test
    void catalogAndUnknownPathsAreNotGuarded() {
        assertTrue(ServiceKey.forPath("/api/imam/catalog").isEmpty());
        assertTrue(ServiceKey.forPath("/api/imam/svc/unknown/1").isEmpty());
        assertTrue(ServiceKey.forPath("/api/imam/nikahsX").isEmpty());
    }

    @Test
    void slugsAreUnique() {
        assertEquals(ServiceKey.values().length,
                java.util.Arrays.stream(ServiceKey.values()).map(ServiceKey::slug).distinct().count());
    }
}
