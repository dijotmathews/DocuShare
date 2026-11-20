package com.dijo.document;

/**
 * A download that failed (UC-005 AF-1 to AF-3). The message is the one to show
 * to the client.
 */
public class DocumentDownloadException extends RuntimeException {

    private final DownloadOutcome outcome;

    public DocumentDownloadException(DownloadOutcome outcome, Throwable cause) {
        super(outcome.message(), cause);
        this.outcome = outcome;
    }

    public DownloadOutcome getOutcome() {
        return outcome;
    }

    /** HTTP status for the download response. */
    public int httpStatus() {
        return switch (outcome) {
            case NOT_FOUND -> 404;
            case ACCESS_DENIED -> 403;
            default -> 500;
        };
    }
}
