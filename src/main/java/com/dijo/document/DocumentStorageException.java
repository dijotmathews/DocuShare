package com.dijo.document;

/**
 * UC-003 AF-3: the file couldn't be written (disk full, permission denied, ...).
 */
public class DocumentStorageException extends RuntimeException {

    public static final String MESSAGE = "Failed to upload file. Please check the storage location and try again.";

    public DocumentStorageException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
