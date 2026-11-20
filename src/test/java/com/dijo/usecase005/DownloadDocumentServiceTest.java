package com.dijo.usecase005;

import com.dijo.client.ClientService;
import com.dijo.document.ClientDocumentService;
import com.dijo.document.Document;
import com.dijo.document.DocumentDownloadException;
import com.dijo.document.DocumentService;
import com.dijo.document.DownloadAuditEntry;
import com.dijo.document.DownloadAuditRepository;
import com.dijo.document.DownloadOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * UC-005 Download Document: ClientDocumentService.download (Main Flow steps
 * 2–5, AF-1 to AF-3, BR-01 to BR-04).
 */
@SpringBootTest
class DownloadDocumentServiceTest {

    private static final byte[] CONTENT = "%PDF-1.7 ITR-V".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private ClientDocumentService clientDocumentService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DownloadAuditRepository auditRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Records what the service hands over. */
    private static final class Capture implements ClientDocumentService.DownloadTarget {
        String fileName;
        String contentType;
        long size = -1;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        @Override
        public OutputStream start(String fileName, String contentType, long size) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.size = size;
            return bytes;
        }

        boolean started() {
            return fileName != null;
        }
    }

    private DownloadAuditEntry onlyAuditEntry(Document document) {
        List<DownloadAuditEntry> entries = auditRepository.findByDocumentIdOrderByIdAsc(document.getId());
        assertEquals(1, entries.size());
        return entries.get(0);
    }

    @Test
    void mainFlow_br03_streamsTheFileUnderItsOriginalNameAndLogsIt() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "ITR-V 2025 (signed).pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");
        Capture capture = new Capture();

        clientDocumentService.download(document.getId(), capture);

        // Step 4 / BR-03
        assertEquals("ITR-V 2025 (signed).pdf", capture.fileName);
        assertEquals("application/pdf", capture.contentType);
        assertEquals(CONTENT.length, capture.size);
        assertArrayEquals(CONTENT, capture.bytes.toByteArray());
        // Step 5 / BR-02
        DownloadAuditEntry entry = onlyAuditEntry(document);
        assertEquals(DownloadOutcome.SUCCESS, entry.getOutcome());
        assertEquals(pan, entry.getUsername());
        assertEquals(pan, entry.getDocumentPanNumber());
        assertEquals("ITR-V 2025 (signed).pdf", entry.getFileName());
        assertNotNull(entry.getAttemptedAt());
    }

    @Test
    void unknownFileTypesAreSentAsBinary() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "computation.xyz123", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");
        Capture capture = new Capture();

        clientDocumentService.download(document.getId(), capture);

        assertEquals("application/octet-stream", capture.contentType);
    }

    @Test
    void af1_missingFileIsReportedAndLogged() throws IOException {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "itr.pdf", CONTENT);
        Files.delete(DownloadFixtures.fileOf(document));
        DownloadFixtures.signInAs(pan, "CLIENT");
        Capture capture = new Capture();

        DocumentDownloadException ex = assertThrows(DocumentDownloadException.class,
                () -> clientDocumentService.download(document.getId(), capture));

        assertEquals("Document could not be found. Please contact your consultant.", ex.getMessage());
        assertEquals(404, ex.httpStatus());
        assertTrue(!capture.started(), "no file transferred");
        assertEquals(DownloadOutcome.NOT_FOUND, onlyAuditEntry(document).getOutcome());
    }

    @Test
    void af2_br01_anotherClientsDocumentIsDeniedAndLogged() {
        String owner = DownloadFixtures.createClient(clientService);
        String intruder = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, owner, "itr.pdf", CONTENT);
        DownloadFixtures.signInAs(intruder, "CLIENT");
        Capture capture = new Capture();

        DocumentDownloadException ex = assertThrows(DocumentDownloadException.class,
                () -> clientDocumentService.download(document.getId(), capture));

        assertEquals("You do not have permission to access this document.", ex.getMessage());
        assertEquals(403, ex.httpStatus());
        assertTrue(!capture.started(), "no file transferred");
        DownloadAuditEntry entry = onlyAuditEntry(document);
        assertEquals(DownloadOutcome.ACCESS_DENIED, entry.getOutcome());
        assertEquals(intruder, entry.getUsername());
        assertEquals(owner, entry.getDocumentPanNumber());
    }

    @Test
    void af2_nonExistentDocumentLooksTheSameAsSomeoneElses() {
        String pan = DownloadFixtures.createClient(clientService);
        DownloadFixtures.signInAs(pan, "CLIENT");
        long missingId = Long.MAX_VALUE - 7;

        DocumentDownloadException ex = assertThrows(DocumentDownloadException.class,
                () -> clientDocumentService.download(missingId, new Capture()));

        assertEquals(DownloadOutcome.ACCESS_DENIED, ex.getOutcome());
        List<DownloadAuditEntry> entries = auditRepository.findByUsernameOrderByIdAsc(pan);
        assertEquals(1, entries.size());
        assertEquals(missingId, entries.get(0).getDocumentId());
        assertNull(entries.get(0).getDocumentPanNumber());
    }

    @Test
    void af3_unreadableFileIsReportedAndLogged() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")), "root can read any file");
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "itr.pdf", CONTENT);
        var file = DownloadFixtures.fileOf(document);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        DownloadFixtures.signInAs(pan, "CLIENT");
        try {
            DocumentDownloadException ex = assertThrows(DocumentDownloadException.class,
                    () -> clientDocumentService.download(document.getId(), new Capture()));

            assertEquals("Unable to download document. Please try again or contact support.", ex.getMessage());
            assertEquals(500, ex.httpStatus());
            assertEquals(DownloadOutcome.READ_ERROR, onlyAuditEntry(document).getOutcome());
        } finally {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        }
    }

    @Test
    void br02_everyAttemptIsLogged() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "itr.pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");

        clientDocumentService.download(document.getId(), new Capture());
        clientDocumentService.download(document.getId(), new Capture());

        List<DownloadOutcome> outcomes = new ArrayList<>();
        auditRepository.findByDocumentIdOrderByIdAsc(document.getId()).forEach(e -> outcomes.add(e.getOutcome()));
        assertEquals(List.of(DownloadOutcome.SUCCESS, DownloadOutcome.SUCCESS), outcomes);
    }

    @Test
    void br04_onlySignedInClientsCanDownload() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "itr.pdf", CONTENT);

        // Still signed in as admin
        assertThrows(AccessDeniedException.class,
                () -> clientDocumentService.download(document.getId(), new Capture()));
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> clientDocumentService.download(document.getId(), new Capture()));
    }
}
