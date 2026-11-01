package org.nikahgator.divorce;

public enum DivorceType {
    TALAQ("Talaq (divorce by the husband)"),
    KHULA("Khula (divorce at the wife's request)"),
    MUTUAL("Mutual agreement"),
    FASKH("Faskh (annulment)");

    private final String label;

    DivorceType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
