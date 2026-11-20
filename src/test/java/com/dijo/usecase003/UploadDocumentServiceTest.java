package com.dijo.usecase003;

import com.dijo.client.ClientService;
import com.dijo.document.ClientNotFoundException;
import com.dijo.document.Document;
import com.dijo.document.DocumentAlreadyExistsException;
import com.dijo.document.DocumentField;
import com.dijo.document.DocumentRepository;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentStorageException;
import com.dijo.document.DocumentType;
import com.dijo.document.DocumentUpload;
import com.dijo.document.DocumentValidationException;
import com.dijo.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dijo.usecase003.UploadFixtures.STORE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-003 Upload Document: DocumentService (Main Flow steps 9–12, AF-1 to AF-4,
 * BR-01 to BR-05).
 */
@SpringBootTest
class UploadDocumentServiceTest {

    private static final byte[] CONTENT = "%PDF-1.7 acknowledgement".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private DocumentService documentService;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ClientService clientService;
    @Autowired
    private UserRepository userRepository;

    private String pan;
    private String location;

    @BeforeEach
    void createClient() {
        UploadFixtures.signInAs("admin", "ADMIN");
        pan = UploadFixtures.nextPan();
        location = UploadFixtures.nextLocation();
        UploadFixtures.createClient(clientService, pan, "Acme Traders Pvt Ltd");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private DocumentUpload upload(String fileName, byte[] content) {
        return new DocumentUpload(pan, "2025", "2024", DocumentType.IT_ACKNOWLEDGEMENT, location, fileName, content);
    }

    private void assertNothingStored(String loc) {
        assertTrue(Files.notExists(STORE.resolve(loc)), "nothing written at " + loc);
        assertTrue(documentRepository.findAll().stream().noneMatch(d -> d.getLocation().equals(loc)));
    }

    @Test
    void mainFlow_storesFileAndRecordsMetadata() throws IOException {
        Document document = documentService.upload(
                new DocumentUpload(pan.toLowerCase(), " 2025 ", "2024", DocumentType.BALANCE_SHEET,
                        " " + location + "/ ", "balance-sheet.pdf", CONTENT), false);

        // Step 11: file stored at the location
        Path file = STORE.resolve(location).resolve("balance-sheet.pdf");
        assertArrayEquals(CONTENT, Files.readAllBytes(file));

        // Step 12: record with all metadata
        Document saved = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals(pan, saved.getPanNumber());
        assertEquals(2025, saved.getAssessmentYear());
        assertEquals(2024, saved.getFinancialYear());
        assertEquals(DocumentType.BALANCE_SHEET, saved.getDocumentType());
        assertEquals(location, saved.getLocation());
        assertEquals("balance-sheet.pdf", saved.getDocumentIdentifier());
        assertNotNull(saved.getUploadedAt());
        assertEquals(userRepository.findByUsername("admin").orElseThrow().getId(), saved.getUploadedBy().getId());
    }

    @Test
    void mainFlow_folderPartsOfTheBrowserFileNameAreDropped() {
        documentService.upload(upload("C:\\Users\\me\\Desktop\\itr.pdf", CONTENT), false);

        assertTrue(Files.exists(STORE.resolve(location).resolve("itr.pdf")));
    }

    @Test
    void af1_br02_unknownPanIsRejected() {
        String unknown = UploadFixtures.nextPan();
        DocumentUpload request = new DocumentUpload(unknown, "2025", "2024", DocumentType.IT_ACKNOWLEDGEMENT,
                location, "itr.pdf", CONTENT);

        ClientNotFoundException ex = assertThrows(ClientNotFoundException.class,
                () -> documentService.upload(request, false));

        assertEquals("Client with PAN " + unknown + " not found. Please verify the PAN number.", ex.getMessage());
        assertEquals(Set.of(DocumentField.PAN), ex.getFieldErrors().keySet());
        assertNothingStored(location);
    }

    @Test
    void af2_br01_missingFieldsAreAllListed() {
        DocumentUpload empty = new DocumentUpload(" ", "", null, null, "", null, null);

        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(empty, false));

        assertEquals("Please fill in the required fields: File, PAN, Assessment Year, Financial Year, "
                + "Document Type, Location.", ex.getMessage());
        assertEquals(Set.of(DocumentField.values()), ex.getFieldErrors().keySet());
    }

    @Test
    void af2_missingFileOnly() {
        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(upload(null, null), false));

        assertEquals("Please select a file to upload.", ex.getMessage());
        assertEquals(Set.of(DocumentField.FILE), ex.getFieldErrors().keySet());
        assertNothingStored(location);
    }

    @Test
    void emptyFileIsRejected() {
        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(upload("itr.pdf", new byte[0]), false));

        assertEquals(Set.of(DocumentField.FILE), ex.getFieldErrors().keySet());
        assertNothingStored(location);
    }

    @Test
    void af3_unwritableLocationFailsWithoutRecord() throws IOException {
        // A regular file where the folder should be makes creating the folder fail
        Files.createDirectories(STORE.resolve(location));
        Files.writeString(STORE.resolve(location).resolve("blocked"), "not a folder");
        String blocked = location + "/blocked/sub";
        DocumentUpload request = new DocumentUpload(pan, "2025", "2024", DocumentType.AUDIT_REPORT, blocked,
                "audit.pdf", CONTENT);

        DocumentStorageException ex = assertThrows(DocumentStorageException.class,
                () -> documentService.upload(request, false));

        assertEquals("Failed to upload file. Please check the storage location and try again.", ex.getMessage());
        assertTrue(documentRepository.findAll().stream().noneMatch(d -> d.getLocation().equals(blocked)));
    }

    @Test
    void af4_existingFileIsNotOverwrittenWithoutConfirmation() throws IOException {
        documentService.upload(upload("itr.pdf", CONTENT), false);
        byte[] newer = "%PDF-1.7 revised".getBytes(StandardCharsets.UTF_8);

        DocumentAlreadyExistsException ex = assertThrows(DocumentAlreadyExistsException.class,
                () -> documentService.upload(upload("itr.pdf", newer), false));

        assertEquals("A file with this name already exists at the location. Overwrite or choose a different "
                + "location?", ex.getMessage());
        assertEquals(location + "/itr.pdf", ex.getStoredPath());
        assertArrayEquals(CONTENT, Files.readAllBytes(STORE.resolve(location).resolve("itr.pdf")));
    }

    @Test
    void af4_overwriteReplacesFileAndUpdatesTheSameRecord() throws IOException {
        Document first = documentService.upload(upload("itr.pdf", CONTENT), false);
        byte[] newer = "%PDF-1.7 revised".getBytes(StandardCharsets.UTF_8);

        Document second = documentService.upload(new DocumentUpload(pan, "2024", "2023",
                DocumentType.TAX_COMPUTATION, location, "itr.pdf", newer), true);

        assertEquals(first.getId(), second.getId());
        assertArrayEquals(newer, Files.readAllBytes(STORE.resolve(location).resolve("itr.pdf")));
        List<Document> atLocation = documentRepository.findAll().stream()
                .filter(d -> d.getLocation().equals(location)).toList();
        assertEquals(1, atLocation.size());
        assertEquals(DocumentType.TAX_COMPUTATION, atLocation.get(0).getDocumentType());
        assertEquals(2024, atLocation.get(0).getAssessmentYear());
        try (var files = Files.list(STORE.resolve(location))) {
            assertEquals(List.of("itr.pdf"), files.map(p -> p.getFileName().toString()).toList(),
                    "no temporary files left behind");
        }
    }

    @Test
    void af4_fileOnDiskWithoutRecordAlsoCountsAsExisting() throws IOException {
        Files.createDirectories(STORE.resolve(location));
        Files.writeString(STORE.resolve(location).resolve("itr.pdf"), "placed by hand");

        assertThrows(DocumentAlreadyExistsException.class, () -> documentService.upload(upload("itr.pdf", CONTENT),
                false));
    }

    @Test
    void br03_documentTypesMatchTheSpec() {
        assertEquals(List.of("IT_ACKNOWLEDGEMENT", "BALANCE_SHEET", "PROFIT_LOSS", "AUDIT_REPORT",
                        "TAX_AUDIT_REPORT", "TAX_COMPUTATION", "MISC_REPORT"),
                Arrays.stream(DocumentType.values()).map(Enum::name).toList());
    }

    @ParameterizedTest
    @EnumSource(DocumentType.class)
    void br03_everyDocumentTypeCanBeUploaded(DocumentType type) {
        Document document = documentService.upload(new DocumentUpload(pan, "2025", "2024", type, location,
                type.name() + ".pdf", CONTENT), false);

        assertEquals(type, documentRepository.findById(document.getId()).orElseThrow().getDocumentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"25", "202", "20245", "abcd", "2O25", "1899", "-202"})
    void br05_invalidAssessmentYearIsRejected(String year) {
        DocumentUpload request = new DocumentUpload(pan, year, "2024", DocumentType.IT_ACKNOWLEDGEMENT, location,
                "itr.pdf", CONTENT);

        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(request, false));

        assertEquals(Set.of(DocumentField.ASSESSMENT_YEAR), ex.getFieldErrors().keySet());
        assertNothingStored(location);
    }

    @Test
    void br05_yearsFarInTheFutureAreRejected() {
        String tooLate = String.valueOf(Year.now().getValue() + 2);
        DocumentUpload request = new DocumentUpload(pan, "2025", tooLate, DocumentType.IT_ACKNOWLEDGEMENT,
                location, "itr.pdf", CONTENT);

        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(request, false));

        assertEquals(Set.of(DocumentField.FINANCIAL_YEAR), ex.getFieldErrors().keySet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside", "/etc/docushare", "a/../../b", "a/./b", "a\\b", ".hidden", "a//b"})
    void locationMustStayInsideTheDocumentStore(String badLocation) {
        DocumentUpload request = new DocumentUpload(pan, "2025", "2024", DocumentType.IT_ACKNOWLEDGEMENT,
                badLocation, "itr.pdf", CONTENT);

        DocumentValidationException ex = assertThrows(DocumentValidationException.class,
                () -> documentService.upload(request, false));

        assertEquals(Set.of(DocumentField.LOCATION), ex.getFieldErrors().keySet());
        assertFalse(Files.exists(STORE.getParent().resolve("outside")));
    }

    @Test
    void br04_anonymousUserCannotUpload() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> documentService.upload(upload("itr.pdf", CONTENT), false));
        assertNothingStored(location);
    }

    @Test
    void br04_clientUserCannotUpload() {
        UploadFixtures.signInAs(pan, "CLIENT");

        assertThrows(AccessDeniedException.class, () -> documentService.upload(upload("itr.pdf", CONTENT), false));
        assertNothingStored(location);
    }
}
