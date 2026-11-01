package org.nikahgator.service;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.EmailLog;
import org.nikahgator.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends every e-mail the app sends and writes each one to the append-only e-mail log (never deleted, never
 * holding a secret). The signed-in user, when there is one, is recorded as the sender.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public static final String CERTIFICATE = "CERTIFICATE";
    public static final String OTP = "OTP";
    public static final String PASSWORD = "PASSWORD";
    public static final String REMINDER = "REMINDER";
    public static final String WELCOME = "WELCOME";
    public static final String OTHER = "OTHER";

    /** What the log records about an e-mail besides its text. Null fields are simply unknown. */
    public record Meta(String kind, Long masjidId, String relatedType, Long relatedId) {
        public static Meta of(String kind) {
            return new Meta(kind, null, null, null);
        }

        public static Meta of(String kind, Long masjidId) {
            return new Meta(kind, masjidId, null, null);
        }
    }

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLog;

    @Value("${app.mail.from:}")
    private String from;

    public EmailService(JavaMailSender mailSender, EmailLogRepository emailLog) {
        this.mailSender = mailSender;
        this.emailLog = emailLog;
    }

    public boolean send(String to, String subject, String body) {
        return send(Meta.of(OTHER), to, subject, body, null);
    }

    /** For an e-mail carrying a code or password: the e-mail log keeps the text with the secret as [hidden]. */
    public boolean send(String to, String subject, String body, String secret) {
        return send(Meta.of(OTHER), to, subject, body, secret);
    }

    public boolean send(Meta meta, String to, String subject, String body) {
        return send(meta, to, subject, body, null);
    }

    public boolean send(Meta meta, String to, String subject, String body, String secret) {
        EmailLog entry = newEntry(meta, to, subject, redact(meta.kind(), body, secret));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            entry.setSuccess(true);
        } catch (Exception e) {
            fail(entry, to, e);
        }
        return finish(entry);
    }

    /** Sends a plain e-mail with one attachment (a PDF). Returns the saved log entry, which says if it worked. */
    public EmailLog sendWithAttachment(Meta meta, String to, String subject, String body, String filename, byte[] data,
            String replyTo) {
        EmailLog entry = newEntry(meta, to, subject, body);
        entry.setAttachmentName(cut(filename, 190));
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) helper.setFrom(from);
            if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(data), "application/pdf");
            mailSender.send(message);
            entry.setSuccess(true);
        } catch (Exception e) {
            fail(entry, to, e);
        }
        finish(entry);
        return entry;
    }

    /**
     * The text to keep in the log. Codes and passwords are replaced by [hidden]; if an e-mail of a secret kind
     * (sign-in code, new account) was sent without naming its secret, no text at all is kept.
     */
    public static String redact(String kind, String body, String secret) {
        if (body == null) return null;
        if (secret != null && !secret.isEmpty()) return body.replace(secret, "[hidden]");
        return OTP.equals(kind) || WELCOME.equals(kind) ? "[hidden]" : body;
    }

    private EmailLog newEntry(Meta meta, String to, String subject, String body) {
        EmailLog entry = new EmailLog();
        entry.setRecipient(cut(to, 190));
        entry.setSubject(cut(subject, 255));
        entry.setBody(body);
        entry.setKind(meta.kind() == null ? OTHER : meta.kind());
        entry.setMasjidId(meta.masjidId());
        entry.setRelatedType(meta.relatedType());
        entry.setRelatedId(meta.relatedId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            entry.setSentByUserId(u.getId());
            entry.setSentByName(cut(u.getFullName(), 160));
            if (entry.getMasjidId() == null) entry.setMasjidId(u.getMasjidId());
        }
        return entry;
    }

    private void fail(EmailLog entry, String to, Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        log.error("Failed to send e-mail to {}: {} | server said: {}", to, e.getMessage(), root.getMessage());
        entry.setSuccess(false);
        entry.setError(cut(root.getMessage() != null ? root.getMessage() : e.getMessage(), 500));
    }

    private boolean finish(EmailLog entry) {
        entry.setStatus(entry.isSuccess() ? "SENT" : "FAILED");
        emailLog.save(entry);
        return entry.isSuccess();
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.substring(0, Math.min(s.length(), max));
    }
}
