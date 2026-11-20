package com.dijo.usecase004;

import com.dijo.client.ClientService;
import com.dijo.document.ClientDocumentService;
import com.dijo.document.ClientDocumentService.DocumentSummary;
import com.dijo.document.ClientDocumentService.MyDocuments;
import com.dijo.document.Document;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-004 View Documents: ClientDocumentService (Main Flow steps 1–2, AF-1,
 * AF-2, BR-01 to BR-03).
 */
@SpringBootTest
class ViewDocumentsServiceTest {

    @Autowired
    private ClientDocumentService clientDocumentService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mainFlow_br02_br03_listsOwnDocumentsMostRecentFirstWithUploadedMetadata() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        Document first = DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2024, 2023);
        Document second = DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        Document third = DocumentFixtures.upload(documentService, pan, DocumentType.TAX_COMPUTATION, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        MyDocuments mine = clientDocumentService.findMyDocuments();

        assertEquals(pan, mine.panNumber());
        assertEquals("Acme Traders Pvt Ltd", mine.clientName());
        // BR-02: reverse chronological order
        assertEquals(List.of(third.getId(), second.getId(), first.getId()),
                mine.documents().stream().map(DocumentSummary::id).toList());
        // BR-03: metadata as uploaded
        DocumentSummary latest = mine.documents().get(0);
        assertEquals(DocumentType.TAX_COMPUTATION, latest.documentType());
        assertEquals(2025, latest.assessmentYear());
        assertEquals(2024, latest.financialYear());
        assertEquals(third.getUploadedAt(), latest.uploadedAt());
        assertEquals("tax_computation.pdf", latest.fileName());
    }

    @Test
    void br01_clientsOnlySeeDocumentsForTheirOwnPan() {
        String mine = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        String other = DocumentFixtures.createClient(clientService, "Other Co");
        Document own = DocumentFixtures.upload(documentService, mine, DocumentType.AUDIT_REPORT, 2025, 2024);
        Document foreign = DocumentFixtures.upload(documentService, other, DocumentType.PROFIT_LOSS, 2025, 2024);

        DocumentFixtures.signInAs(mine, "CLIENT");
        assertEquals(List.of(own.getId()),
                clientDocumentService.findMyDocuments().documents().stream().map(DocumentSummary::id).toList());

        DocumentFixtures.signInAs(other, "CLIENT");
        assertEquals(List.of(foreign.getId()),
                clientDocumentService.findMyDocuments().documents().stream().map(DocumentSummary::id).toList());
    }

    @Test
    void af1_af2_clientWithoutDocumentsGetsAnEmptyList() {
        String pan = DocumentFixtures.createClient(clientService, "New Client LLP");
        DocumentFixtures.signInAs(pan, "CLIENT");

        MyDocuments mine = clientDocumentService.findMyDocuments();

        assertTrue(mine.documents().isEmpty());
        assertEquals("New Client LLP", mine.clientName());
    }

    @Test
    void onlyClientsCanListTheirDocuments() {
        DocumentFixtures.signInAs("admin", "ADMIN");
        assertThrows(AccessDeniedException.class, clientDocumentService::findMyDocuments);

        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, clientDocumentService::findMyDocuments);
    }
}
