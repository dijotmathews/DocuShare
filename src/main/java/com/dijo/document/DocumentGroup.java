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

    /** Message shown on a group's tab when it has no documents. */
    public String emptyMessage() {
        return "No " + noun + " yet.";
    }
}
