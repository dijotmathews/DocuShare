package com.dijo.document;

/**
 * The kinds of document an admin can upload (UC-003 BR-03).
 */
public enum DocumentType {

    IT_ACKNOWLEDGEMENT("IT Acknowledgement"),
    BALANCE_SHEET("Balance Sheet"),
    PROFIT_LOSS("Profit & Loss"),
    AUDIT_REPORT("Audit Report"),
    TAX_AUDIT_REPORT("Tax Audit Report"),
    TAX_COMPUTATION("Tax Computation"),
    MISC_REPORT("Misc Report");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
