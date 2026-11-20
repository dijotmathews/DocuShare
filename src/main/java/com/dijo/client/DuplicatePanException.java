package com.dijo.client;

import java.util.Map;

/**
 * Thrown when creating a client whose PAN is already registered (UC-006 AF-1).
 */
public class DuplicatePanException extends ClientValidationException {

    private final String panNumber;

    public DuplicatePanException(String panNumber) {
        super("A client with PAN " + panNumber + " already exists.",
                Map.of(ClientField.PAN, "This PAN is already registered"));
        this.panNumber = panNumber;
    }

    public String getPanNumber() {
        return panNumber;
    }
}
