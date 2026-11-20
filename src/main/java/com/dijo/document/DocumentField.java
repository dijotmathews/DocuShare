package com.dijo.document;

/**
 * The fields of the upload form, with their display labels.
 */
public enum DocumentField {

    FILE("File"),
    PAN("PAN"),
    ASSESSMENT_YEAR("Assessment Year"),
    FINANCIAL_YEAR("Financial Year"),
    DOCUMENT_TYPE("Document Type"),
    LOCATION("Location");

    private final String label;

    DocumentField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
