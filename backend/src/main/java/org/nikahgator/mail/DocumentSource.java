package org.nikahgator.mail;

import java.util.Set;

import org.nikahgator.entity.AppUser;

/**
 * Implemented by each service that produces a PDF the imam may e-mail. It reuses the service's own PDF
 * method; the mailer never builds a PDF itself.
 */
public interface DocumentSource {

    /** The e-mailable document: what the e-mail says and the PDF to attach. */
    record Doc(Long masjidId, String label, String number, String filename, byte[] pdf, boolean sensitive) {
    }

    /** The document types (the service slugs, or "nikah-certificate") this source handles. */
    Set<String> documentTypes();

    /**
     * Finds the record in the imam's own masjid (404 otherwise) and renders it. Throws a 409 ApiException
     * when the record is void or, for a nikah certificate, not issued/valid or not completed.
     */
    Doc load(AppUser imam, String type, Long id);
}
