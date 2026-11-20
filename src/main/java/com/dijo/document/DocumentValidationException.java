package com.dijo.document;

import java.util.Map;

/**
 * Thrown when upload data fails validation. Carries a summary message for the
 * form and an error message per offending field.
 */
public class DocumentValidationException extends RuntimeException {

    private final Map<DocumentField, String> fieldErrors;

    public DocumentValidationException(String summary, Map<DocumentField, String> fieldErrors) {
        super(summary);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<DocumentField, String> getFieldErrors() {
        return fieldErrors;
    }
}
