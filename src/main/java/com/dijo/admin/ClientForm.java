package com.dijo.admin;

import com.dijo.client.Client;
import com.dijo.client.ClientDetails;
import com.dijo.client.ClientField;
import com.dijo.client.ClientValidationException;
import com.dijo.ui.ErrorSummary;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Add / edit form for a client (UC-006). Validation happens in ClientService;
 * this form shows the returned errors on the matching fields.
 */
class ClientForm extends Div {

    static final String CREATE_LABEL = "Create Client";
    static final String SAVE_LABEL = "Save Changes";
    static final String RESET_PASSWORD_LABEL = "Reset password";

    private final H3 title = new H3();
    private final ErrorSummary errorSummary = new ErrorSummary();

    private final TextField clientName = new TextField(ClientField.CLIENT_NAME.label());
    private final TextField pan = new TextField(ClientField.PAN.label());
    private final DatePicker dateOfIncorporation = new DatePicker(ClientField.DATE_OF_INCORPORATION.label());
    private final TextArea address = new TextArea(ClientField.ADDRESS.label());
    private final EmailField email = new EmailField(ClientField.EMAIL.label());
    private final TextField contactNumber = new TextField(ClientField.CONTACT_NUMBER.label());
    private final TextField whatsAppNumber = new TextField(ClientField.WHATSAPP_NUMBER.label() + " (optional)");

    private final Map<ClientField, HasValidation> fields = new EnumMap<>(ClientField.class);

    private final Button resetPassword = new Button(RESET_PASSWORD_LABEL);
    private final Div account = new Div();
    private final Button submit = new Button(CREATE_LABEL);
    private final Button cancel = new Button("Cancel");

    private Client editedClient;
    private Consumer<ClientDetails> onCreate = d -> { };
    private EditHandler onSave = (c, d) -> { };
    private Consumer<Client> onResetPassword = c -> { };

    @FunctionalInterface
    interface EditHandler {
        void save(Client client, ClientDetails details);
    }

    ClientForm() {
        addClassNames("client-form", "form-card");

        clientName.setPlaceholder("e.g. Acme Traders Pvt Ltd");
        clientName.setMaxLength(200);
        pan.setPlaceholder("e.g. ABCDE1234F");
        pan.setHelperText("Used as the client's login username");
        pan.setMaxLength(10);
        pan.addValueChangeListener(e -> {
            String upper = e.getValue() == null ? "" : e.getValue().toUpperCase();
            if (!upper.equals(e.getValue())) {
                pan.setValue(upper);
            }
        });
        dateOfIncorporation.setMax(LocalDate.now());
        contactNumber.setPlaceholder("e.g. +91 98765 43210");
        whatsAppNumber.setPlaceholder("e.g. +91 98765 43210");

        fields.put(ClientField.CLIENT_NAME, clientName);
        fields.put(ClientField.PAN, pan);
        fields.put(ClientField.DATE_OF_INCORPORATION, dateOfIncorporation);
        fields.put(ClientField.ADDRESS, address);
        fields.put(ClientField.EMAIL, email);
        fields.put(ClientField.CONTACT_NUMBER, contactNumber);
        fields.put(ClientField.WHATSAPP_NUMBER, whatsAppNumber);

        // BR-01: all fields except WhatsApp number are mandatory
        clientName.setRequiredIndicatorVisible(true);
        pan.setRequiredIndicatorVisible(true);
        dateOfIncorporation.setRequiredIndicatorVisible(true);
        address.setRequiredIndicatorVisible(true);
        email.setRequiredIndicatorVisible(true);
        contactNumber.setRequiredIndicatorVisible(true);

        // Errors come from the server-side validation, not the components
        clientName.setManualValidation(true);
        pan.setManualValidation(true);
        dateOfIncorporation.setManualValidation(true);
        address.setManualValidation(true);
        email.setManualValidation(true);
        contactNumber.setManualValidation(true);
        whatsAppNumber.setManualValidation(true);

        FormLayout layout = new FormLayout(clientName, pan, dateOfIncorporation, address, email, contactNumber, whatsAppNumber);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        submit.addThemeVariants(ButtonVariant.PRIMARY);
        submit.addClickListener(e -> submit());
        Div buttons = new Div(submit, cancel);
        buttons.addClassName("client-form__buttons");

        // Client login: only for existing clients
        Span accountTitle = new Span("Client login");
        accountTitle.addClassName("client-form__account-title");
        Span accountText = new Span("Lost password? Generate a new one to share with the client.");
        accountText.addClassName("client-form__account-text");
        resetPassword.addThemeVariants(ButtonVariant.SMALL);
        resetPassword.addClickListener(e -> onResetPassword.accept(editedClient));
        account.add(new Div(accountTitle, accountText), resetPassword);
        account.addClassName("client-form__account");

        // A field's error goes away as soon as the admin edits it
        fields.values().forEach(f -> ((HasValue<?, ?>) f).addValueChangeListener(e -> {
            f.setInvalid(false);
            f.setErrorMessage(null);
        }));

        add(title, errorSummary, layout, buttons, account);
        showCreate();
    }

    void setOnCreate(Consumer<ClientDetails> onCreate) {
        this.onCreate = onCreate;
    }

    void setOnSave(EditHandler onSave) {
        this.onSave = onSave;
    }

    void setOnResetPassword(Consumer<Client> onResetPassword) {
        this.onResetPassword = onResetPassword;
    }

    void setOnCancel(Runnable onCancel) {
        cancel.addClickListener(e -> onCancel.run());
    }

    /** Empty form for a new client (step 1). */
    void showCreate() {
        editedClient = null;
        title.setText("New Client");
        submit.setText(CREATE_LABEL);
        pan.setReadOnly(false);
        account.setVisible(false);
        fill(new ClientDetails());
        clearErrors();
    }

    /** Form pre-filled with an existing client's details. The PAN can't be changed. */
    void showEdit(Client client) {
        editedClient = client;
        title.setText(client.getClientName() == null ? "Client " + client.getPanNumber() : client.getClientName());
        submit.setText(SAVE_LABEL);
        pan.setReadOnly(true);
        account.setVisible(true);
        fill(client.toDetails());
        clearErrors();
    }

    Client getEditedClient() {
        return editedClient;
    }

    private void fill(ClientDetails d) {
        clientName.setValue(nullToEmpty(d.getClientName()));
        pan.setValue(nullToEmpty(d.getPanNumber()));
        dateOfIncorporation.setValue(d.getDateOfIncorporation());
        address.setValue(nullToEmpty(d.getAddress()));
        email.setValue(nullToEmpty(d.getEmail()));
        contactNumber.setValue(nullToEmpty(d.getContactNumber()));
        whatsAppNumber.setValue(nullToEmpty(d.getWhatsAppNumber()));
    }

    private ClientDetails read() {
        ClientDetails d = new ClientDetails();
        d.setClientName(clientName.getValue());
        d.setPanNumber(pan.getValue());
        d.setDateOfIncorporation(dateOfIncorporation.getValue());
        d.setAddress(address.getValue());
        d.setEmail(email.getValue());
        d.setContactNumber(contactNumber.getValue());
        d.setWhatsAppNumber(whatsAppNumber.getValue());
        return d;
    }

    private void submit() {
        clearErrors();
        if (editedClient == null) {
            onCreate.accept(read());
        } else {
            onSave.save(editedClient, read());
        }
    }

    /** Shows a validation failure: summary on top, message on each offending field. */
    void showErrors(ClientValidationException ex) {
        showErrors(ex, null, null);
    }

    /** Same as {@link #showErrors(ClientValidationException)}, with an extra action button in the summary. */
    void showErrors(ClientValidationException ex, String actionLabel, Runnable action) {
        clearErrors();
        errorSummary.show(ex.getMessage(), actionLabel, action);
        ex.getFieldErrors().forEach((field, message) -> {
            HasValidation component = fields.get(field);
            component.setErrorMessage(message);
            component.setInvalid(true);
        });
        // Move focus to the first offending field
        fields.entrySet().stream()
                .filter(e -> ex.getFieldErrors().containsKey(e.getKey()))
                .findFirst()
                .ifPresent(e -> ((Component) e.getValue()).getElement().callJsFunction("focus"));
    }

    private void clearErrors() {
        errorSummary.clear();
        fields.values().forEach(f -> {
            f.setInvalid(false);
            f.setErrorMessage(null);
        });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
