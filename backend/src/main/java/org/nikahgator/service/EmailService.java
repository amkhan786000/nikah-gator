package org.nikahgator.service;

import org.nikahgator.entity.EmailLog;
import org.nikahgator.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLog;

    @Value("${app.mail.from:}")
    private String from;

    public EmailService(JavaMailSender mailSender, EmailLogRepository emailLog) {
        this.mailSender = mailSender;
        this.emailLog = emailLog;
    }

    public boolean send(String to, String subject, String body) {
        return send(to, subject, body, null);
    }

    /** For an e-mail carrying a code or password: the e-mail log keeps the text with the secret as [hidden]. */
    public boolean send(String to, String subject, String body, String secret) {
        EmailLog entry = new EmailLog();
        entry.setRecipient(to);
        entry.setSubject(subject);
        entry.setBody(secret == null || secret.isEmpty() ? body : body.replace(secret, "[hidden]"));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            entry.setSuccess(true);
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.error("Failed to send e-mail to {}: {} | server said: {}", to, e.getMessage(), root.getMessage());
            entry.setSuccess(false);
            String err = root.getMessage() != null ? root.getMessage() : e.getMessage();
            entry.setError(err == null ? null : err.substring(0, Math.min(err.length(), 500)));
        }
        emailLog.save(entry);
        return entry.isSuccess();
    }
}
