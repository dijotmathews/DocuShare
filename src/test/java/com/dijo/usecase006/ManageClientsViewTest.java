package com.dijo.usecase006;

import com.dijo.admin.AdminDashboardView;
import com.dijo.admin.ManageClientsView;
import com.dijo.client.Client;
import com.dijo.client.ClientRepository;
import com.dijo.security.ClientLoginView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-006 Manage Clients: client list, add/edit form and default password
 * display (Main Flow steps 1–16, AF-1 to AF-4, BR-01, BR-07, BR-08).
 */
@SpringBootTest
class ManageClientsViewTest extends SpringBrowserlessTest {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private com.dijo.user.UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String username, String role) {
        var principal = User.withUsername(username).password("n/a").roles(role).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    private ManageClientsView openAsAdmin() {
        signInAs("admin", "ADMIN");
        return navigate(ManageClientsView.class);
    }

    private TextField clientName() {
        return $(TextField.class).withLabel("Client Name").single();
    }

    private TextField pan() {
        return $(TextField.class).withLabel("PAN").single();
    }

    private DatePicker date() {
        return $(DatePicker.class).withLabel("Date of Incorporation").single();
    }

    private TextArea address() {
        return $(TextArea.class).withLabel("Address").single();
    }

    private EmailField email() {
        return $(EmailField.class).withLabel("Email").single();
    }

    private TextField contact() {
        return $(TextField.class).withLabel("Contact Number").single();
    }

    private TextField whatsApp() {
        return $(TextField.class).withLabel("WhatsApp Number (optional)").single();
    }

    private void click(String caption) {
        test($(Button.class).withText(caption).first()).click();
    }

    private void fillValid(String panNumber) {
        test(clientName()).setValue("Acme Traders Pvt Ltd");
        test(pan()).setValue(panNumber);
        test(date()).setValue(LocalDate.of(2015, 4, 1));
        test(address()).setValue("12 MG Road, Kochi");
        email().setValue("accounts@acme.in");
        test(contact()).setValue("+91 98765 43210");
        test(whatsApp()).setValue("9876543210");
    }

    private boolean errorShown(String text) {
        return $(Span.class).all().stream().anyMatch(s -> s.isVisible() && text.equals(s.getText()));
    }

    @SuppressWarnings("unchecked")
    private Grid<Client> grid() {
        return $(Grid.class).single();
    }

    private List<String> gridPans() {
        return grid().getListDataView().getItems().map(Client::getPanNumber).toList();
    }

    @Test
    void precondition_adminReachesManageClientsFromDashboard() {
        signInAs("admin", "ADMIN");
        navigate(AdminDashboardView.class);

        test($(RouterLink.class).withText("Clients").first()).click();

        assertInstanceOf(ManageClientsView.class, getCurrentView());
    }

    @Test
    void mainFlowStep1_addNewClientShowsCreationForm() {
        openAsAdmin();

        click("Add New Client");

        assertTrue(pan().isVisible());
        assertFalse(pan().isReadOnly());
        assertTrue($(Button.class).withText("Create Client").single().isVisible());
        // BR-01: all fields except WhatsApp are marked mandatory
        assertTrue(clientName().isRequiredIndicatorVisible());
        assertTrue(pan().isRequiredIndicatorVisible());
        assertTrue(date().isRequiredIndicatorVisible());
        assertTrue(address().isRequiredIndicatorVisible());
        assertTrue(email().isRequiredIndicatorVisible());
        assertTrue(contact().isRequiredIndicatorVisible());
        assertFalse(whatsApp().isRequiredIndicatorVisible());
    }

    @Test
    void mainFlow_createClientShowsDefaultPasswordAndSuccess() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);

        click("Create Client");

        // Steps 12–13: records created
        assertTrue(clientRepository.findByPanNumber(panNumber).isPresent());
        var user = userRepository.findByUsername(panNumber).orElseThrow();

        // Step 14 / BR-07: dialog shows the default password, which is the real one
        Dialog dialog = $(Dialog.class).single();
        assertTrue(dialog.isOpened());
        assertEquals("Client created successfully.", dialog.getHeaderTitle());
        List<String> values = $(Span.class, dialog).withClassName("default-password-dialog__value").all()
                .stream().map(Span::getText).toList();
        assertEquals(panNumber, values.get(0));
        assertEquals(panNumber + "@01042015", values.get(1));
        assertTrue(passwordEncoder.matches(values.get(1), user.getPassword()));

        // Step 15: success notification
        assertTrue($(Notification.class).all().stream()
                .anyMatch(n -> n.getElement().getProperty("text", "").equals("Client created successfully.")
                        || n.getElement().getOuterHTML().contains("Client created successfully.")));

        // Step 16: client appears in the list and its details are shown
        assertTrue(gridPans().contains(panNumber));
        assertEquals("Acme Traders Pvt Ltd", grid().asSingleSelect().getValue().getClientName());
        assertEquals("Acme Traders Pvt Ltd", clientName().getValue());
        assertEquals(panNumber, grid().asSingleSelect().getValue().getPanNumber());
        assertTrue(pan().isReadOnly());
        assertEquals(panNumber, pan().getValue());
    }

    @Test
    void af1_duplicatePanShowsErrorAndOffersToEditExisting() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);
        click("Create Client");
        test($(Button.class).withText("Done").single()).click();

        click("Add New Client");
        fillValid(panNumber);
        click("Create Client");

        assertTrue(errorShown("A client with PAN " + panNumber + " already exists."));
        assertTrue(pan().isInvalid());

        click("Edit existing client");

        assertEquals(panNumber, grid().asSingleSelect().getValue().getPanNumber());
        assertTrue(pan().isReadOnly());
    }

    @Test
    void af2_missingFieldsAreListedAndHighlighted() {
        openAsAdmin();
        click("Add New Client");
        test(pan()).setValue(TestPans.next());
        test(address()).setValue("12 MG Road");

        click("Create Client");

        assertTrue(errorShown("Please fill in the required fields: Client Name, Date of Incorporation, Email, Contact Number."));
        assertTrue(clientName().isInvalid());
        assertTrue(date().isInvalid());
        assertTrue(email().isInvalid());
        assertTrue(contact().isInvalid());
        assertFalse(pan().isInvalid());
        assertFalse(address().isInvalid());
        assertFalse(whatsApp().isInvalid());
        assertTrue($(Dialog.class).all().isEmpty());
    }

    @Test
    void af3_invalidEmailIsHighlighted() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);
        email().setValue("not-an-email");

        click("Create Client");

        assertTrue(errorShown("Please enter a valid email address."));
        assertTrue(email().isInvalid());
        assertFalse(contact().isInvalid());
        assertTrue(clientRepository.findByPanNumber(panNumber).isEmpty());
    }

    @Test
    void af4_invalidContactNumberIsHighlighted() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);
        test(contact()).setValue("12ab");

        click("Create Client");

        assertTrue(errorShown("Please enter a valid contact number."));
        assertTrue(contact().isInvalid());
        assertFalse(email().isInvalid());
        assertTrue(clientRepository.findByPanNumber(panNumber).isEmpty());
    }

    @Test
    void errorsClearAfterCorrectionAndResubmit() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);
        email().setValue("broken");
        click("Create Client");
        assertTrue(email().isInvalid());

        email().setValue("fixed@acme.in");
        click("Create Client");

        assertFalse(email().isInvalid());
        assertTrue(clientRepository.findByPanNumber(panNumber).isPresent());
    }

    @Test
    void editClient_selectingRowOpensFormAndSavesChanges() {
        openAsAdmin();
        String panNumber = TestPans.next();
        click("Add New Client");
        fillValid(panNumber);
        click("Create Client");
        test($(Button.class).withText("Done").single()).click();

        test(contact()).setValue("0484 2345678");
        click("Save Changes");

        assertEquals("0484 2345678", clientRepository.findByPanNumber(panNumber).orElseThrow().getContactNumber());
        assertEquals(panNumber, pan().getValue());
    }

    @Test
    void fieldErrorClearsAsSoonAsTheFieldIsEdited() {
        openAsAdmin();
        click("Add New Client");
        click("Create Client");
        assertTrue(email().isInvalid());

        email().setValue("accounts@acme.in");

        assertFalse(email().isInvalid());
        assertTrue(contact().isInvalid());
    }

    private String createAndCloseDialog(String panNumber) {
        click("Add New Client");
        fillValid(panNumber);
        click("Create Client");
        test($(Button.class).withText("Done").single()).click();
        return panNumber;
    }

    @Test
    void af5_resetPasswordIsOnlyOfferedForExistingClients() {
        openAsAdmin();
        click("Add New Client");
        assertTrue($(Button.class).withText("Reset password").all().isEmpty(), "hidden on the create form");

        createAndCloseDialog(TestPans.next());

        assertTrue($(Button.class).withText("Reset password").single().isVisible());
    }

    @Test
    void af5_resetPasswordAsksFirstThenShowsTheNewPassword() {
        openAsAdmin();
        String panNumber = createAndCloseDialog(TestPans.next());
        String before = userRepository.findByUsername(panNumber).orElseThrow().getPassword();

        click("Reset password");
        ConfirmDialog confirm = $(ConfirmDialog.class).single();
        assertTrue(confirm.isOpened());
        assertEquals(before, userRepository.findByUsername(panNumber).orElseThrow().getPassword());

        test(confirm).confirm();

        Dialog dialog = $(Dialog.class).single();
        assertEquals("Password reset for " + panNumber, dialog.getHeaderTitle());
        List<String> values = $(Span.class, dialog).withClassName("default-password-dialog__value").all()
                .stream().map(Span::getText).toList();
        assertEquals(panNumber, values.get(0));
        assertTrue(passwordEncoder.matches(values.get(1),
                userRepository.findByUsername(panNumber).orElseThrow().getPassword()));
    }

    @Test
    void af5_cancellingTheResetKeepsThePassword() {
        openAsAdmin();
        String panNumber = createAndCloseDialog(TestPans.next());
        String before = userRepository.findByUsername(panNumber).orElseThrow().getPassword();

        click("Reset password");
        test($(ConfirmDialog.class).single()).cancel();

        assertEquals(before, userRepository.findByUsername(panNumber).orElseThrow().getPassword());
        assertTrue($(Dialog.class).all().stream().noneMatch(Dialog::isOpened));
    }

    @Test
    void br08_anonymousUserIsSentToLogin() {
        navigate("admin/clients", ClientLoginView.class);

        assertInstanceOf(ClientLoginView.class, getCurrentView());
    }

    @Test
    void br08_clientUserCannotOpenManageClients() {
        signInAs("ABCDE1234F", "CLIENT");

        try {
            navigate("admin/clients", ManageClientsView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof ManageClientsView);
    }
}
