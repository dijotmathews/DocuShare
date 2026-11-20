package com.dijo.document;

import java.util.Map;

/**
 * UC-003 AF-1: no client exists with the entered PAN.
 */
public class ClientNotFoundException extends DocumentValidationException {

    public ClientNotFoundException(String panNumber) {
        super("Client with PAN " + panNumber + " not found. Please verify the PAN number.",
                Map.of(DocumentField.PAN, "No client with this PAN"));
    }
}
