package org.nikahgator.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.EmailLog;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.mail.DocumentSource.Doc;
import org.nikahgator.mail.MailDtos.Failure;
import org.nikahgator.mail.MailDtos.Outcome;
import org.nikahgator.repository.EmailLogRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** E-mails a service's PDF to up to {@value #MAX_RECIPIENTS} people, one message each, and logs every one. */
@Component
public class DocumentMailer {
    public static final int MAX_RECIPIENTS = 5;
    private static final Pattern ADDRESS = Pattern.compile("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    private final Map<String, DocumentSource> sources = new HashMap<>();
    private final EmailService email;
    private final MasjidRepository masjids;
    private final EmailLogRepository logs;

    public DocumentMailer(List<DocumentSource> all, EmailService email, MasjidRepository masjids, EmailLogRepository logs) {
        all.forEach(s -> s.documentTypes().forEach(t -> sources.put(t, s)));
        this.email = email;
        this.masjids = masjids;
        this.logs = logs;
    }

    public Outcome send(AppUser imam, String type, Long id, List<String> to, String note) {
        List<String> recipients = parseRecipients(to);
        return deliver(imam, type, id, recipients, note);
    }

    /** Tries again a CERTIFICATE e-mail that failed, to the same address, with the document as it is now. */
    public Outcome resend(AppUser imam, Long logId) {
        EmailLog old = logs.findById(logId).filter(e -> imam.getMasjidId() != null && imam.getMasjidId().equals(e.getMasjidId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "E-mail not found"));
        if (!EmailService.CERTIFICATE.equals(old.getKind()) || !"FAILED".equals(old.getStatus())
                || old.getRelatedType() == null || old.getRelatedId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a document e-mail that failed can be sent again.");
        }
        return deliver(imam, old.getRelatedType(), old.getRelatedId(), List.of(old.getRecipient()), null);
    }

    private Outcome deliver(AppUser imam, String type, Long id, List<String> recipients, String note) {
        DocumentSource source = sources.get(type);
        if (source == null) throw new ApiException(HttpStatus.NOT_FOUND, "Unknown document type");
        Doc doc = source.load(imam, type, id);
        String masjidName = masjids.findById(doc.masjidId()).map(Masjid::getName).orElse("your masjid");
        String subject = subject(masjidName, doc);
        String body = body(masjidName, doc, imam.getFullName(), note);
        EmailService.Meta meta = new EmailService.Meta(EmailService.CERTIFICATE, doc.masjidId(), type, id);
        List<String> sent = new ArrayList<>();
        List<Failure> failed = new ArrayList<>();
        for (String to : recipients) {
            EmailLog entry = email.sendWithAttachment(meta, to, subject, body, doc.filename(), doc.pdf(), imam.getEmail());
            if (entry.isSuccess()) sent.add(to);
            else failed.add(new Failure(to, "The e-mail could not be delivered."));
        }
        return new Outcome(sent, failed);
    }

    /** Trims, drops repeats, and checks 1 to 5 plain addresses. */
    public static List<String> parseRecipients(List<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String r : raw) {
                if (r == null || r.isBlank()) continue;
                String a = r.trim();
                if (a.length() > 190 || !ADDRESS.matcher(a).matches()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "\"" + clip(a) + "\" is not a valid e-mail address.");
                }
                out.add(a);
            }
        }
        if (out.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Enter at least one e-mail address.");
        if (out.size() > MAX_RECIPIENTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can send to at most " + MAX_RECIPIENTS + " addresses at a time.");
        }
        return new ArrayList<>(out);
    }

    static String subject(String masjidName, Doc doc) {
        return oneLine(doc.label() + " " + doc.number() + " from " + masjidName);
    }

    /** Plain text: the masjid, the document and its number, the sender's own note. Never any private notes. */
    static String body(String masjidName, Doc doc, String senderName, String note) {
        StringBuilder b = new StringBuilder("Assalamu alaikum,\n\n");
        b.append(oneLine(masjidName)).append(" has sent you the attached ").append(doc.label().toLowerCase())
                .append(" (").append(oneLine(doc.number())).append(").\n");
        if (note != null && !note.isBlank()) {
            b.append("\nMessage from ").append(oneLine(senderName == null ? "the masjid" : senderName)).append(":\n")
                    .append(note.trim()).append('\n');
        }
        b.append("\nThe document is attached as a PDF (").append(doc.filename()).append(").\n")
                .append("Sent through Masjidly on behalf of ").append(oneLine(masjidName)).append('.');
        return b.toString();
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n\\p{Cntrl}]+", " ").trim();
    }

    private static String clip(String s) {
        String t = oneLine(s);
        return t.length() > 60 ? t.substring(0, 60) + "..." : t;
    }
}
