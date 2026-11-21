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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
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
    void uploadDatesAreBackdatedInTheAssessmentYearAndNeverInTheFuture() {
        List<Document> documents = documentRepository.findByPanNumber(DemoDataInitializer.DEMO_PAN);
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        int latest = DemoDataInitializer.latestCompletedFinancialYear(LocalDate.now());

        for (Document d : documents) {
            Instant from = LocalDate.of(d.getAssessmentYear(), 4, 1).atStartOfDay(zone).toInstant();
            assertTrue(!d.getUploadedAt().isBefore(from), d.getDocumentIdentifier() + " after 1 April of its AY");
            assertTrue(d.getUploadedAt().isBefore(now), d.getDocumentIdentifier() + " not in the future");
        }
        // Older financial years are fully backdated to their typical days, in a realistic order
        for (int fy = latest - 2; fy < latest; fy++) {
            int year = fy;
            List<DocumentType> byDate = documents.stream().filter(d -> d.getFinancialYear() == year)
                    .sorted(Comparator.comparing(Document::getUploadedAt))
                    .map(Document::getDocumentType).toList();
            assertEquals(List.of(DocumentType.MISC_REPORT, DocumentType.BALANCE_SHEET, DocumentType.PROFIT_LOSS,
                    DocumentType.AUDIT_REPORT, DocumentType.TAX_AUDIT_REPORT, DocumentType.TAX_COMPUTATION,
                    DocumentType.IT_ACKNOWLEDGEMENT), byDate, "FY " + fy);
            documents.stream().filter(d -> d.getFinancialYear() == year).forEach(d -> assertEquals(
                    LocalDate.of(year + 1, 4, 1).plusDays(DemoDataInitializer.uploadDayInAssessmentYear(d.getDocumentType())),
                    LocalDate.ofInstant(d.getUploadedAt(), zone), d.getDocumentIdentifier()));
        }
        assertEquals(documents.size(), documents.stream().map(Document::getUploadedAt).distinct().count(),
                "no two documents share an upload time");
    }

    @Test
    void recentYearDatesStillAheadMoveToJustBeforeNowInOrder() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant now = LocalDateTime.of(2026, 9, 1, 12, 0).atZone(zone).toInstant();
        DemoDataInitializer demo = new DemoDataInitializer(null, null, null, Clock.fixed(now, zone),
                new NoTransactions(), null);

        Instant pl = demo.plannedUpload(DocumentType.PROFIT_LOSS, 2025, 2, 7, now);
        Instant audit = demo.plannedUpload(DocumentType.AUDIT_REPORT, 2025, 3, 7, now);
        Instant itr = demo.plannedUpload(DocumentType.IT_ACKNOWLEDGEMENT, 2025, 6, 7, now);

        // P&L's typical day (15 Aug 2026) has passed: kept
        assertEquals(LocalDateTime.of(2026, 8, 15, 11, 14).atZone(zone).toInstant(), pl);
        // Audit (8 Sep) and ITR (23 Oct) are still ahead: just before now, in order
        assertEquals(now.minus(Duration.ofHours(4)), audit);
        assertEquals(now.minus(Duration.ofHours(1)), itr);
    }

    /** The date calculation needs no transactions. */
    private static final class NoTransactions extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {
        }
    }

    @Test
    void latestCompletedFinancialYearEndsInMarch() {
        assertEquals(2025, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 11, 21)));
        assertEquals(2025, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 4, 1)));
        assertEquals(2024, DemoDataInitializer.latestCompletedFinancialYear(LocalDate.of(2026, 3, 31)));
    }
}
