package com.dijo.client;

import com.dijo.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A client of the consultancy. Each client has exactly one CLIENT user account
 * whose username is the client's PAN.
 */
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String panNumber;

    // Nullable in the schema so existing databases can add the column;
    // required by ClientValidator for every create and edit.
    @Column(length = 200)
    private String clientName;

    @Column(nullable = false)
    private LocalDate dateOfIncorporation;

    @Column(nullable = false, length = 1000)
    private String address;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String contactNumber;

    private String whatsAppNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    protected Client() {
    }

    Client(String panNumber, User user, ClientDetails details) {
        this.panNumber = panNumber;
        this.user = user;
        apply(details);
    }

    /** Copies the editable fields (everything except PAN and user). */
    void apply(ClientDetails details) {
        this.clientName = details.getClientName().trim();
        this.dateOfIncorporation = details.getDateOfIncorporation();
        this.address = details.getAddress().trim();
        this.email = details.getEmail().trim();
        this.contactNumber = details.getContactNumber().trim();
        String whatsApp = details.getWhatsAppNumber();
        this.whatsAppNumber = whatsApp == null || whatsApp.isBlank() ? null : whatsApp.trim();
    }

    public Long getId() {
        return id;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public LocalDate getDateOfIncorporation() {
        return dateOfIncorporation;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getWhatsAppNumber() {
        return whatsAppNumber;
    }

    public User getUser() {
        return user;
    }

    /** Form data pre-filled with this client's current values. */
    public ClientDetails toDetails() {
        ClientDetails details = new ClientDetails();
        details.setPanNumber(panNumber);
        details.setClientName(clientName);
        details.setDateOfIncorporation(dateOfIncorporation);
        details.setAddress(address);
        details.setEmail(email);
        details.setContactNumber(contactNumber);
        details.setWhatsAppNumber(whatsAppNumber);
        return details;
    }
}
