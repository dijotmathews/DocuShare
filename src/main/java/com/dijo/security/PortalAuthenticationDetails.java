package com.dijo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Authentication details that also record which login portal was used.
 */
public class PortalAuthenticationDetails extends WebAuthenticationDetails {

    private final LoginPortal portal;

    public PortalAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.portal = LoginPortal.fromParameter(request.getParameter(LoginPortal.PARAMETER));
    }

    public LoginPortal getPortal() {
        return portal;
    }
}
