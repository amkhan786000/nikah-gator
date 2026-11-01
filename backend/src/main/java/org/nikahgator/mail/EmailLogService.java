package org.nikahgator.mail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.EmailLog;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.mail.MailDtos.LogDetail;
import org.nikahgator.mail.MailDtos.LogPage;
import org.nikahgator.mail.MailDtos.LogRow;
import org.nikahgator.repository.EmailLogRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.service.EmailService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Read-only view of the e-mail log. The imam sees only their own masjid; the platform admin sees all. */
@Service
public class EmailLogService {
    private final EmailLogRepository logs;
    private final MasjidRepository masjids;

    public EmailLogService(EmailLogRepository logs, MasjidRepository masjids) {
        this.logs = logs;
        this.masjids = masjids;
    }

    public LogPage imamList(AppUser imam, LocalDate from, LocalDate to, String recipient, String kind, String status, int page, int size) {
        return list(masjidOf(imam), from, to, recipient, kind, status, page, size);
    }

    /** masjidId null: every masjid. */
    public LogPage adminList(Long masjidId, LocalDate from, LocalDate to, String recipient, String kind, String status, int page, int size) {
        return list(masjidId == null ? 0L : masjidId, from, to, recipient, kind, status, page, size);
    }

    public LogDetail imamGet(AppUser imam, Long id) {
        Long masjidId = masjidOf(imam);
        return detail(logs.findById(id).filter(e -> masjidId.equals(e.getMasjidId())).orElseThrow(EmailLogService::notFound));
    }

    public LogDetail adminGet(Long id) {
        return detail(logs.findById(id).orElseThrow(EmailLogService::notFound));
    }

    private LogPage list(long masjidId, LocalDate from, LocalDate to, String recipient, String kind, String status, int page, int size) {
        LocalDateTime start = from == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDateTime.of(2999, 1, 1, 0, 0) : to.plusDays(1).atStartOfDay();
        String like = "%" + (recipient == null ? "" : recipient.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
        Page<EmailLog> p = logs.search(masjidId, start, end, like, clean(kind), clean(status).toUpperCase(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        Map<Long, String> names = masjidNames(p.getContent());
        List<LogRow> rows = p.getContent().stream().map(e -> new LogRow(e.getId(), e.getCreatedAt(), e.getKind(), e.getStatus(),
                e.getRecipient(), e.getSubject(), e.getSentByName(), e.getMasjidId(), names.get(e.getMasjidId()),
                e.getAttachmentName(), e.getRelatedType(), e.getRelatedId())).toList();
        return new LogPage(rows, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    private LogDetail detail(EmailLog e) {
        boolean canResend = EmailService.CERTIFICATE.equals(e.getKind()) && "FAILED".equals(e.getStatus())
                && e.getRelatedType() != null && e.getRelatedId() != null;
        return new LogDetail(e.getId(), e.getCreatedAt(), e.getKind(), e.getStatus(), e.getRecipient(), e.getSubject(), e.getBody(),
                e.getError(), e.getSentByName(), e.getSentByUserId(), e.getMasjidId(), masjidNames(List.of(e)).get(e.getMasjidId()),
                e.getAttachmentName(), e.getRelatedType(), e.getRelatedId(), canResend);
    }

    private Map<Long, String> masjidNames(List<EmailLog> rows) {
        List<Long> ids = rows.stream().map(EmailLog::getMasjidId).filter(i -> i != null).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        masjids.findAllById(ids).forEach((Masjid m) -> names.put(m.getId(), m.getName()));
        return names;
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static Long masjidOf(AppUser imam) {
        if (imam.getMasjidId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "This account is not linked to a masjid.");
        return imam.getMasjidId();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "E-mail not found");
    }
}
