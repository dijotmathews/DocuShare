package com.dijo.security;

import com.dijo.user.Pan;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Validates credentials against the database (UC-001 step 4, UC-002 step 5)
 * and then verifies the user has the role belonging to the login portal used
 * (UC-001 step 5, UC-002 step 6). A user with valid credentials but the wrong
 * role is rejected, so no authenticated session is created for them
 * (UC-001 AF-2, UC-002 AF-2).
 */
public class PortalAuthenticationProvider implements AuthenticationProvider {

    private final DaoAuthenticationProvider delegate;

    public PortalAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.delegate = new DaoAuthenticationProvider(userDetailsService);
        this.delegate.setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        LoginPortal portal = authentication.getDetails() instanceof PortalAuthenticationDetails details
                ? details.getPortal()
                : LoginPortal.CLIENT;

        Authentication attempt = authentication;
        if (portal == LoginPortal.CLIENT) {
            // PANs are matched case-insensitively ("abcde1234f" -> "ABCDE1234F")
            String pan = Pan.normalize(authentication.getName());
            if (Pan.isValid(pan)) {
                UsernamePasswordAuthenticationToken normalized =
                        UsernamePasswordAuthenticationToken.unauthenticated(pan, authentication.getCredentials());
                normalized.setDetails(authentication.getDetails());
                attempt = normalized;
            }
        }

        // Credentials are verified first, so the role (and AF-2 message) is
        // only revealed to someone who knows the password.
        Authentication result = delegate.authenticate(attempt);

        String requiredAuthority = "ROLE_" + portal.role().name();
        boolean hasRole = result.getAuthorities().stream()
                .anyMatch(a -> requiredAuthority.equals(a.getAuthority()));
        if (!hasRole) {
            throw portal == LoginPortal.ADMIN ? new NotAdminException() : new NotClientException();
        }
        // UC-002 BR-02: a client's username must be a valid PAN number
        if (portal == LoginPortal.CLIENT && !Pan.isValid(result.getName())) {
            throw new BadCredentialsException("Bad credentials");
        }
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
