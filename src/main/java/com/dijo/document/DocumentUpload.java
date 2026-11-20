package com.dijo.document;

/**
 * What the admin submits on the upload form: the file plus its metadata.
 *
 * @param fileName the name of the selected file, as reported by the browser
 * @param content  the file's bytes, or null if no file was selected
 */
public record DocumentUpload(String panNumber, String assessmentYear, String financialYear,
                             DocumentType documentType, String location, String fileName, byte[] content) {
}
