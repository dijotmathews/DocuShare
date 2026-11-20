package com.dijo.admin;

import com.dijo.client.Client;
import com.dijo.client.ClientDetails;
import com.dijo.client.ClientService;
import com.dijo.client.ClientValidationException;
import com.dijo.client.DuplicatePanException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

/**
 * UC-006: client list with add / edit form. Admin only (BR-08).
 */
@Route(value = ManageClientsView.ROUTE, autoLayout = false)
@PageTitle("Manage Clients | DocuShare")
@RolesAllowed("ADMIN")
public class ManageClientsView extends Main {

    public static final String ROUTE = "admin/clients";
    static final String CREATED_MESSAGE = "Client created successfully.";
    static final String UPDATED_MESSAGE = "Client updated successfully.";
    static final String PASSWORD_RESET_MESSAGE = "Password reset.";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ClientService clientService;
    private final Grid<Client> grid = new Grid<>();
    private final ClientForm form = new ClientForm();

    public ManageClientsView(AuthenticationContext authenticationContext, ClientService clientService) {
        this.clientService = clientService;
        addClassName("dashboard");

        H2 title = new H2("Manage Clients");
        Button addClient = new Button("Add New Client", VaadinIcon.PLUS.create(), e -> openCreateForm());
        addClient.addThemeVariants(ButtonVariant.PRIMARY);
        Div toolbar = new Div(title, addClient);
        toolbar.addClassName("manage-clients__toolbar");

        configureGrid();
        form.setOnCreate(this::create);
        form.setOnSave(this::save);
        form.setOnCancel(this::closeForm);
        form.setOnResetPassword(this::confirmResetPassword);
        form.setVisible(false);

        Div body = new Div(grid, form);
        body.addClassName("manage-clients__body");

        Div content = new Div(toolbar, body);
        content.addClassName("dashboard__content");
        content.addClassName("manage-clients");

        add(new AdminHeader(authenticationContext), content);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addClassName("manage-clients__grid");
        grid.addThemeVariants(GridVariant.ROW_STRIPES);
        grid.addColumn(Client::getPanNumber).setHeader("PAN")
                .setComparator(Client::getPanNumber).setSortable(true).setAutoWidth(true);
        grid.addColumn(c -> Objects.toString(c.getClientName(), "")).setHeader("Client Name")
                .setComparator(Comparator.comparing(c -> Objects.toString(c.getClientName(), ""),
                        String.CASE_INSENSITIVE_ORDER))
                .setSortable(true).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(c -> c.getDateOfIncorporation().format(DATE_FORMAT)).setHeader("Date of Incorporation")
                .setComparator(Client::getDateOfIncorporation).setSortable(true).setAutoWidth(true);
        grid.addColumn(Client::getEmail).setHeader("Email")
                .setComparator(Client::getEmail).setSortable(true).setAutoWidth(true);
        grid.addColumn(Client::getContactNumber).setHeader("Contact Number")
                .setComparator(Client::getContactNumber).setSortable(true).setAutoWidth(true);
        grid.addColumn(c -> Objects.toString(c.getWhatsAppNumber(), "")).setHeader("WhatsApp")
                .setComparator(Comparator.comparing(c -> Objects.toString(c.getWhatsAppNumber(), "")))
                .setSortable(true).setAutoWidth(true);
        grid.setEmptyStateText("No clients yet. Click \"Add New Client\" to create one.");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                openEditForm(e.getValue());
            } else if (form.getEditedClient() != null) {
                closeForm();
            }
        });
    }

    private void refreshGrid() {
        grid.setItems(clientService.findAll());
    }

    private void openCreateForm() {
        grid.asSingleSelect().clear();
        form.showCreate();
        showForm();
    }

    private void openEditForm(Client client) {
        form.showEdit(client);
        showForm();
    }

    private void showForm() {
        form.setVisible(true);
        // On narrow screens the form sits below the list; bring it into view
        form.scrollIntoView();
    }

    private void closeForm() {
        form.setVisible(false);
        grid.asSingleSelect().clear();
    }

    private void create(ClientDetails details) {
        try {
            ClientService.CreatedClient created = clientService.createClient(details);
            refreshGrid();
            // Step 16: show the new client's details
            selectByPan(created.client().getPanNumber());
            Notification.show(CREATED_MESSAGE, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.SUCCESS);
            showCredentials(CREATED_MESSAGE, created.client().getPanNumber(), created.defaultPassword());
        } catch (DuplicatePanException ex) {
            // AF-1: offer to edit the existing client instead
            form.showErrors(ex, "Edit existing client", () -> selectByPan(ex.getPanNumber()));
        } catch (ClientValidationException ex) {
            form.showErrors(ex);
        }
    }

    private void save(Client client, ClientDetails details) {
        try {
            Client updated = clientService.updateClient(client.getId(), details);
            refreshGrid();
            selectByPan(updated.getPanNumber());
            Notification.show(UPDATED_MESSAGE, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.SUCCESS);
        } catch (ClientValidationException ex) {
            form.showErrors(ex);
        }
    }

    private void selectByPan(String pan) {
        clientService.findByPan(pan).ifPresent(client -> {
            // Select the instance held by the grid so the row is highlighted
            grid.getListDataView().getItems()
                    .filter(c -> c.getId().equals(client.getId()))
                    .findFirst()
                    .ifPresentOrElse(grid::select, () -> openEditForm(client));
        });
    }

    /** Asks before replacing the client's password, since the old one stops working. */
    private void confirmResetPassword(Client client) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Reset password for " + client.getPanNumber() + "?");
        dialog.setText("The password will be set back to the default (PAN@DDMMYYYY of the date of "
                + "incorporation). The client's current password stops working immediately.");
        dialog.setConfirmText("Reset password");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelable(true);
        dialog.addConfirmListener(e -> {
            String password = clientService.resetPassword(client.getId());
            Notification.show(PASSWORD_RESET_MESSAGE, 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.SUCCESS);
            showCredentials("Password reset for " + client.getPanNumber(), client.getPanNumber(), password);
        });
        dialog.open();
    }

    /**
     * Step 14 / BR-07: show the default password (after creation or a reset)
     * so the admin can pass it on.
     */
    private void showCredentials(String header, String pan, String password) {
        Dialog dialog = new Dialog();
        dialog.addClassName("default-password-dialog");
        dialog.setHeaderTitle(header);

        Paragraph intro = new Paragraph("Share these login details with the client:");
        Div credentials = new Div(
                labelled("PAN (username)", pan),
                labelled("Default password", password));
        credentials.addClassName("default-password-dialog__credentials");
        Paragraph note = new Paragraph("The default password is the PAN, '@' and the date of incorporation "
                + "as DDMMYYYY. Ask the client to keep it private.");
        note.addClassName("default-password-dialog__note");
        dialog.add(intro, credentials, note);

        Button close = new Button("Done", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(close);
        dialog.setCloseOnOutsideClick(false);
        dialog.open();
    }

    private static Div labelled(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("default-password-dialog__label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("default-password-dialog__value");
        Div row = new Div(labelSpan, valueSpan);
        row.addClassName("default-password-dialog__row");
        return row;
    }
}
