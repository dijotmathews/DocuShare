package com.dijo.usecase005;

import com.dijo.client.ClientDashboardView;
import com.dijo.client.ClientService;
import com.dijo.document.Document;
import com.dijo.document.DocumentService;
import com.dijo.document.DownloadAuditRepository;
import com.dijo.document.DownloadOutcome;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-005 Download Document: Download links on "My Documents" (Main Flow
 * steps 1–5, AF-1).
 */
@SpringBootTest
class DownloadDocumentViewTest extends SpringBrowserlessTest {

    private static final byte[] CONTENT = "%PDF-1.7 balance sheet".getBytes(StandardCharsets.UTF_8);

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

    /** The Download link of every document in the open group tab. */
    private List<Anchor> downloadLinks() {
        return $(Anchor.class).all().stream().filter(a -> a.hasClassName("my-documents__download")).toList();
    }

    @Test
    void step1_everyDocumentHasADownloadLink() {
        String pan = DownloadFixtures.createClient(clientService);
        DownloadFixtures.upload(documentService, pan, "a.pdf", CONTENT);
        DownloadFixtures.upload(documentService, pan, "b.pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");

        navigate(ClientDashboardView.class);

        List<Anchor> links = downloadLinks();
        assertEquals(2, links.size());
        links.forEach(link -> {
            assertTrue(link.getElement().hasAttribute("download"), "browser saves instead of opening");
            assertTrue(link.getElement().getAttribute("aria-label").startsWith("Download IT Acknowledgement"));
        });
    }

    @Test
    void mainFlow_clickingDownloadDeliversTheFileAndLogsIt() {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "balance-sheet.pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);
        ByteArrayOutputStream received = new ByteArrayOutputStream();

        test(downloadLinks().get(0)).download(received);

        assertArrayEquals(CONTENT, received.toByteArray());
        assertEquals(DownloadOutcome.SUCCESS,
                auditRepository.findByDocumentIdOrderByIdAsc(document.getId()).get(0).getOutcome());
    }

    @Test
    void af1_missingFileShowsTheErrorMessage() throws IOException {
        String pan = DownloadFixtures.createClient(clientService);
        Document document = DownloadFixtures.upload(documentService, pan, "itr.pdf", CONTENT);
        DownloadFixtures.signInAs(pan, "CLIENT");
        navigate(ClientDashboardView.class);
        Files.delete(DownloadFixtures.fileOf(document));
        ByteArrayOutputStream received = new ByteArrayOutputStream();

        test(downloadLinks().get(0)).download(received);
        roundTrip();

        assertEquals(0, received.size());
        assertTrue($(Notification.class).all().stream().anyMatch(n -> n.getElement().getOuterHTML()
                .contains("Document could not be found. Please contact your consultant.")
                || "Document could not be found. Please contact your consultant."
                        .equals(n.getElement().getProperty("text", ""))));
    }
}
