package com.dijo.usecase004;

import com.dijo.client.ClientDashboardView;
import com.dijo.client.ClientService;
import com.dijo.document.Document;
import com.dijo.document.DocumentGroup;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.dijo.security.HomeView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-004 View Documents: the client's "My Documents" page, grouped into
 * Filings / Financials / Computations / Misc tabs and by financial year
 * (Main Flow steps 1–5, AF-1 to AF-3, BR-01 to BR-05).
 */
@SpringBootTest
class ViewDocumentsViewTest extends SpringBrowserlessTest {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private TabSheet tabs() {
        return $(TabSheet.class).single();
    }

    /** Tab captions as "Label count", in order. */
    private List<String> tabCaptions() {
        return DocumentGroupOrder.ALL.stream().map(g -> {
            Tab tab = test(tabs()).getTab(g.ordinal());
            List<Span> spans = $(Span.class, tab).all();
            return spans.get(0).getText() + " " + spans.get(1).getText();
        }).toList();
    }

    private void open(DocumentGroup group) {
        test(tabs()).select(group.ordinal());
    }

    /** Content of the selected tab (other tabs' content may stay attached, hidden). */
    private com.vaadin.flow.component.Component openTab() {
        int selected = tabs().getSelectedIndex();
        return test(tabs()).getTabContent(selected);
    }

    /** Year headings in the open tab, top to bottom. */
    private List<String> yearHeadings() {
        return $(H3.class, openTab()).all().stream().filter(h -> h.hasClassName("my-documents__year")).map(H3::getText).toList();
    }

    /** Document types listed under one year heading in the open tab, top to bottom. */
    private List<String> typesIn(String yearHeading) {
        Section section = $(Section.class, openTab()).all().stream()
                .filter(s -> $(H3.class, s).single().getText().equals(yearHeading))
                .findFirst().orElseThrow();
        return $(Span.class, section).all().stream()
                .filter(s -> s.hasClassName("my-documents__item-type")).map(Span::getText).toList();
    }

    @Test
    void trigger_clientLandsOnMyDocumentsAfterLogin() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate("", ClientDashboardView.class);

        assertInstanceOf(ClientDashboardView.class, getCurrentView());
        assertFalse(getCurrentView() instanceof HomeView);
    }

    @Test
    void step2_groupTabsInOrderWithCounts() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.TAX_AUDIT_REPORT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.MISC_REPORT, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals(List.of("Filings 1", "Financials 2", "Computations 0", "Misc 1"), tabCaptions());
        // Header names the client
        assertTrue($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("Acme Traders Pvt Ltd (PAN " + pan + ")")));
    }

    @Test
    void mainFlow_groupShowsFinancialYearsLatestFirstWithDocumentDetails() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2024, 2023);
        DocumentFixtures.upload(documentService, pan, DocumentType.PROFIT_LOSS, 2025, 2024);
        Document latest = DocumentFixtures.upload(documentService, pan, DocumentType.AUDIT_REPORT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);

        open(DocumentGroup.FINANCIALS);

        // BR-05: latest financial year first
        assertEquals(List.of("FY 2024-25", "FY 2023-24"), yearHeadings());
        // BR-02: most recent upload first within a year; BR-04: only this group's types
        assertEquals(List.of("Audit Report", "Profit & Loss"), typesIn("FY 2024-25"));
        assertEquals(List.of("Balance Sheet"), typesIn("FY 2023-24"));
        // BR-03: assessment year and upload date as uploaded
        String expectedDate = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault()).format(latest.getUploadedAt());
        assertTrue($(Span.class, openTab()).all().stream().anyMatch(s -> s.hasClassName("my-documents__item-meta")
                && s.getText().equals("AY 2025-26 · Uploaded " + expectedDate)));
        // UC-005: every document has a Download link
        assertEquals(3, $(Anchor.class, openTab()).all().stream().filter(a -> a.hasClassName("my-documents__download")).count());
    }

    @Test
    void opensOnTheFirstGroupThatHasDocuments() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.TAX_COMPUTATION, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertTrue(test(tabs()).isSelected(DocumentGroup.COMPUTATIONS.ordinal()));
        assertEquals(List.of("Tax Computation"), typesIn("FY 2024-25"));
    }

    @Test
    void af3_emptyGroupSaysSo() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);

        open(DocumentGroup.COMPUTATIONS);

        assertEquals("No computations yet.", $(Paragraph.class, openTab()).single().getText());
        assertTrue(yearHeadings().isEmpty());
    }

    @Test
    void br01_otherClientsDocumentsAreNotListed() {
        String mine = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        String other = DocumentFixtures.createClient(clientService, "Other Co");
        DocumentFixtures.upload(documentService, mine, DocumentType.AUDIT_REPORT, 2025, 2024);
        DocumentFixtures.upload(documentService, other, DocumentType.PROFIT_LOSS, 2025, 2024);
        DocumentFixtures.upload(documentService, other, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.signInAs(mine, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals(List.of("Filings 0", "Financials 1", "Computations 0", "Misc 0"), tabCaptions());
        assertEquals(List.of("Audit Report"), typesIn("FY 2024-25"));
    }

    @Test
    void af1_af2_emptyStateWhenNothingIsUploadedYet() {
        String pan = DocumentFixtures.createClient(clientService, "New Client LLP");
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals("No documents available yet. Check back soon.", $(H3.class).single().getText());
        assertTrue($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("please contact your consultant")));
        assertTrue($(TabSheet.class).all().isEmpty());
        assertTrue($(Div.class).all().stream().anyMatch(d -> d.hasClassName("my-documents__empty")));
    }

    @Test
    void adminCannotOpenMyDocuments() {
        DocumentFixtures.signInAs("admin", "ADMIN");

        try {
            navigate(ClientDashboardView.ROUTE, ClientDashboardView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof ClientDashboardView);
    }

    /** The groups in tab order. */
    private static final class DocumentGroupOrder {
        static final List<DocumentGroup> ALL = List.of(DocumentGroup.values());
    }
}
