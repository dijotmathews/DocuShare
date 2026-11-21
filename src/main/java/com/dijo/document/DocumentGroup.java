package com.dijo.document;

/**
 * The groups a client's documents are shown under on "My Documents" (UC-004).
 * Every {@link DocumentType} belongs to exactly one group.
 */
public enum DocumentGroup {

    FILINGS("Filings", "filings"),
    FINANCIALS("Financials", "financials"),
    COMPUTATIONS("Computations", "computations"),
    MISC("Misc", "misc documents");

    private final String label;
    private final String noun;

    DocumentGroup(String label, String noun) {
        this.label = label;
        this.noun = noun;
    }

    public String label() {
        return label;
    }

    /** URL segment of the group's page, e.g. "financials" in /documents/financials. */
    public String slug() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The group with this URL segment, if any. */
    public static java.util.Optional<DocumentGroup> fromSlug(String slug) {
        return java.util.Arrays.stream(values()).filter(g -> g.slug().equals(slug)).findFirst();
    }

    /** Message shown for a group that has no documents. */
    public String emptyMessage() {
        return "No " + noun + " yet.";
    }
}
