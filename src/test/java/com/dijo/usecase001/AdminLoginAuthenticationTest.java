package com.dijo.usecase001;

import com.dijo.security.LoginPortal;
import com.dijo.security.NotAdminException;
import com.dijo.security.PortalAuthenticationDetails;
import com.dijo.user.Role;
import com.dijo.user.User;
import com.dijo.user.UserRepository;
import com.dijo.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-001 Admin Login: credential validation and role checks
 * (Main Flow steps 4–6, AF-1, AF-2, BR-01, BR-03).
 */
@SpringBootTest
class AdminLoginAuthenticationTest {

    private static final String CLIENT_PAN = "ABCDE1234F";
    private static final String CLIENT_PASSWORD = "client-secret";

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createClientUser() {
        if (userRepository.findByUsername(CLIENT_PAN).isEmpty()) {
            userService.createUser(CLIENT_PAN, CLIENT_PASSWORD, Role.CLIENT);
        }
    }

    /** Authenticates as if the credentials were posted from the admin login page. */
    private Authentication login(String username, String password) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setParameter(LoginPortal.PARAMETER, "admin");
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        token.setDetails(new PortalAuthenticationDetails(request));
        return authenticationManager.authenticate(token);
    }

    @Test
    void mainFlow_adminWithValidCredentials_isAuthenticatedAsAdmin() {
        Authentication result = login("admin", "admin-secret");

        assertTrue(result.isAuthenticated());
        assertEquals("admin", result.getName());
        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }

    @Test
    void precondition_initialAdminAccountIsSeededFromConfiguration() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void af1_wrongPassword_isRejected() {
        assertThrows(BadCredentialsException.class, () -> login("admin", "wrong"));
    }

    @Test
    void af1_unknownUsername_isRejectedWithSameErrorAsWrongPassword() {
        assertThrows(BadCredentialsException.class, () -> login("no-such-user", "whatever"));
    }

    @Test
    void af2_clientWithValidCredentials_isDeniedAdminAccess() {
        NotAdminException ex = assertThrows(NotAdminException.class, () -> login(CLIENT_PAN, CLIENT_PASSWORD));
        assertEquals("Access denied. Admin privileges required.", ex.getMessage());
    }

    @Test
    void af2_clientWithWrongPassword_getsInvalidCredentialsNotAccessDenied() {
        // Role is only revealed after the password has been verified
        assertThrows(BadCredentialsException.class, () -> login(CLIENT_PAN, "wrong"));
    }

    @Test
    void br03_passwordsAreStoredHashed() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        assertNotEquals("admin-secret", admin.getPassword());
        assertTrue(admin.getPassword().startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches("admin-secret", admin.getPassword()));
    }

    @Test
    void br03_emptyPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> userService.createUser("someone", "", Role.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> userService.createUser("someone", "   ", Role.ADMIN));
        assertTrue(userRepository.findByUsername("someone").isEmpty());
    }
}
