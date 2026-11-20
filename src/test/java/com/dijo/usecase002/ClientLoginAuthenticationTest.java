package com.dijo.usecase002;

import com.dijo.security.LoginPortal;
import com.dijo.security.NotClientException;
import com.dijo.security.PortalAuthenticationDetails;
import com.dijo.user.Pan;
import com.dijo.user.Role;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-002 Client Login: credential validation, role check and PAN format
 * (Main Flow steps 5–7, AF-1, AF-2, BR-01, BR-02).
 */
@SpringBootTest
class ClientLoginAuthenticationTest {

    private static final String CLIENT_PAN = "PQRST6789K";
    private static final String CLIENT_PASSWORD = "client-secret";

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @BeforeEach
    void createClientUser() {
        if (userRepository.findByUsername(CLIENT_PAN).isEmpty()) {
            userService.createUser(CLIENT_PAN, CLIENT_PASSWORD, Role.CLIENT);
        }
    }

    /** Authenticates as if the credentials were posted from the client login page. */
    private Authentication login(String pan, String password) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setParameter(LoginPortal.PARAMETER, "client");
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(pan, password);
        token.setDetails(new PortalAuthenticationDetails(request));
        return authenticationManager.authenticate(token);
    }

    @Test
    void mainFlow_clientWithValidPanAndPassword_isAuthenticatedAsClient() {
        Authentication result = login(CLIENT_PAN, CLIENT_PASSWORD);

        assertTrue(result.isAuthenticated());
        assertEquals(CLIENT_PAN, result.getName());
        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CLIENT"::equals));
    }

    @Test
    void mainFlow_panIsCaseInsensitiveAndTrimmed() {
        Authentication result = login("  pqrst6789k ", CLIENT_PASSWORD);

        assertEquals(CLIENT_PAN, result.getName());
    }

    @Test
    void af1_wrongPassword_isRejected() {
        assertThrows(BadCredentialsException.class, () -> login(CLIENT_PAN, "wrong"));
    }

    @Test
    void af1_unknownPan_isRejected() {
        assertThrows(BadCredentialsException.class, () -> login("ZZZZZ9999Z", CLIENT_PASSWORD));
    }

    @Test
    void af2_adminWithValidCredentials_isDeniedOnClientLogin() {
        NotClientException ex = assertThrows(NotClientException.class, () -> login("admin", "admin-secret"));
        assertEquals("Access denied. This account is for admin use only.", ex.getMessage());
    }

    @Test
    void af2_requestWithoutPortal_isTreatedAsClientLogin() {
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated("admin", "admin-secret");
        token.setDetails(new PortalAuthenticationDetails(new MockHttpServletRequest("POST", "/login")));

        assertThrows(NotClientException.class, () -> authenticationManager.authenticate(token));
    }

    @Test
    void br02_malformedPan_isRejectedEvenWithCorrectPassword() {
        userService.createUser("notapan", CLIENT_PASSWORD, Role.CLIENT);
        try {
            assertThrows(BadCredentialsException.class, () -> login("notapan", CLIENT_PASSWORD));
        } finally {
            userRepository.findByUsername("NOTAPAN").ifPresent(userRepository::delete);
            userRepository.findByUsername("notapan").ifPresent(userRepository::delete);
        }
    }

    @Test
    void br02_panFormat() {
        assertTrue(Pan.isValid("ABCDE1234F"));
        assertFalse(Pan.isValid("ABCD1234F"));
        assertFalse(Pan.isValid("ABCDE12345"));
        assertFalse(Pan.isValid("abcde1234f"));
        assertFalse(Pan.isValid(""));
        assertFalse(Pan.isValid(null));
        assertEquals("ABCDE1234F", Pan.normalize(" abcde1234f "));
    }
}
