package com.dijo.client;

import java.util.Map;

/**
 * Thrown when client data fails validation. Carries a summary message for the
 * form and an error message per offending field.
 */
public class ClientValidationException extends RuntimeException {

    private final Map<ClientField, String> fieldErrors;

    public ClientValidationException(String summary, Map<ClientField, String> fieldErrors) {
        super(summary);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<ClientField, String> getFieldErrors() {
        return fieldErrors;
    }
}
