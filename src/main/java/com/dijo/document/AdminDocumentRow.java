package com.dijo.document;

import java.time.Instant;

/**
 * One row of the admin's document list (UC-007 step 2, BR-03).
 *
 * @param clientName null if the client has no name on record
 */
public record AdminDocumentRow(Long id, String panNumber, String clientName, DocumentType documentType,
                               int assessmentYear, int financialYear, Instant uploadedAt, String uploadedBy,
                               String location, String fileName) {

    /** Path of the file relative to the document store. */
    public String storedPath() {
        return location + "/" + fileName;
    }
}
