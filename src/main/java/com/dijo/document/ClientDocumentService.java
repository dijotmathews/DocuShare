package com.dijo.document;

import com.dijo.client.Client;
import com.dijo.client.ClientRepository;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * The signed-in client's view of their documents (UC-004) and downloads
 * (UC-005). Only CLIENT users
 * may call it, and the PAN always comes from the signed-in account, never
 * from the caller, so a client can only see their own documents (BR-01).
 */
@Service
@RolesAllowed("CLIENT")
public class ClientDocumentService {

    /** One row of the client's document list. */
    public record DocumentSummary(Long id, DocumentType documentType, int assessmentYear, int financialYear,
                                  Instant uploadedAt, String fileName) {

        static DocumentSummary of(Document d) {
            return new DocumentSummary(d.getId(), d.getDocumentType(), d.getAssessmentYear(), d.getFinancialYear(),
                    d.getUploadedAt(), d.getDocumentIdentifier());
        }
    }

    /** The signed-in client and their documents, most recent first (BR-02). */
    public record MyDocuments(String panNumber, String clientName, List<DocumentSummary> documents) {
    }

    /** Receives the file: called once the download is allowed, before any bytes are sent. */
    @FunctionalInterface
    public interface DownloadTarget {
        OutputStream start(String fileName, String contentType, long size) throws IOException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ClientDocumentService.class);
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("com.dijo.security.audit");

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final DownloadAuditRepository auditRepository;
    private final DocumentStorage storage;
    private final Clock clock;

    public ClientDocumentService(DocumentRepository documentRepository, ClientRepository clientRepository,
                                 DownloadAuditRepository auditRepository, DocumentStorage storage, Clock clock) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.auditRepository = auditRepository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MyDocuments findMyDocuments() {
        String pan = SecurityContextHolder.getContext().getAuthentication().getName();
        String clientName = clientRepository.findByPanNumber(pan).map(Client::getClientName).orElse(null);
        List<DocumentSummary> documents = documentRepository.findByPanNumberOrderByUploadedAtDescIdDesc(pan)
                .stream().map(DocumentSummary::of).toList();
        return new MyDocuments(pan, clientName, documents);
    }

    /**
     * Streams one of the signed-in client's documents to the target under its
     * original file name (steps 2–5, BR-03). Every attempt is recorded in the
     * download audit, whatever the outcome (BR-02). Not transactional, so the
     * audit entry for a failed attempt is kept.
     *
     * @throws DocumentDownloadException ACCESS_DENIED if the document isn't the client's or doesn't
     *                                   exist (AF-2, BR-01), NOT_FOUND if its file is missing (AF-1),
     *                                   READ_ERROR if the file can't be read (AF-3)
     */
    public void download(Long documentId, DownloadTarget target) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Document document = documentId == null ? null : documentRepository.findById(documentId).orElse(null);

        // Step 2 / BR-01. A missing record is reported the same way, so ids can't be probed.
        if (document == null || !document.getPanNumber().equals(username)) {
            SECURITY_LOG.warn("Denied download of document {} by {} (document PAN: {})", documentId, username,
                    document == null ? "none" : document.getPanNumber());
            throw fail(username, documentId, document, DownloadOutcome.ACCESS_DENIED, null);
        }

        // Step 3
        Path file;
        try {
            file = storage.pathOf(document.getLocation(), document.getDocumentIdentifier());
        } catch (IllegalArgumentException e) {
            LOG.error("Stored path of document {} is outside the document store", documentId, e);
            throw fail(username, documentId, document, DownloadOutcome.READ_ERROR, e);
        }
        if (!Files.isRegularFile(file)) {
            LOG.warn("File of document {} is missing: {}", documentId, file);
            throw fail(username, documentId, document, DownloadOutcome.NOT_FOUND, null);
        }

        // Step 4
        try (InputStream in = Files.newInputStream(file)) {
            OutputStream out = target.start(document.getDocumentIdentifier(), contentType(file), Files.size(file));
            in.transferTo(out);
            out.flush();
        } catch (IOException e) {
            LOG.error("Could not read file of document {}: {}", documentId, file, e);
            throw fail(username, documentId, document, DownloadOutcome.READ_ERROR, e);
        }

        // Step 5
        audit(username, documentId, document, DownloadOutcome.SUCCESS);
        LOG.info("Document {} ({}) downloaded by {}", documentId, document.getDocumentIdentifier(), username);
    }

    private DocumentDownloadException fail(String username, Long documentId, Document document,
                                           DownloadOutcome outcome, Throwable cause) {
        audit(username, documentId, document, outcome);
        return new DocumentDownloadException(outcome, cause);
    }

    private void audit(String username, Long documentId, Document document, DownloadOutcome outcome) {
        auditRepository.save(new DownloadAuditEntry(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS), username,
                documentId, document, outcome));
    }

    private static String contentType(Path file) {
        String type = null;
        try {
            type = Files.probeContentType(file);
        } catch (IOException ignored) {
            // fall back to the file name below
        }
        if (type == null) {
            type = URLConnection.guessContentTypeFromName(file.getFileName().toString());
        }
        return type == null ? "application/octet-stream" : type;
    }
}
