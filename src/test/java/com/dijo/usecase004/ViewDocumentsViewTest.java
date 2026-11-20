package com.dijo.usecase004;

import com.dijo.client.ClientDashboardView;
import com.dijo.client.ClientService;
import com.dijo.document.ClientDocumentService.DocumentSummary;
import com.dijo.document.Document;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.dijo.security.HomeView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.data.provider.SortDirection;
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
 * UC-004 View Documents: the client's "My Documents" page (Main Flow steps
 * 1–4, AF-1, AF-2, BR-01 to BR-03).
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

    @SuppressWarnings("unchecked")
    private Grid<DocumentSummary> grid() {
        return $(Grid.class).single();
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
    void mainFlow_tableShowsTheRequiredColumnsMostRecentFirst() {
        String pan = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        DocumentFixtures.upload(documentService, pan, DocumentType.IT_ACKNOWLEDGEMENT, 2024, 2023);
        Document latest = DocumentFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET, 2025, 2024);
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        Grid<DocumentSummary> grid = grid();
        // Step 2: columns
        assertEquals(List.of("Document Type", "Assessment Year", "Financial Year", "Upload Date"),
                grid.getColumns().stream().filter(c -> c.isVisible()).map(c -> c.getHeaderText())
                        .filter(h -> h != null && !h.isEmpty()).toList());
        assertEquals(2, test(grid).size());
        // BR-02 / BR-03: newest first, with the uploaded metadata
        assertEquals("Balance Sheet", test(grid).getCellText(0, 0));
        assertEquals("2025", test(grid).getCellText(0, 1));
        assertEquals("2024", test(grid).getCellText(0, 2));
        String expectedDate = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault()).format(latest.getUploadedAt());
        assertEquals(expectedDate, test(grid).getCellText(0, 3));
        assertEquals("IT Acknowledgement", test(grid).getCellText(1, 0));
        // BR-02: the default sort is visible on the Upload Date column
        List<GridSortOrder<DocumentSummary>> sort = grid.getSortOrder();
        assertEquals("Upload Date", sort.get(0).getSorted().getHeaderText());
        assertEquals(SortDirection.DESCENDING, sort.get(0).getDirection());
        // Header names the client
        assertTrue($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("Acme Traders Pvt Ltd (PAN " + pan + ")")));
    }

    @Test
    void br01_otherClientsDocumentsAreNotListed() {
        String mine = DocumentFixtures.createClient(clientService, "Acme Traders Pvt Ltd");
        String other = DocumentFixtures.createClient(clientService, "Other Co");
        DocumentFixtures.upload(documentService, mine, DocumentType.AUDIT_REPORT, 2025, 2024);
        DocumentFixtures.upload(documentService, other, DocumentType.PROFIT_LOSS, 2025, 2024);
        DocumentFixtures.signInAs(mine, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals(1, test(grid()).size());
        assertEquals("Audit Report", test(grid()).getCellText(0, 0));
    }

    @Test
    void af1_af2_emptyStateWhenNothingIsUploadedYet() {
        String pan = DocumentFixtures.createClient(clientService, "New Client LLP");
        DocumentFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals("No documents available yet. Check back soon.", $(H3.class).single().getText());
        assertTrue($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("please contact your consultant")));
        assertTrue($(Grid.class).all().isEmpty());
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
