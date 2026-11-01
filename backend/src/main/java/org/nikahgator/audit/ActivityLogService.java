package org.nikahgator.audit;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The activity log: an append-only record of who did what. There is deliberately no update or delete anywhere in
 * here (and no retention job): entries are inserted and read, nothing else. A failure to write an entry is
 * logged and swallowed, so it can never break the request that caused it.
 */
@Service
public class ActivityLogService {
    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String VOID = "VOID";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String VIEW = "VIEW";
    public static final List<String> ACTIONS = List.of(CREATE, UPDATE, DELETE, VOID, LOGIN, LOGIN_FAILED, LOGOUT, VIEW);

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("NikahRequest", "Nikah"), Map.entry("Certificate", "Nikah certificate"),
            Map.entry("ServiceCert", "Community service certificate"), Map.entry("JumaLetter", "Juma letter"),
            Map.entry("LifeEvent", "Life event record"), Map.entry("Divorce", "Divorce record"),
            Map.entry("CounselingSession", "Counseling session"), Map.entry("HallBooking", "Hall booking"),
            Map.entry("AppUser", "User account"), Map.entry("MasjidReminderSettings", "Reminder settings"),
            Map.entry("MasjidService", "Masjid service"), Map.entry("ImamService", "Imam service setting"),
            Map.entry("StaffPermission", "Staff permission"), Map.entry("NikahFee", "Nikah fee"));

    /** One entry as read back. */
    public record Row(Long id, Long masjidId, String masjidName, Long userId, String userName, String userRole,
                      String action, String entityType, Long entityId, String summary, String changedFields,
                      String ip, String userAgent, LocalDateTime createdAt) {
    }

    public record PageResult(List<Row> items, long total, int page, int size) {
    }

    /** What to look for; null parts are not filtered on. {@code masjidId} is how an imam is kept to their own masjid. */
    public record Filter(Long masjidId, LocalDate from, LocalDate to, Long userId, String action, String entityType,
                         Long entityId) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate afterCommit;

    public ActivityLogService(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.afterCommit = new TransactionTemplate(txManager);
        // its own transaction: the caller's has already committed (or there is none)
        this.afterCommit.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static String label(String entityType) {
        if (entityType == null) {
            return "record";
        }
        String known = LABELS.get(entityType);
        if (known != null) {
            return known;
        }
        String spaced = entityType.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    /** "groomName" -> "groom name". */
    public static String humanField(String field) {
        String s = field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return s.equals("password hash") ? "password" : s;
    }

    // ---- writing --------------------------------------------------------------------------------------

    /** Records something done by the signed-in user (or whoever {@link Actor#current()} says). */
    public void record(String action, Long masjidId, String entityType, Long entityId, String summary, String changedFields) {
        record(Actor.current(), action, masjidId, entityType, entityId, summary, changedFields);
    }

    public void record(Actor actor, String action, Long masjidId, String entityType, Long entityId, String summary,
                       String changedFields) {
        try {
            Long masjid = masjidId != null ? masjidId : actor == null ? null : actor.masjidId();
            Object[] row = { masjid, actor == null ? null : actor.userId(), cut(actor == null ? null : actor.name(), 160),
                    actor == null ? null : actor.role(), action, entityType, entityId, summary, changedFields,
                    cut(clientIp(), 64), cut(userAgent(), 300) };
            // Only a change that really committed is worth recording.
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        insert(row);
                    }
                });
            } else {
                insert(row);
            }
        } catch (RuntimeException e) {
            log.error("Could not record activity ({} {} {})", action, entityType, entityId, e);
        }
    }

    private void insert(Object[] row) {
        try {
            afterCommit.executeWithoutResult(s -> jdbc.update(
                    "INSERT INTO activity_log (masjid_id, user_id, user_name, user_role, action, entity_type, entity_id, "
                            + "summary, changed_fields, ip, user_agent) VALUES (?,?,?,?,?,?,?,?,?,?,?)", row));
        } catch (RuntimeException e) {
            log.error("Could not write the activity log entry: {}", row[7], e);
        }
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a.getRequest() : null;
    }

    private static String clientIp() {
        HttpServletRequest r = currentRequest();
        if (r == null) {
            return null;
        }
        String forwarded = r.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : r.getRemoteAddr();
    }

    private static String userAgent() {
        HttpServletRequest r = currentRequest();
        return r == null ? null : r.getHeader("User-Agent");
    }

    // ---- reading --------------------------------------------------------------------------------------

    private static final String SELECT = "SELECT a.id, a.masjid_id, m.name AS masjid_name, a.user_id, a.user_name, a.user_role, "
            + "a.action, a.entity_type, a.entity_id, a.summary, a.changed_fields, a.ip, a.user_agent, a.created_at "
            + "FROM activity_log a LEFT JOIN masjids m ON m.id = a.masjid_id";

    private static String where(Filter f, List<Object> args) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (f.masjidId() != null) {
            w.append(" AND a.masjid_id = ?");
            args.add(f.masjidId());
        }
        if (f.from() != null) {
            w.append(" AND a.created_at >= ?");
            args.add(Timestamp.valueOf(f.from().atStartOfDay()));
        }
        if (f.to() != null) {
            w.append(" AND a.created_at < ?");
            args.add(Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        }
        if (f.userId() != null) {
            w.append(" AND a.user_id = ?");
            args.add(f.userId());
        }
        if (f.action() != null && !f.action().isBlank()) {
            w.append(" AND a.action = ?");
            args.add(f.action());
        }
        if (f.entityType() != null && !f.entityType().isBlank()) {
            w.append(" AND a.entity_type = ?");
            args.add(f.entityType());
        }
        if (f.entityId() != null) {
            w.append(" AND a.entity_id = ?");
            args.add(f.entityId());
        }
        return w.toString();
    }

    private static Row row(java.sql.ResultSet r, int i) throws java.sql.SQLException {
        return new Row(r.getLong("id"), (Long) r.getObject("masjid_id"), r.getString("masjid_name"),
                (Long) r.getObject("user_id"), r.getString("user_name"), r.getString("user_role"), r.getString("action"),
                r.getString("entity_type"), (Long) r.getObject("entity_id"), r.getString("summary"),
                r.getString("changed_fields"), r.getString("ip"), r.getString("user_agent"),
                r.getTimestamp("created_at").toLocalDateTime());
    }

    /** Newest first. */
    public PageResult search(Filter f, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), 200);
        List<Object> args = new ArrayList<>();
        String w = where(f, args);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM activity_log a" + w, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add(p * s);
        List<Row> rows = jdbc.query(SELECT + w + " ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?",
                ActivityLogService::row, pageArgs.toArray());
        return new PageResult(rows, total == null ? 0 : total, p, s);
    }

    public List<Row> export(Filter f, int max) {
        List<Object> args = new ArrayList<>();
        String w = where(f, args);
        args.add(max);
        return jdbc.query(SELECT + w + " ORDER BY a.created_at DESC, a.id DESC LIMIT ?", ActivityLogService::row, args.toArray());
    }

    public record Person(Long id, String name) {
    }

    public record FilterChoices(List<Person> users, List<String> entityTypes, List<String> actions) {
    }

    /** What the filter drop-downs offer: the people and record types that appear in this log. */
    public FilterChoices choices(Long masjidId) {
        String scope = masjidId == null ? "" : " AND masjid_id = ?";
        Object[] args = masjidId == null ? new Object[0] : new Object[] { masjidId };
        List<Person> users = jdbc.query("SELECT user_id, MAX(user_name) AS n FROM activity_log WHERE user_id IS NOT NULL"
                + scope + " GROUP BY user_id ORDER BY n", (r, i) -> new Person(r.getLong(1), r.getString(2)), args);
        List<String> types = jdbc.queryForList("SELECT DISTINCT entity_type FROM activity_log WHERE entity_type IS NOT NULL"
                + scope + " ORDER BY entity_type", String.class, args);
        return new FilterChoices(users, types, ACTIONS);
    }

    // ---- CSV -----------------------------------------------------------------------------------------

    /** A cell for a spreadsheet: quoted, and defanged if it could be read as a formula. */
    public static String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static String toCsv(List<Row> rows) {
        StringBuilder sb = new StringBuilder("When,Masjid,User,Role,Action,Record type,Record id,Summary,Changes,IP address\r\n");
        for (Row r : rows) {
            sb.append(String.join(",", csv(String.valueOf(r.createdAt()).replace('T', ' ')), csv(r.masjidName()),
                    csv(r.userName()), csv(r.userRole()), csv(r.action()), csv(label(r.entityType())),
                    csv(r.entityId() == null ? null : String.valueOf(r.entityId())), csv(r.summary()),
                    csv(r.changedFields()), csv(r.ip()))).append("\r\n");
        }
        return sb.toString();
    }
}
