package com.dijo.client;

/**
 * The fields of the client form, with their display labels.
 */
public enum ClientField {

    CLIENT_NAME("Client Name"),
    PAN("PAN"),
    DATE_OF_INCORPORATION("Date of Incorporation"),
    ADDRESS("Address"),
    EMAIL("Email"),
    CONTACT_NUMBER("Contact Number"),
    WHATSAPP_NUMBER("WhatsApp Number");

    private final String label;

    ClientField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
