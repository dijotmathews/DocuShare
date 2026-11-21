package com.dijo.demo;

import com.dijo.document.Document;
import com.dijo.document.DocumentGroup;
import com.dijo.document.DocumentRepository;
import com.dijo.document.DocumentType;
import com.dijo.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code demo} profile creates a demo client with documents in every
 * group (Filings, Financials, Computations, Misc) for three financial years.
 */
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataInitializerTest {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Value("${app.documents.root}")
    private Path documentsRoot;

    @Test
    void demoClientHasEveryDocumentTypeForThreeFinancialYears() {
        assertTrue(clientRepository.findByPanNumber(DemoDataInitializer.DEMO_PAN).isPresent());
        List<Document> documents = documentRepository.findByPanNumber(DemoDataInitializer.DEMO_PAN);

        assertEquals(DocumentType.values().length * DemoDataInitializer.YEARS, documents.size());
        Map<DocumentGroup, Set<Integer>> yearsPerGroup = documents.stream().collect(Collectors.groupingBy(
                d -> d.getDocumentType().group(), Collectors.mapping(Document::getFinancialYear, Collectors.toSet())));
        int latest = DemoDataInitializer.latestCompletedFinancialYear(LocalDate.now());
        for (DocumentGroup group : DocumentGroup.values()) {
            assertEquals(Set.of(latest - 2, latest - 1, latest), yearsPerGroup.get(group), group.name());
        }
        documents.forEach(d -> assertEquals(d.getFinancialYear() + 1, d.getAssessmentYear()));
    }

    @Test
    void demoFilesAreRealPdfs() throws IOException {
        Document any = documentRepository.findByPanNumber(DemoDataInitializer.DEMO_PAN).get(0);
        byte[] content = Files.readAllBytes(documentsRoot.resolve(any.getStoredPath()));
        String text = new String(content, StandardCharsets.ISO_8859_1);

        assertTrue(text.startsWith("%PDF-1.4"));
        assertTrue(text.contains("(" + any.getDocumentType().label().replace("(", "\\(").replace(")", "\\)") + ") Tj"));
        assertTrue(text.endsWith("%%EOF\n"));
    }

    @Test
    void latestCompletedFinancialYearEndsInMarch() {
        assertEquals(2025, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 11, 21)));
        assertEquals(2025, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 4, 1)));
        assertEquals(2024, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 3, 31)));
    }
}
