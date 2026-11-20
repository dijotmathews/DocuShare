package com.dijo.document;

import com.dijo.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * An uploaded tax document. It belongs to a client through the PAN, and the
 * file lives in the document store at {@code location/documentIdentifier}.
 */
@Entity
@Table(name = "documents", uniqueConstraints = @UniqueConstraint(columnNames = {"location", "documentIdentifier"}))
public class Document {

    // Explicit type: without it the SQLite dialect emitted an untyped id column
    // here, which SQLite doesn't treat as an auto-increment rowid alias.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false)
    private String panNumber;

    // "smallint", not the default "integer": Hibernate leaves the type off an
    // identity column when "integer" already appears earlier in the CREATE
    // TABLE statement, and SQLite then doesn't auto-assign ids.
    @Column(nullable = false, columnDefinition = "smallint")
    private int assessmentYear;

    @Column(nullable = false, columnDefinition = "smallint")
    private int financialYear;

    /** The stored file's name inside {@link #location}. */
    @Column(nullable = false)
    private String documentIdentifier;

    /** Folder inside the document store, e.g. {@code ABCDE1234F/2025}. */
    @Column(nullable = false, length = 1000)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private Instant uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    protected Document() {
    }

    Document(String location, String documentIdentifier) {
        this.location = location;
        this.documentIdentifier = documentIdentifier;
    }

    /** Sets the metadata from an upload (a new one, or one that overwrites this document's file). */
    void record(DocumentValidator.ValidUpload upload, User admin, Instant now) {
        this.panNumber = upload.panNumber();
        this.assessmentYear = upload.assessmentYear();
        this.financialYear = upload.financialYear();
        this.documentType = upload.documentType();
        this.uploadedBy = admin;
        this.uploadedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public int getAssessmentYear() {
        return assessmentYear;
    }

    public int getFinancialYear() {
        return financialYear;
    }

    public String getDocumentIdentifier() {
        return documentIdentifier;
    }

    public String getLocation() {
        return location;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    /** Path of the file relative to the document store. */
    public String getStoredPath() {
        return location + "/" + documentIdentifier;
    }
}
