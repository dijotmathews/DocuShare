package com.dijo.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One download attempt, successful or not (UC-005 BR-02). Kept as plain values
 * rather than links so the trail survives if a document is removed.
 */
@Entity
@Table(name = "download_audit")
public class DownloadAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant attemptedAt;

    /** Who tried to download (the signed-in username, i.e. the client's PAN). */
    @Column(nullable = false)
    private String username;

    /** The requested document id, as sent by the browser. */
    private Long documentId;

    /** The PAN the document belongs to; null if the document doesn't exist. */
    private String documentPanNumber;

    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DownloadOutcome outcome;

    protected DownloadAuditEntry() {
    }

    DownloadAuditEntry(Instant attemptedAt, String username, Long documentId, Document document,
                       DownloadOutcome outcome) {
        this.attemptedAt = attemptedAt;
        this.username = username;
        this.documentId = documentId;
        this.documentPanNumber = document == null ? null : document.getPanNumber();
        this.fileName = document == null ? null : document.getDocumentIdentifier();
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public String getUsername() {
        return username;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getDocumentPanNumber() {
        return documentPanNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public DownloadOutcome getOutcome() {
        return outcome;
    }
}
