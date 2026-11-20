package com.dijo.document;

import java.time.LocalDate;

/**
 * Filters for the admin's document list (UC-007 step 6). Every part is
 * optional; null or blank means "don't filter on this".
 *
 * @param search       part of a PAN or client name, case-insensitive (BR-04)
 * @param documentType only this type
 * @param uploadedFrom first upload date to include
 * @param uploadedTo   last upload date to include
 */
public record DocumentFilter(String search, DocumentType documentType, LocalDate uploadedFrom, LocalDate uploadedTo) {

    public static final DocumentFilter NONE = new DocumentFilter(null, null, null, null);

    public boolean isEmpty() {
        return (search == null || search.isBlank()) && documentType == null && uploadedFrom == null
                && uploadedTo == null;
    }
}
