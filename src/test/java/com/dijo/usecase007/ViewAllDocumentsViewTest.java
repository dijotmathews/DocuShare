package com.dijo.usecase007;

import com.dijo.admin.AdminDashboardView;
import com.dijo.admin.AllDocumentsView;
import com.dijo.client.ClientService;
import com.dijo.document.AdminDocumentRow;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-007 View All Documents: the admin's document list (Main Flow steps 1–7,
 * AF-2, BR-01, BR-04).
 */
@SpringBootTest
class ViewAllDocumentsViewTest extends SpringBrowserlessTest {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private Grid<AdminDocumentRow> grid() {
        return $(Grid.class).single();
    }

    @SuppressWarnings("unchecked")
    private Select<DocumentType> typeFilter() {
        return $(Select.class).single();
    }

    private TextField searchField() {
        return $(TextField.class).withLabel("PAN or client name").single();
    }

    private String emptyStateText() {
        return $(Paragraph.class, grid()).single().getText();
    }

    @Test
    void trigger_adminOpensAllDocumentsFromDashboard() {
        AllDocumentsFixtures.signInAs("admin", "ADMIN");
        navigate(AdminDashboardView.class);

        test($(RouterLink.class).withText("Documents").first()).click();

        assertInstanceOf(AllDocumentsView.class, getCurrentView());
    }

    @Test
    void mainFlow_columnsAndDefaultOrder() {
        String word = AllDocumentsFixtures.uniqueWord();
        String pan = AllDocumentsFixtures.createClient(clientService, "Acme " + word);
        AllDocumentsFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET);
        AllDocumentsFixtures.upload(documentService, pan, DocumentType.AUDIT_REPORT);

        navigate(AllDocumentsView.class);

        // Step 2
        assertEquals(List.of("PAN", "Client Name", "Document Type", "Assessment Year", "Financial Year",
                        "Upload Date", "Uploaded By", "File"),
                grid().getColumns().stream().filter(c -> c.isVisible()).map(c -> c.getHeaderText()).toList());
        // Step 3: newest first, shown on the Upload Date column
        List<GridSortOrder<AdminDocumentRow>> sort = grid().getSortOrder();
        assertEquals("Upload Date", sort.get(0).getSorted().getHeaderText());
        assertEquals(SortDirection.DESCENDING, sort.get(0).getDirection());

        // Steps 6–7: search narrows the list to this client
        test(searchField()).setValue(word.toLowerCase());

        assertEquals(2, test(grid()).size());
        assertEquals("Audit Report", test(grid()).getCellText(0, 2));
        assertEquals("Balance Sheet", test(grid()).getCellText(1, 2));
        assertEquals("Acme " + word, test(grid()).getCellText(0, 1));
        assertEquals("admin", test(grid()).getCellText(0, 6));
        assertTrue($(Span.class).all().stream().anyMatch(s -> "2 documents".equals(s.getText())));
    }

    @Test
    void step6_typeFilterCombinesWithSearch() {
        String word = AllDocumentsFixtures.uniqueWord();
        String pan = AllDocumentsFixtures.createClient(clientService, word);
        AllDocumentsFixtures.upload(documentService, pan, DocumentType.BALANCE_SHEET);
        AllDocumentsFixtures.upload(documentService, pan, DocumentType.PROFIT_LOSS);
        navigate(AllDocumentsView.class);

        test(searchField()).setValue(pan);
        test(typeFilter()).selectItem("Profit & Loss");

        assertEquals(1, test(grid()).size());
        assertEquals("Profit & Loss", test(grid()).getCellText(0, 2));
    }

    @Test
    void af2_noMatchesShowsMessageAndClearFiltersRestoresTheList() {
        AllDocumentsFixtures.signInAs("admin", "ADMIN");
        navigate(AllDocumentsView.class);
        int total = test(grid()).size();

        test(searchField()).setValue("no-such-client-" + AllDocumentsFixtures.uniqueWord());

        assertEquals(0, test(grid()).size());
        assertEquals("No documents match your search criteria. Try different filters.", emptyStateText());

        test($(Button.class).withText("Clear filters").first()).click();

        assertEquals("", searchField().getValue());
        assertEquals(total, test(grid()).size());
        assertTrue($(Button.class).withText("Clear filters").all().stream().noneMatch(b -> b.isVisible()));
    }

    @Test
    void br01_clientCannotOpenAllDocuments() {
        AllDocumentsFixtures.signInAs("ABCDE1234F", "CLIENT");

        try {
            navigate(AllDocumentsView.ROUTE, AllDocumentsView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof AllDocumentsView);
    }
}
