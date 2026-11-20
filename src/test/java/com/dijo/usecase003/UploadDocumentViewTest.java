package com.dijo.usecase003;

import com.dijo.admin.AdminDashboardView;
import com.dijo.admin.AllDocumentsView;
import com.dijo.admin.UploadDocumentView;
import com.dijo.client.ClientService;
import com.dijo.document.DocumentRepository;
import com.dijo.document.DocumentType;
import com.dijo.security.ClientLoginView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.dijo.client.Client;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static com.dijo.usecase003.UploadFixtures.STORE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-003 Upload Document: the upload form (Main Flow steps 1–14, AF-1, AF-2,
 * AF-4, BR-01, BR-03, BR-04).
 */
@SpringBootTest
class UploadDocumentViewTest extends SpringBrowserlessTest {

    private static final byte[] CONTENT = "%PDF-1.7 acknowledgement".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private ClientService clientService;
    @Autowired
    private DocumentRepository documentRepository;

    private String pan;

    @BeforeEach
    void createClient() {
        UploadFixtures.signInAs("admin", "ADMIN");
        pan = UploadFixtures.nextPan();
        UploadFixtures.createClient(clientService, pan, "Acme Traders Pvt Ltd");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private TextField field(String label) {
        return $(TextField.class).withLabel(label).single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Client> panField() {
        return $(ComboBox.class).withLabel("PAN").single();
    }

    @SuppressWarnings("unchecked")
    private Select<DocumentType> documentType() {
        return $(Select.class).single();
    }

    private void click(String caption) {
        test($(Button.class).withText(caption).first()).click();
    }

    private void selectFile(String name, byte[] content) {
        test($(Upload.class).single()).upload(name, "application/pdf", content);
    }

    private void fillValid(String location) {
        test(panField()).selectItem(pan);
        test(field("Assessment Year")).setValue("2025");
        test(field("Financial Year")).setValue("2024");
        test(documentType()).selectItem("IT Acknowledgement");
        test(field("Location")).setValue(location);
    }

    private boolean errorShown(String text) {
        return $(Span.class).all().stream().anyMatch(s -> s.isVisible() && text.equals(s.getText()));
    }

    private boolean notificationShown(String text) {
        return $(Notification.class).all().stream()
                .anyMatch(n -> n.getElement().getOuterHTML().contains(text)
                        || text.equals(n.getElement().getProperty("text", "")));
    }

    @Test
    void trigger_adminOpensUploadFromDashboard() {
        navigate(AdminDashboardView.class);

        test($(RouterLink.class).withText("Upload Documents").first()).click();

        assertInstanceOf(UploadDocumentView.class, getCurrentView());
    }

    @Test
    void step1_formShowsAllFieldsAsRequired() {
        navigate(UploadDocumentView.class);

        assertTrue($(Upload.class).single().isVisible());
        assertTrue(panField().isRequiredIndicatorVisible());
        for (String label : new String[] {"Assessment Year", "Financial Year", "Location"}) {
            assertTrue(field(label).isRequiredIndicatorVisible(), label);
        }
        assertTrue(documentType().isRequiredIndicatorVisible());
        // BR-03: the dropdown offers exactly the allowed types
        assertEquals(Arrays.stream(DocumentType.values()).map(DocumentType::label).toList(),
                test(documentType()).getSuggestions());
    }

    @Test
    void mainFlow_uploadStoresFileAndReturnsToDashboard() throws IOException {
        navigate(UploadDocumentView.class);
        String location = UploadFixtures.nextLocation();
        selectFile("itr-2025.pdf", CONTENT);
        fillValid(location);

        click("Upload");

        Path stored = STORE.resolve(location).resolve("itr-2025.pdf");
        assertArrayEquals(CONTENT, Files.readAllBytes(stored));
        assertTrue(documentRepository.findByLocationAndDocumentIdentifier(location, "itr-2025.pdf").isPresent());
        assertTrue(notificationShown("Document uploaded successfully."));
        assertInstanceOf(AllDocumentsView.class, getCurrentView());
    }

    @Test
    void selectingPanShowsClientNameAndSuggestsLocation() {
        navigate(UploadDocumentView.class);

        test(panField()).selectItem(pan);
        test(field("Assessment Year")).setValue("2025");

        assertEquals(pan, panField().getValue().getPanNumber());
        assertEquals("Client: Acme Traders Pvt Ltd", panField().getHelperText());
        assertEquals(pan + "/2025", field("Location").getValue());
    }

    @Test
    void panDropdownListsClientsAndFiltersByPanOrName() {
        navigate(UploadDocumentView.class);

        assertTrue(test(panField()).getSuggestions().contains(pan));

        test(panField()).setFilter(pan.substring(0, 7).toLowerCase());
        assertTrue(test(panField()).getSuggestions().contains(pan));

        test(panField()).setFilter("acme traders");
        assertTrue(test(panField()).getSuggestions().contains(pan));

        test(panField()).setFilter("no such client");
        assertTrue(test(panField()).getSuggestions().isEmpty());
    }

    @Test
    void af1_panCanOnlyBeAnExistingClient() {
        navigate(UploadDocumentView.class);
        String unknown = UploadFixtures.nextPan();

        assertFalse(test(panField()).getSuggestions().contains(unknown));
        assertFalse(panField().isAllowCustomValue());
    }

    @Test
    void af2_missingFieldsAreListedAndHighlighted() {
        navigate(UploadDocumentView.class);

        click("Upload");

        assertTrue(errorShown("Please fill in the required fields: File, PAN, Assessment Year, Financial Year, "
                + "Document Type, Location."));
        assertTrue(errorShown("Select a file"));
        assertTrue(panField().isInvalid());
        assertTrue(field("Assessment Year").isInvalid());
        assertTrue(field("Financial Year").isInvalid());
        assertTrue(documentType().isInvalid());
        assertTrue(field("Location").isInvalid());
    }

    @Test
    void fieldErrorsClearWhenTheFieldIsFilledIn() {
        navigate(UploadDocumentView.class);
        click("Upload");
        assertTrue(field("Assessment Year").isInvalid());

        test(field("Assessment Year")).setValue("2025");
        selectFile("itr.pdf", CONTENT);

        assertFalse(field("Assessment Year").isInvalid());
        assertFalse(errorShown("Select a file"));
        assertTrue(field("Financial Year").isInvalid());
    }

    @Test
    void af4_existingFileAsksBeforeOverwriting() throws IOException {
        String location = UploadFixtures.nextLocation();
        Files.createDirectories(STORE.resolve(location));
        Files.writeString(STORE.resolve(location).resolve("itr.pdf"), "older version");
        navigate(UploadDocumentView.class);
        selectFile("itr.pdf", CONTENT);
        fillValid(location);

        click("Upload");

        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertTrue(dialog.isOpened());
        assertTrue(test(dialog).getText().startsWith("A file with this name already exists at the location. "
                + "Overwrite or choose a different location?"));
        assertEquals("older version", Files.readString(STORE.resolve(location).resolve("itr.pdf")));

        test(dialog).confirm();

        assertArrayEquals(CONTENT, Files.readAllBytes(STORE.resolve(location).resolve("itr.pdf")));
        assertInstanceOf(AllDocumentsView.class, getCurrentView());
    }

    @Test
    void af4_changeLocationKeepsTheExistingFile() throws IOException {
        String location = UploadFixtures.nextLocation();
        Files.createDirectories(STORE.resolve(location));
        Files.writeString(STORE.resolve(location).resolve("itr.pdf"), "older version");
        navigate(UploadDocumentView.class);
        selectFile("itr.pdf", CONTENT);
        fillValid(location);
        click("Upload");

        test($(ConfirmDialog.class).single()).cancel();

        assertEquals("older version", Files.readString(STORE.resolve(location).resolve("itr.pdf")));
        assertInstanceOf(UploadDocumentView.class, getCurrentView());

        String other = UploadFixtures.nextLocation();
        test(field("Location")).setValue(other);
        click("Upload");

        assertArrayEquals(CONTENT, Files.readAllBytes(STORE.resolve(other).resolve("itr.pdf")));
    }

    @Test
    void br04_anonymousUserIsSentToLogin() {
        SecurityContextHolder.clearContext();

        navigate(UploadDocumentView.ROUTE, ClientLoginView.class);

        assertInstanceOf(ClientLoginView.class, getCurrentView());
    }

    @Test
    void br04_clientUserCannotOpenUpload() {
        UploadFixtures.signInAs(pan, "CLIENT");

        try {
            navigate(UploadDocumentView.ROUTE, UploadDocumentView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof UploadDocumentView);
    }
}
