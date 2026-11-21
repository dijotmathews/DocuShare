package com.dijo.document;

/**
 * The kinds of document an admin can upload (UC-003 BR-03).
 */
public enum DocumentType {

    IT_ACKNOWLEDGEMENT("IT Acknowledgement", DocumentGroup.FILINGS),
    BALANCE_SHEET("Balance Sheet", DocumentGroup.FINANCIALS),
    PROFIT_LOSS("Profit & Loss", DocumentGroup.FINANCIALS),
    AUDIT_REPORT("Audit Report", DocumentGroup.FINANCIALS),
    TAX_AUDIT_REPORT("Tax Audit Report", DocumentGroup.FINANCIALS),
    TAX_COMPUTATION("Tax Computation", DocumentGroup.COMPUTATIONS),
    MISC_REPORT("Misc Report", DocumentGroup.MISC);

    private final String label;
    private final DocumentGroup group;

    DocumentType(String label, DocumentGroup group) {
        this.label = label;
        this.group = group;
    }

    public String label() {
        return label;
    }

    /** The group the client sees this type under (UC-004 BR-04). */
    public DocumentGroup group() {
        return group;
    }
}
