package org.nikahgator.audit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * What changed between two states of an entity, for the activity log. Only plain values (text, numbers, flags,
 * dates, enums) are compared. Secrets are never kept: a redacted field is reduced to a fingerprint so a change is
 * noticed, and shown as "[changed]" - its value is never stored.
 */
public final class ChangeDiff {
    private ChangeDiff() {
    }

    public static final String CHANGED = "[changed]";
    static final int MAX_LEN = 200;
    private static final Pattern SECRET_NAME =
            Pattern.compile("(?i)(?!must).*(password|passwd|hash|token|secret|apikey).*|otp|.*otp$|.*otpcode.*");
    private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentHashMap<>();

    /** A secret value, reduced to a fingerprint: equal secrets compare equal, but the value cannot be read back. */
    record Secret(int fingerprint) {
    }

    public static boolean isRedactedName(String fieldName) {
        return SECRET_NAME.matcher(fieldName).matches();
    }

    private static boolean simple(Class<?> t) {
        return t.isPrimitive() || t == String.class || t == Character.class || Number.class.isAssignableFrom(t)
                || t == Boolean.class || t.isEnum() || Temporal.class.isAssignableFrom(t) || t == java.util.Date.class;
    }

    private static List<Field> fieldsOf(Class<?> type) {
        return FIELDS.computeIfAbsent(type, c -> {
            List<Field> out = new ArrayList<>();
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                if (k == Auditable.class) {
                    continue; // the bookkeeping columns are not changes
                }
                for (Field f : k.getDeclaredFields()) {
                    int m = f.getModifiers();
                    if (Modifier.isStatic(m) || Modifier.isTransient(m) || f.isSynthetic() || !simple(f.getType())
                            || f.isAnnotationPresent(Transient.class) || f.isAnnotationPresent(Id.class)
                            || f.isAnnotationPresent(AuditIgnore.class)) {
                        continue;
                    }
                    f.setAccessible(true);
                    out.add(f);
                }
            }
            return out;
        });
    }

    /** The loggable field values of an entity, by field name. */
    public static Map<String, Object> snapshot(Object entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Field f : fieldsOf(entity.getClass())) {
            try {
                Object v = f.get(entity);
                if (v != null && (f.isAnnotationPresent(AuditRedact.class) || isRedactedName(f.getName()))) {
                    v = new Secret(v.hashCode());
                }
                out.put(f.getName(), v);
            } catch (IllegalAccessException ignored) {
                // not readable: leave it out
            }
        }
        return out;
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }

    /** field -> {old, new} (display text) for every field that differs. */
    public static Map<String, String[]> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, String[]> out = new LinkedHashMap<>();
        for (var e : after.entrySet()) {
            Object old = before == null ? null : before.get(e.getKey());
            if (!same(old, e.getValue())) {
                out.put(e.getKey(), new String[] { show(old), show(e.getValue()) });
            }
        }
        return out;
    }

    /** Every non-empty value, as {value, null} (deleted). */
    public static Map<String, String[]> deletedValues(Map<String, Object> state) {
        Map<String, String[]> out = new LinkedHashMap<>();
        for (var e : state.entrySet()) {
            if (e.getValue() != null) {
                out.put(e.getKey(), new String[] { show(e.getValue()), null });
            }
        }
        return out;
    }

    static String show(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Secret) {
            return CHANGED;
        }
        String s = String.valueOf(v);
        return s.length() <= MAX_LEN ? s : s.substring(0, 80) + "... (" + s.length() + " characters)";
    }

    /** {"field":{"old":"a","new":"b"}}; null when there is nothing to say. */
    public static String toJson(Map<String, String[]> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : changes.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(quote(e.getKey())).append(":{\"old\":").append(quote(e.getValue()[0]))
                    .append(",\"new\":").append(quote(e.getValue()[1])).append('}');
        }
        return sb.append('}').toString();
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
