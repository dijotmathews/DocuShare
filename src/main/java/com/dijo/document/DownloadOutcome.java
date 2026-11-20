package com.dijo.document;

/**
 * Result of a download attempt (UC-005), with the message shown to the client
 * when it failed.
 */
public enum DownloadOutcome {

    SUCCESS(null),
    /** AF-1: the record exists but the file is gone. */
    NOT_FOUND("Document could not be found. Please contact your consultant."),
    /** AF-2: the document isn't the client's (or doesn't exist). */
    ACCESS_DENIED("You do not have permission to access this document."),
    /** AF-3: the file exists but couldn't be read. */
    READ_ERROR("Unable to download document. Please try again or contact support.");

    private final String message;

    DownloadOutcome(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
