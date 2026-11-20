package com.dijo.client;

import java.time.LocalDate;

/**
 * Client data as entered in the add/edit form.
 */
public class ClientDetails {

    private String panNumber;
    private String clientName;
    private LocalDate dateOfIncorporation;
    private String address;
    private String email;
    private String contactNumber;
    private String whatsAppNumber;

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDate getDateOfIncorporation() {
        return dateOfIncorporation;
    }

    public void setDateOfIncorporation(LocalDate dateOfIncorporation) {
        this.dateOfIncorporation = dateOfIncorporation;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getWhatsAppNumber() {
        return whatsAppNumber;
    }

    public void setWhatsAppNumber(String whatsAppNumber) {
        this.whatsAppNumber = whatsAppNumber;
    }
}
