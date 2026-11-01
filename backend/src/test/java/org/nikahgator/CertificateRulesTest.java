package org.nikahgator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.service.CertificateService;
import org.springframework.http.HttpStatus;

class CertificateRulesTest {

    private static NikahRequest nikah(String status) {
        NikahRequest n = new NikahRequest();
        n.setStatus(status);
        return n;
    }

    @Test
    void onlyCompletedNikahsMayIssueOrDownload() {
        assertDoesNotThrow(() -> CertificateService.requireCompleted(nikah(NikahRequest.COMPLETED)));
        for (String s : new String[] { NikahRequest.REGISTERED, NikahRequest.CANCELLED, null }) {
            ApiException e = assertThrows(ApiException.class, () -> CertificateService.requireCompleted(nikah(s)));
            assertEquals(HttpStatus.CONFLICT, e.getStatus());
        }
    }
}
