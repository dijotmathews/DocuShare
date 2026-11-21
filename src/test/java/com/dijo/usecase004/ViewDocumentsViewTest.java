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
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
 * Filings / Financials / Computations / Misc (side nav) and by financial year
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

    /** Side nav entries as "Label count", in order. */
    private List<String> navEntries() {
        return $(SideNavItem.class).all().stream()
                .map(i -> i.getLabel() + " " + ((Span) i.getSuffixComponent()).getText())
                .toList();
    }

    private void open(DocumentGroup group) {
        navigate(ClientDashboardView.ROUTE + "/" + group.slug(), ClientDashboardView.class);
    }

    private String currentPath() {
        return com.vaadin.flow.component.UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }

    /** Title of the group being shown. */
    private String groupTitle() {
        return $(H3.class).all().stream().filter(h -> h.hasClassName("my-documents__group-title"))
                .map(H3::getText).findFirst().orElseThrow();
    }

    /** Year headings of the shown group, top to bottom. */
    private List<String> yearHeadings() {
        return $(H4.class).all().stream().filter(h -> h.hasClassName("my-documents__year")).map(H4::getText).toList();
    }

    /** Document types listed under one year heading, top to bottom. */
    private List<String> typesIn(String yearHeading) {
        Div section = $(Div.class).all().stream()
                .filter(d -> d.hasClassName("my-documents__year-section"))
                .filter(d -> $(H4.class, d).single().getText().equals(yearHeading))
                .findFirst().orElseThrow();
        return $(Span.class, section).all().stream()
                .filter(sp -> sp.hasClassName("my-documents__item-type")).map(Span::getText).toList();
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
    void step2_sideNavListsGroupsInOrderWithCounts() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.TAX_AUDIT_REPORT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.MISC_REPORT, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals(List.of("Filings 1", "Financials 2", "Computations 0", "Misc 1"), navEntries());
        assertEquals(List.of("documents/filings", "documents/financials", "documents/computations", "documents/misc"),
                $(SideNavItem.class).all().stream().map(SideNavItem::getPath).toList());
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

        assertEquals("Financials", groupTitle());
        // BR-05: latest financial year first
        assertEquals(List.of("FY 2024-25", "FY 2023-24"), yearHeadings());
        // BR-02: most recent upload first within a year; BR-04: only this group's types
        assertEquals(List.of("Audit Report", "Profit & Loss"), typesIn("FY 2024-25"));
        assertEquals(List.of("Balance Sheet"), typesIn("FY 2023-24"));
        // BR-03: assessment year and upload date as uploaded
        String expectedDate = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault()).format(latest.getUploadedAt());
        assertTrue($(Span.class).all().stream().anyMatch(s -> s.hasClassName("my-documents__item-meta")
                && s.getText().equals("AY 2025-26 · Uploaded " + expectedDate)));
        // UC-005: every document has a Download link
        assertEquals(3, $(Anchor.class).all().stream().filter(a -> a.hasClassName("my-documents__download")).count());
    }

    @Test
    void opensOnTheFirstGroupThatHasDocuments() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.TAX_COMPUTATION, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals("documents/computations", currentPath());
        assertEquals("Computations", groupTitle());
        assertEquals(List.of("Tax Computation"), typesIn("FY 2024-25"));
    }

    @Test
    void switchingGroupReplacesTheShownDocuments() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);
        assertEquals(List.of("IT Acknowledgement"), typesIn("FY 2024-25"));

        open(DocumentGroup.FINANCIALS);

        assertEquals(List.of("Balance Sheet"), typesIn("FY 2024-25"));
        assertEquals(1, $(SideNav.class).all().size());
        assertEquals(1, $(H3.class).all().stream().filter(h -> h.hasClassName("my-documents__group-title")).count());
    }

    @Test
    void unknownGroupInTheUrlShowsTheFirstGroupWithDocuments() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.ROUTE + "/nonsense", ClientDashboardView.class);

        assertEquals("documents/financials", currentPath());
    }

    @Test
    void af3_emptyGroupSaysSo() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);

        open(DocumentGroup.COMPUTATIONS);

        assertEquals("Computations", groupTitle());
        assertTrue($(Paragraph.class).all().stream().anyMatch(p -> p.getText().equals("No computations yet.")));
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

        assertEquals(List.of("Filings 0", "Financials 1", "Computations 0", "Misc 0"), navEntries());
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
        assertTrue($(SideNav.class).all().isEmpty());
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
}
