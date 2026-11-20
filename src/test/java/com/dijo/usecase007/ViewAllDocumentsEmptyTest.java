package com.dijo.usecase007;

import com.dijo.admin.AllDocumentsView;
import com.dijo.document.DocumentFilter;
import com.dijo.document.DocumentService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * UC-007 AF-1: nothing uploaded yet. The shared test database always has
 * documents from other tests, so the service is replaced with an empty one.
 */
@SpringBootTest
class ViewAllDocumentsEmptyTest extends SpringBrowserlessTest {

    @MockitoBean
    private DocumentService documentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void af1_noDocumentsShowsTheEmptyStateWithoutClearFilters() {
        when(documentService.findAll(any(DocumentFilter.class))).thenReturn(List.of());
        AllDocumentsFixtures.signInAs("admin", "ADMIN");

        navigate(AllDocumentsView.class);

        Grid<?> grid = $(Grid.class).single();
        assertEquals("No documents have been uploaded yet.", $(Paragraph.class, grid).single().getText());
        assertTrue($(Button.class).withText("Clear filters").all().stream().noneMatch(b -> b.isVisible()));
    }
}
