package com.dijo.usecase005;

import com.dijo.client.ClientDashboardView;
import com.dijo.client.ClientService;
import com.dijo.document.Document;
import com.dijo.document.DocumentService;
import com.dijo.document.DownloadAuditRepository;
import com.dijo.document.DownloadOutcome;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-005 AF-4 Print Document: a Print link right next to each document's
 * Download link on "My Documents".
 */
@SpringBootTest
class PrintDocumentViewTest extends SpringBrowserlessTest {

    private static final byte[] CONTENT = "%PDF-1.7 tax computation".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DownloadAuditRepository auditRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private List<Anchor> links(String className) {
        return $(Anchor.class).all().stream().filter(a -> a.hasClassName(className)).toList();
    }

    @Test
    void everyPrintableDocumentHasPrintRightAfterDownload() {
        String pan = DownloadFixtures.createClient(clientService);
        DownloadFixtures.upload(documentService, pan, "a.pdf", CONTENT);
        DownloadFixtures.upload(documentService, pan, "b.PDF", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        List<Anchor> prints = links("my-documents__print");
        assertEquals(2, prints.size());
        for (Anchor print : prints) {
            Component actions = print.getParent().orElseThrow();
            assertTrue(((Div) actions).hasClassName("my-documents__actions"));
            List<Component> children = actions.getChildren().toList();
            assertEquals(2, children.size());
            assertTrue(((Anchor) children.get(0)).hasClassName("my-documents__download"), "Download comes first");
            assertSame(print, children.get(1));
            assertFalse(print.getElement().hasAttribute("download"), "opens for printing instead of saving");
            assertEquals("_blank", print.getElement().getAttribute("target"));
            assertTrue(print.getElement().getAttribute("aria-label").startsWith("Print IT Acknowledgement"));
        }
    }

    @Test
    void printDeliversTheFileAndIsAuditedLikeADownload() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "computation.pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);
        ByteArrayOutputStream received = new ByteArrayOutputStream();

        test(links("my-documents__print").get(0)).download(received);

        assertArrayEquals(CONTENT, received.toByteArray());
        assertEquals(DownloadOutcome.SUCCESS,
                auditRepository.findByDocumentIdOrderByIdAsc(document.getId()).get(0).getOutcome());
    }

    @Test
    void filesTheBrowserCannotPrintHaveOnlyDownload() {
        String pan = DownloadFixtures.createClient(clientService);
        DownloadFixtures.upload(documentService, pan, "workings.xlsx", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        assertEquals(1, links("my-documents__download").size());
        assertTrue(links("my-documents__print").isEmpty());
    }
}
