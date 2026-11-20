package com.dijo.admin;

import com.dijo.client.Client;
import com.dijo.client.ClientService;
import com.dijo.document.DocumentAlreadyExistsException;
import com.dijo.document.DocumentField;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentStorageException;
import com.dijo.document.DocumentType;
import com.dijo.document.DocumentUpload;
import com.dijo.document.DocumentValidationException;
import com.dijo.ui.ErrorSummary;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;

/**
 * UC-003: upload a document and tag it with the client's PAN, years, type and
 * storage location. Admin only (BR-04).
 */
@Route(value = UploadDocumentView.ROUTE, autoLayout = false)
@PageTitle("Upload Document | DocuShare")
@RolesAllowed("ADMIN")
public class UploadDocumentView extends Main {

    public static final String ROUTE = "admin/documents/upload";
    static final String SUCCESS_MESSAGE = "Document uploaded successfully.";
    static final int MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024;

    private static final String PAN_HELPER = "Search by PAN or client name";

    private final DocumentService documentService;
    private final ClientService clientService;

    private final ErrorSummary errorSummary = new ErrorSummary();
    private final Upload upload = new Upload();
    private final Span fileError = new Span();
    private final ComboBox<Client> pan = new ComboBox<>(DocumentField.PAN.label());
    private final TextField assessmentYear = new TextField(DocumentField.ASSESSMENT_YEAR.label());
    private final TextField financialYear = new TextField(DocumentField.FINANCIAL_YEAR.label());
    private final Select<DocumentType> documentType = new Select<>();
    private final TextField location = new TextField(DocumentField.LOCATION.label());
    private final Map<DocumentField, HasValidation> fields = new EnumMap<>(DocumentField.class);

    /** The file received from the browser; set by the upload handler, cleared when removed. */
    private volatile SelectedFile selectedFile;
    /** Location typed by the admin; until then it is suggested from PAN and Assessment Year. */
    private boolean locationEditedByUser;

    private record SelectedFile(String name, byte[] content) {
    }

    public UploadDocumentView(AuthenticationContext authenticationContext, DocumentService documentService,
                              ClientService clientService) {
        this.documentService = documentService;
        this.clientService = clientService;
        addClassName("dashboard");

        H2 title = new H2("Upload Document");
        Paragraph intro = new Paragraph("Upload a tax document and tag it with the client's PAN so the client "
                + "can find it in their documents.");
        intro.addClassName("dashboard__intro");

        Div form = new Div(errorSummary, fileSection(), metadataLayout(), buttons());
        form.addClassNames("upload-form", "form-card");

        Div content = new Div(title, intro, form);
        content.addClassName("dashboard__content");
        content.addClassName("upload-document");

        add(new AdminHeader(authenticationContext), content);
    }

    private Component fileSection() {
        upload.setUploadHandler(UploadHandler.inMemory((metadata, data) -> {
            selectedFile = new SelectedFile(metadata.fileName(), data);
            // The upload request isn't a UI round trip; push the change to the UI
            upload.getUI().ifPresent(ui -> ui.access(this::clearFileError));
        }));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_SIZE_BYTES);
        upload.addFileRemovedListener(e -> selectedFile = null);
        upload.addFileRejectedListener(e -> showFileError(e.getErrorMessage()));
        upload.addClassName("upload-form__upload");
        // Distinct from the form's "Upload" button, which does the actual upload
        upload.setI18n(new UploadI18N()
                .setAddFiles(new UploadI18N.AddFiles().setOne("Select file...").setMany("Select files..."))
                .setDropFiles(new UploadI18N.DropFiles().setOne("or drop the file here").setMany("or drop files here")));

        Span label = new Span(DocumentField.FILE.label());
        label.addClassName("upload-form__file-label");
        label.getElement().setAttribute("aria-hidden", "true");
        Span hint = new Span("Up to 25 MB");
        hint.addClassName("upload-form__file-hint");
        fileError.addClassName("upload-form__file-error");
        fileError.setVisible(false);

        Div section = new Div(label, upload, hint, fileError);
        section.addClassName("upload-form__file");
        return section;
    }

    private Component metadataLayout() {
        // The PAN is picked from the existing clients, so it can't be mistyped (AF-1)
        pan.setItems(UploadDocumentView::matches, clientService.findAll());
        pan.setItemLabelGenerator(Client::getPanNumber);
        pan.setRenderer(LitRenderer.<Client>of("""
                        <div class="upload-form__client">
                          <span class="upload-form__client-pan">${item.pan}</span>
                          <span class="upload-form__client-name">${item.name}</span>
                        </div>""")
                .withProperty("pan", Client::getPanNumber)
                .withProperty("name", c -> c.getClientName() == null ? "" : c.getClientName()));
        pan.setPlaceholder("Select a client");
        pan.setHelperText(PAN_HELPER);
        pan.setClearButtonVisible(true);
        pan.addValueChangeListener(e -> {
            showClientName();
            suggestLocation();
        });

        assessmentYear.setPlaceholder("e.g. 2025");
        assessmentYear.setHelperText("4-digit year");
        financialYear.setPlaceholder("e.g. 2024");
        financialYear.setHelperText("4-digit year");
        for (TextField year : new TextField[] {assessmentYear, financialYear}) {
            year.setMaxLength(4);
            year.setAllowedCharPattern("[0-9]");
        }
        assessmentYear.addValueChangeListener(e -> suggestLocation());

        documentType.setLabel(DocumentField.DOCUMENT_TYPE.label());
        documentType.setItems(DocumentType.values());
        documentType.setItemLabelGenerator(DocumentType::label);
        documentType.setPlaceholder("Select a type");

        location.setPlaceholder("e.g. ABCDE1234F/2025");
        location.setHelperText("Folder in the document store");
        location.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                locationEditedByUser = !e.getValue().isBlank();
            }
        });

        fields.put(DocumentField.PAN, pan);
        fields.put(DocumentField.ASSESSMENT_YEAR, assessmentYear);
        fields.put(DocumentField.FINANCIAL_YEAR, financialYear);
        fields.put(DocumentField.DOCUMENT_TYPE, documentType);
        fields.put(DocumentField.LOCATION, location);

        // BR-01: all metadata fields are mandatory; errors come from the server
        pan.setRequiredIndicatorVisible(true);
        assessmentYear.setRequiredIndicatorVisible(true);
        financialYear.setRequiredIndicatorVisible(true);
        documentType.setRequiredIndicatorVisible(true);
        location.setRequiredIndicatorVisible(true);
        pan.setManualValidation(true);
        assessmentYear.setManualValidation(true);
        financialYear.setManualValidation(true);
        documentType.setManualValidation(true);
        location.setManualValidation(true);

        // A field's error goes away as soon as the admin edits it
        fields.values().forEach(f -> ((HasValue<?, ?>) f).addValueChangeListener(e -> {
            f.setInvalid(false);
            f.setErrorMessage(null);
        }));

        FormLayout layout = new FormLayout(pan, documentType, assessmentYear, financialYear, location);
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("32em", 2));
        layout.setColspan(location, 2);
        return layout;
    }

    private Component buttons() {
        Button submit = new Button("Upload", e -> submit(false));
        submit.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate(AdminDashboardView.class)));
        Div buttons = new Div(submit, cancel);
        buttons.addClassName("client-form__buttons");
        return buttons;
    }

    /** Shows the selected client's name under the PAN, so the admin can check it's the right one. */
    private void showClientName() {
        Client client = pan.getValue();
        pan.setHelperText(client == null || client.getClientName() == null ? PAN_HELPER
                : "Client: " + client.getClientName());
    }

    /** Matches a client whose PAN or name contains the typed text, ignoring case. */
    private static boolean matches(Client client, String filter) {
        String text = filter.trim().toLowerCase(Locale.ROOT);
        return client.getPanNumber().toLowerCase(Locale.ROOT).contains(text)
                || (client.getClientName() != null && client.getClientName().toLowerCase(Locale.ROOT).contains(text));
    }

    private String selectedPan() {
        return pan.getValue() == null ? null : pan.getValue().getPanNumber();
    }

    private void suggestLocation() {
        if (locationEditedByUser) {
            return;
        }
        String panValue = selectedPan();
        String year = assessmentYear.getValue();
        location.setValue(panValue != null && year.length() == 4 ? panValue + "/" + year : "");
    }

    private void submit(boolean overwrite) {
        clearErrors();
        SelectedFile file = selectedFile;
        DocumentUpload request = new DocumentUpload(selectedPan(), assessmentYear.getValue(),
                financialYear.getValue(), documentType.getValue(), location.getValue(),
                file == null ? null : file.name(), file == null ? null : file.content());
        try {
            documentService.upload(request, overwrite);
            // Steps 13–14: confirm and show the document list (UC-007)
            Notification.show(SUCCESS_MESSAGE, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.SUCCESS);
            getUI().ifPresent(ui -> ui.navigate(AllDocumentsView.class));
        } catch (DocumentValidationException ex) {
            showErrors(ex);
        } catch (DocumentAlreadyExistsException ex) {
            confirmOverwrite(ex);
        } catch (DocumentStorageException ex) {
            // AF-3
            errorSummary.show(ex.getMessage());
            location.setInvalid(true);
            location.setErrorMessage("Couldn't write to this location");
            location.focus();
        }
    }

    /** AF-4: ask whether to replace the existing file or pick another location. */
    private void confirmOverwrite(DocumentAlreadyExistsException ex) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("File already exists");
        dialog.setText(ex.getMessage() + " (" + ex.getStoredPath() + ")");
        dialog.setConfirmText("Overwrite");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> submit(true));
        dialog.setCancelable(true);
        dialog.setCancelText("Change location");
        dialog.addCancelListener(e -> location.focus());
        dialog.open();
    }

    private void showErrors(DocumentValidationException ex) {
        errorSummary.show(ex.getMessage());
        ex.getFieldErrors().forEach((field, message) -> {
            if (field == DocumentField.FILE) {
                showFileError(message.equals("Required") ? "Select a file" : message);
            } else {
                HasValidation component = fields.get(field);
                component.setErrorMessage(message);
                component.setInvalid(true);
            }
        });
        fields.entrySet().stream()
                .filter(e -> ex.getFieldErrors().containsKey(e.getKey()))
                .findFirst()
                .ifPresent(e -> ((Component) e.getValue()).getElement().callJsFunction("focus"));
    }

    private void showFileError(String message) {
        fileError.setText(message);
        fileError.setVisible(true);
        upload.addClassName("upload-form__upload--invalid");
    }

    private void clearFileError() {
        fileError.setVisible(false);
        fileError.setText("");
        upload.removeClassName("upload-form__upload--invalid");
    }

    private void clearErrors() {
        errorSummary.clear();
        clearFileError();
        fields.values().forEach(f -> {
            f.setInvalid(false);
            f.setErrorMessage(null);
        });
    }
}
