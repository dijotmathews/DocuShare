package com.dijo.usecase007;

import com.dijo.client.ClientService;
import com.dijo.document.AdminDocumentRow;
import com.dijo.document.Document;
import com.dijo.document.DocumentFilter;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-007 View All Documents: DocumentService.findAll (Main Flow steps 1–3 and
 * 6–7, AF-2, BR-01 to BR-04).
 */
@SpringBootTest
class ViewAllDocumentsServiceTest {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static DocumentFilter search(String text) {
        return new DocumentFilter(text, null, null, null);
    }

    private static Set<Long> ids(List<AdminDocumentRow> rows) {
        return rows.stream().map(AdminDocumentRow::id).collect(Collectors.toSet());
    }

    @Test
    void mainFlow_br02_br03_listsEveryClientsDocumentsNewestFirstWithFullMetadata() {
        String word = AllDocumentsFixtures.uniqueWord();
        String panA = AllDocumentsFixtures.createClient(clientService, "Alpha " + word);
        String panB = AllDocumentsFixtures.createClient(clientService, "Beta " + word);
        Document first = AllDocumentsFixtures.upload(documentService, panA, DocumentType.BALANCE_SHEET);
        Document second = AllDocumentsFixtures.upload(documentService, panB, DocumentType.AUDIT_REPORT);

        List<AdminDocumentRow> all = documentService.findAll(DocumentFilter.NONE);

        // BR-02: both clients' documents; step 3: newest first
        List<Long> order = all.stream().map(AdminDocumentRow::id)
                .filter(id -> id.equals(first.getId()) || id.equals(second.getId())).toList();
        assertEquals(List.of(second.getId(), first.getId()), order);
        for (int i = 1; i < all.size(); i++) {
            assertFalse(all.get(i).uploadedAt().isAfter(all.get(i - 1).uploadedAt()), "reverse chronological");
        }
        // BR-03: complete metadata including uploader
        AdminDocumentRow row = all.stream().filter(r -> r.id().equals(second.getId())).findFirst().orElseThrow();
        assertEquals(panB, row.panNumber());
        assertEquals("Beta " + word, row.clientName());
        assertEquals(DocumentType.AUDIT_REPORT, row.documentType());
        assertEquals(2025, row.assessmentYear());
        assertEquals(2024, row.financialYear());
        assertEquals(second.getUploadedAt(), row.uploadedAt());
        assertEquals("admin", row.uploadedBy());
        assertEquals(second.getStoredPath(), row.storedPath());
    }

    @Test
    void step6_br04_searchByPanOrClientNameIgnoresCase() {
        String word = AllDocumentsFixtures.uniqueWord();
        String pan = AllDocumentsFixtures.createClient(clientService, "Malabar " + word + " Traders");
        Document document = AllDocumentsFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT);

        assertEquals(Set.of(document.getId()), ids(documentService.findAll(search(pan.toLowerCase()))));
        assertEquals(Set.of(document.getId()), ids(documentService.findAll(search(pan.substring(4).toLowerCase()))));
        assertEquals(Set.of(document.getId()), ids(documentService.findAll(search(word.toUpperCase()))));
        assertEquals(Set.of(document.getId()), ids(documentService.findAll(search("  malabar " + word + " "))));
    }

    @Test
    void step6_filterByDocumentType() {
        String word = AllDocumentsFixtures.uniqueWord();
        String pan = AllDocumentsFixtures.createClient(clientService, word);
        Document sheet = AllDocumentsFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET);
        AllDocumentsFixtures.upload(documentService, pan, DocumentType.PROFIT_LOSS);

        List<AdminDocumentRow> rows = documentService.findAll(
                new DocumentFilter(word, DocumentType.BALANCE_SHEET, null, null));

        assertEquals(Set.of(sheet.getId()), ids(rows));
    }

    @Test
    void step6_filterByUploadDateRangeIncludesBothEnds() {
        String word = AllDocumentsFixtures.uniqueWord();
        String pan = AllDocumentsFixtures.createClient(clientService, word);
        Document document = AllDocumentsFixtures.upload(documentService, pan, DocumentType.TAX_COMPUTATION);
        LocalDate today = LocalDate.now();

        assertEquals(Set.of(document.getId()),
                ids(documentService.findAll(new DocumentFilter(word, null, today, today))));
        assertEquals(Set.of(document.getId()),
                ids(documentService.findAll(new DocumentFilter(word, null, today.minusDays(30), null))));
        assertTrue(documentService.findAll(new DocumentFilter(word, null, today.plusDays(1), null)).isEmpty());
        assertTrue(documentService.findAll(new DocumentFilter(word, null, null, today.minusDays(1))).isEmpty());
    }

    @Test
    void af2_noMatchesGivesAnEmptyList() {
        AllDocumentsFixtures.signInAs("admin", "ADMIN");
        assertTrue(documentService.findAll(search("no-such-client-" + AllDocumentsFixtures.uniqueWord())).isEmpty());
    }

    @Test
    void searchWildcardsAreTakenLiterally() {
        String pan = AllDocumentsFixtures.createClient(clientService, AllDocumentsFixtures.uniqueWord());
        Document document = AllDocumentsFixtures.upload(documentService, pan, DocumentType.MISC_REPORT);

        assertFalse(ids(documentService.findAll(search("%"))).contains(document.getId()));
        assertFalse(ids(documentService.findAll(search("_"))).contains(document.getId()));
    }

    @Test
    void documentsOfClientsWithoutANameAreStillListed() {
        String pan = AllDocumentsFixtures.createClient(clientService, AllDocumentsFixtures.uniqueWord());
        Document document = AllDocumentsFixtures.upload(documentService, pan, DocumentType.MISC_REPORT);

        assertTrue(ids(documentService.findAll(search(pan))).contains(document.getId()));
    }

    @Test
    void br01_onlyAdminsCanListAllDocuments() {
        AllDocumentsFixtures.signInAs("ABCDE1234F", "CLIENT");
        assertThrows(AccessDeniedException.class, () -> documentService.findAll(DocumentFilter.NONE));

        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> documentService.findAll(DocumentFilter.NONE));
    }
}
