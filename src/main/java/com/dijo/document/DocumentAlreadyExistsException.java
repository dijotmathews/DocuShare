package com.dijo.document;

/**
 * UC-003 AF-4: a file with the same name already exists at the location.
 * Uploading again with overwrite allowed replaces it.
 */
public class DocumentAlreadyExistsException extends RuntimeException {

    public static final String MESSAGE =
            "A file with this name already exists at the location. Overwrite or choose a different location?";

    private final String storedPath;

    public DocumentAlreadyExistsException(String storedPath) {
        super(MESSAGE);
        this.storedPath = storedPath;
    }

    /** The existing file, relative to the document store (e.g. ABCDE1234F/2025/itr.pdf). */
    public String getStoredPath() {
        return storedPath;
    }
}
