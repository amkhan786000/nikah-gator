package org.nikahgator.catalog;

/** What a user may do in one service: look (GET) or look and change (everything else). */
public enum AccessLevel {
    VIEW, MANAGE;

    public static AccessLevel parse(String s) {
        if (s == null) {
            return null;
        }
        return switch (s.trim().toUpperCase()) {
            case "VIEW" -> VIEW;
            case "MANAGE" -> MANAGE;
            default -> null;
        };
    }
}
