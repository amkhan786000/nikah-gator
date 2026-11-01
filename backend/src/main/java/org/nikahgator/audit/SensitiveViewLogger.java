package org.nikahgator.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An access-log entry when someone opens (or downloads the PDF of) a sensitive record: a counseling session, a
 * shahada record or a divorce record. Lists and everything else are not logged - only these single records.
 */
@Component
public class SensitiveViewLogger implements HandlerInterceptor {
    static final Pattern SENSITIVE = Pattern.compile("^/api/imam/svc/(counseling|shahada|divorce)/(\\d+)(/pdf)?$");

    private final ActivityLogService activity;

    public SensitiveViewLogger(ActivityLogService activity) {
        this.activity = activity;
    }

    /** {entityType, summary, id} for a request that opens a sensitive record; null for any other request. */
    static String[] describe(String method, String path) {
        if (!"GET".equals(method)) {
            return null;
        }
        Matcher m = SENSITIVE.matcher(path);
        if (!m.matches()) {
            return null;
        }
        boolean pdf = m.group(3) != null;
        String id = m.group(2);
        return switch (m.group(1)) {
            case "counseling" -> new String[] { "CounselingSession", (pdf ? "Downloaded PDF of" : "Opened") + " counseling session #" + id, id };
            case "shahada" -> new String[] { "LifeEvent", (pdf ? "Downloaded PDF of" : "Opened") + " shahada record #" + id, id };
            default -> new String[] { "Divorce", (pdf ? "Downloaded PDF of" : "Opened") + " divorce record #" + id, id };
        };
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        try {
            if (response.getStatus() >= 400) {
                return; // refused or not found: nothing was shown
            }
            String[] d = describe(request.getMethod(), request.getRequestURI());
            if (d != null) {
                activity.record(ActivityLogService.VIEW, null, d[0], Long.valueOf(d[2]), d[1], null);
            }
        } catch (RuntimeException e) {
            // never let the access log break the request
        }
    }
}
