package com.dijo.usecase006;

import com.dijo.client.Client;
import com.dijo.client.ClientDetails;
import com.dijo.client.ClientField;
import com.dijo.client.ClientRepository;
import com.dijo.client.ClientService;
import com.dijo.client.ClientValidationException;
import com.dijo.client.DuplicatePanException;
import com.dijo.user.Role;
import com.dijo.user.User;
import com.dijo.user.UserRepository;
import com.dijo.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-006 Manage Clients: creating and updating clients in ClientService
 * (Main Flow steps 9–14, AF-1 to AF-4, BR-01 to BR-08).
 */
@SpringBootTest
class ManageClientsServiceTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void signInAsAdmin() {
        signInAs("admin", "ADMIN");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    private static ClientDetails validDetails(String pan) {
        ClientDetails d = new ClientDetails();
        d.setPanNumber(pan);
        d.setClientName("  Acme Traders Pvt Ltd ");
        d.setDateOfIncorporation(LocalDate.of(2015, 4, 1));
        d.setAddress("12 MG Road, Kochi");
        d.setEmail("accounts@acme.in");
        d.setContactNumber("+91 98765 43210");
        d.setWhatsAppNumber("9876543210");
        return d;
    }

    private void assertNothingCreated(String pan) {
        assertTrue(clientRepository.findByPanNumber(pan).isEmpty());
        assertTrue(userRepository.findByUsername(pan).isEmpty());
    }

    @Test
    void mainFlow_createsClientAndClientUserWithDefaultPassword() {
        String pan = TestPans.next();

        ClientService.CreatedClient created = clientService.createClient(validDetails(pan));

        Client client = clientRepository.findByPanNumber(pan).orElseThrow();
        assertEquals("Acme Traders Pvt Ltd", client.getClientName());
        assertEquals(LocalDate.of(2015, 4, 1), client.getDateOfIncorporation());
        assertEquals("12 MG Road, Kochi", client.getAddress());
        assertEquals("accounts@acme.in", client.getEmail());
        assertEquals("+91 98765 43210", client.getContactNumber());
        assertEquals("9876543210", client.getWhatsAppNumber());

        // BR-06: user account with role CLIENT, username = PAN
        User user = userRepository.findByUsername(pan).orElseThrow();
        assertEquals(Role.CLIENT, user.getRole());
        assertEquals(created.client().getId(), client.getId());

        // BR-07: default password PAN@DDMMYYYY, returned for the admin and stored hashed
        String password = created.defaultPassword();
        assertEquals(pan + "@01042015", password);
        assertNotEquals(password, user.getPassword());
        assertTrue(passwordEncoder.matches(password, user.getPassword()));
    }

    @Test
    void mainFlow_panIsUpperCasedAndWhatsAppIsOptional() {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan.toLowerCase());
        details.setWhatsAppNumber("  ");

        clientService.createClient(details);

        Client client = clientRepository.findByPanNumber(pan).orElseThrow();
        assertNull(client.getWhatsAppNumber());
    }

    @Test
    void br07_defaultPasswordUsesPanAndDateOfIncorporationWithoutSeparators() {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan.toLowerCase());
        details.setDateOfIncorporation(LocalDate.of(1998, 12, 9));

        String password = clientService.createClient(details).defaultPassword();

        assertEquals(pan + "@09121998", password);
        assertTrue(passwordEncoder.matches(password, userRepository.findByUsername(pan).orElseThrow().getPassword()));
    }

    @Test
    void af1_br02_duplicatePanIsRejected() {
        String pan = TestPans.next();
        clientService.createClient(validDetails(pan));
        long clients = clientRepository.count();

        DuplicatePanException ex = assertThrows(DuplicatePanException.class,
                () -> clientService.createClient(validDetails(pan)));

        assertEquals("A client with PAN " + pan + " already exists.", ex.getMessage());
        assertTrue(ex.getFieldErrors().containsKey(ClientField.PAN));
        assertEquals(clients, clientRepository.count());
    }

    @Test
    void af1_br02_panAlreadyUsedByAUserAccountIsRejected() {
        String pan = TestPans.next();
        userService.createUser(pan, "secret", Role.CLIENT);

        assertThrows(DuplicatePanException.class, () -> clientService.createClient(validDetails(pan)));
        assertTrue(clientRepository.findByPanNumber(pan).isEmpty());
    }

    @Test
    void af2_br01_missingMandatoryFieldsAreAllListed() {
        ClientDetails details = new ClientDetails();
        details.setAddress("12 MG Road");

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals("Please fill in the required fields: Client Name, PAN, Date of Incorporation, Email, Contact Number.",
                ex.getMessage());
        assertEquals(Set.of(ClientField.CLIENT_NAME, ClientField.PAN, ClientField.DATE_OF_INCORPORATION, ClientField.EMAIL,
                ClientField.CONTACT_NUMBER), ex.getFieldErrors().keySet());
    }

    @Test
    void af2_postcondition_nothingIsCreatedWhenValidationFails() {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan);
        details.setAddress(" ");

        assertThrows(ClientValidationException.class, () -> clientService.createClient(details));
        assertNothingCreated(pan);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "a@b", "@acme.in", "accounts@", "a b@acme.in"})
    void af3_br04_invalidEmailIsRejected(String email) {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan);
        details.setEmail(email);

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals("Please enter a valid email address.", ex.getMessage());
        assertEquals(Set.of(ClientField.EMAIL), ex.getFieldErrors().keySet());
        assertNothingCreated(pan);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "98765abcde", "phone", "+91 98765 4321098765", "--9876543210"})
    void af4_br05_invalidContactNumberIsRejected(String phone) {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan);
        details.setContactNumber(phone);

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals("Please enter a valid contact number.", ex.getMessage());
        assertEquals(Set.of(ClientField.CONTACT_NUMBER), ex.getFieldErrors().keySet());
        assertNothingCreated(pan);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+91 98765 43210", "+91-484-2345678", "0484 2345678"})
    void br05_validContactNumbersAreAccepted(String phone) {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan);
        details.setContactNumber(phone);

        clientService.createClient(details);

        assertEquals(phone, clientRepository.findByPanNumber(pan).orElseThrow().getContactNumber());
    }

    @Test
    void br05_invalidWhatsAppNumberIsRejected() {
        ClientDetails details = validDetails(TestPans.next());
        details.setWhatsAppNumber("123");

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals(Set.of(ClientField.WHATSAPP_NUMBER), ex.getFieldErrors().keySet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCD1234F", "ABCDE12345", "12345ABCDE", "ABCDE-1234F"})
    void br03_invalidPanFormatIsRejected(String pan) {
        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(validDetails(pan)));

        assertEquals(Set.of(ClientField.PAN), ex.getFieldErrors().keySet());
        assertFalse(userRepository.findByUsername(pan).isPresent());
    }

    @Test
    void futureDateOfIncorporationIsRejected() {
        ClientDetails details = validDetails(TestPans.next());
        details.setDateOfIncorporation(LocalDate.now().plusDays(1));

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals(Set.of(ClientField.DATE_OF_INCORPORATION), ex.getFieldErrors().keySet());
    }

    @Test
    void br01_clientNameIsMandatoryOnCreateAndEdit() {
        String pan = TestPans.next();
        ClientDetails details = validDetails(pan);
        details.setClientName(" ");

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));
        assertEquals("Please fill in the required fields: Client Name.", ex.getMessage());
        assertEquals(Set.of(ClientField.CLIENT_NAME), ex.getFieldErrors().keySet());
        assertNothingCreated(pan);

        Client client = clientService.createClient(validDetails(pan)).client();
        ClientDetails changes = client.toDetails();
        changes.setClientName(null);
        assertThrows(ClientValidationException.class, () -> clientService.updateClient(client.getId(), changes));
    }

    @Test
    void clientNameLongerThan200CharactersIsRejected() {
        ClientDetails details = validDetails(TestPans.next());
        details.setClientName("A".repeat(201));

        ClientValidationException ex = assertThrows(ClientValidationException.class,
                () -> clientService.createClient(details));

        assertEquals(Set.of(ClientField.CLIENT_NAME), ex.getFieldErrors().keySet());
    }

    @Test
    void edit_updatesDetailsButNotPan() {
        String pan = TestPans.next();
        Client client = clientService.createClient(validDetails(pan)).client();
        ClientDetails changes = client.toDetails();
        changes.setPanNumber("ZZZZZ9999Z");
        changes.setEmail("new@acme.in");
        changes.setClientName("Acme Global LLP");
        changes.setContactNumber("0484 2345678");

        clientService.updateClient(client.getId(), changes);

        Client updated = clientRepository.findById(client.getId()).orElseThrow();
        assertEquals(pan, updated.getPanNumber());
        assertEquals("new@acme.in", updated.getEmail());
        assertEquals("Acme Global LLP", updated.getClientName());
        assertEquals("0484 2345678", updated.getContactNumber());
        assertTrue(clientRepository.findByPanNumber("ZZZZZ9999Z").isEmpty());
    }

    @Test
    void edit_validatesTheSameRules() {
        Client client = clientService.createClient(validDetails(TestPans.next())).client();
        ClientDetails changes = client.toDetails();
        changes.setEmail("broken");

        assertThrows(ClientValidationException.class, () -> clientService.updateClient(client.getId(), changes));
        assertEquals("accounts@acme.in", clientRepository.findById(client.getId()).orElseThrow().getEmail());
    }

    @Test
    void af5_resetPasswordReplacesTheClientPassword() {
        String pan = TestPans.next();
        ClientService.CreatedClient created = clientService.createClient(validDetails(pan));

        // The client changed their password (simulated), then lost it
        userService.changePassword(userRepository.findByUsername(pan).orElseThrow(), "my-own-secret");
        // ... and the date of incorporation was corrected since creation
        ClientDetails changes = created.client().toDetails();
        changes.setDateOfIncorporation(LocalDate.of(2016, 3, 31));
        clientService.updateClient(created.client().getId(), changes);

        String newPassword = clientService.resetPassword(created.client().getId());

        String stored = userRepository.findByUsername(pan).orElseThrow().getPassword();
        assertEquals(pan + "@31032016", newPassword);
        assertTrue(passwordEncoder.matches(newPassword, stored));
        assertFalse(passwordEncoder.matches("my-own-secret", stored));
    }

    @Test
    void af5_onlyAdminsCanResetPasswords() {
        Client client = clientService.createClient(validDetails(TestPans.next())).client();
        signInAs(client.getPanNumber(), "CLIENT");

        assertThrows(AccessDeniedException.class, () -> clientService.resetPassword(client.getId()));
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> clientService.resetPassword(client.getId()));
    }

    @Test
    void br08_anonymousUserCannotManageClients() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> clientService.createClient(validDetails(TestPans.next())));
        assertThrows(AuthenticationCredentialsNotFoundException.class, clientService::findAll);
    }

    @Test
    void br08_clientUserCannotManageClients() {
        signInAs("ABCDE1234F", "CLIENT");

        assertThrows(AccessDeniedException.class, () -> clientService.createClient(validDetails(TestPans.next())));
        assertThrows(AccessDeniedException.class, clientService::findAll);
    }
}
