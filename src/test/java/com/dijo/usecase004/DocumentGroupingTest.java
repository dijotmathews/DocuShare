package com.dijo.usecase004;

import com.dijo.document.ClientDocumentService.DocumentSummary;
import com.dijo.document.ClientDocumentService.FinancialYearDocuments;
import com.dijo.document.ClientDocumentService.MyDocuments;
import com.dijo.document.DocumentGroup;
import com.dijo.document.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC-004 BR-04 / BR-05: which group each document type is shown under, and
 * the order of financial years and documents within a group.
 */
class DocumentGroupingTest {

    @Test
    void br04_everyTypeBelongsToItsGroup() {
        Map<DocumentType, DocumentGroup> expected = Map.of(
                DocumentType.IT_ACKNOWLEDGEMENT, DocumentGroup.FILINGS,
                DocumentType.BALANCE_SHEET, DocumentGroup.FINANCIALS,
                DocumentType.PROFIT_LOSS, DocumentGroup.FINANCIALS,
                DocumentType.AUDIT_REPORT, DocumentGroup.FINANCIALS,
                DocumentType.TAX_AUDIT_REPORT, DocumentGroup.FINANCIALS,
                DocumentType.TAX_COMPUTATION, DocumentGroup.COMPUTATIONS,
                DocumentType.MISC_REPORT, DocumentGroup.MISC);
        assertEquals(DocumentType.values().length, expected.size(), "every type is mapped");
        expected.forEach((type, group) -> assertEquals(group, type.group(), type.name()));
    }

    @Test
    void groupsAreInTabOrder() {
        assertEquals(List.of("Filings", "Financials", "Computations", "Misc"),
                List.of(DocumentGroup.values()).stream().map(DocumentGroup::label).toList());
    }

    private static DocumentSummary doc(long id, DocumentType type, int fy, String uploadedAt) {
        return new DocumentSummary(id, type, fy + 1, fy, Instant.parse(uploadedAt), "f" + id + ".pdf");
    }

    @Test
    void br05_yearsLatestFirstAndDocumentsMostRecentFirst() {
        MyDocuments mine = new MyDocuments("ABCDE1234F", "Acme", List.of(
                doc(1, DocumentType.BALANCE_SHEET, 2022, "2023-09-01T10:00:00Z"),
                doc(2, DocumentType.PROFIT_LOSS, 2024, "2025-08-01T10:00:00Z"),
                doc(3, DocumentType.IT_ACKNOWLEDGEMENT, 2024, "2025-09-01T10:00:00Z"),
                doc(4, DocumentType.AUDIT_REPORT, 2024, "2025-09-15T10:00:00Z"),
                doc(5, DocumentType.TAX_AUDIT_REPORT, 2023, "2024-09-30T10:00:00Z")));

        List<FinancialYearDocuments> financials = mine.byFinancialYear(DocumentGroup.FINANCIALS);

        assertEquals(List.of(2024, 2023, 2022), financials.stream().map(FinancialYearDocuments::financialYear).toList());
        assertEquals(List.of(4L, 2L), financials.get(0).documents().stream().map(DocumentSummary::id).toList());
        assertEquals(4, mine.count(DocumentGroup.FINANCIALS));
        assertEquals(1, mine.count(DocumentGroup.FILINGS));
        assertEquals(0, mine.count(DocumentGroup.COMPUTATIONS));
        assertEquals(List.of(), mine.byFinancialYear(DocumentGroup.MISC));
    }
}
