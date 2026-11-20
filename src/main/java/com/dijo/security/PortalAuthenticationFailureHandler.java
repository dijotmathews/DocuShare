package com.dijo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sends a failed login back to the login page it came from, with
 * {@code ?error}. The exception is stored in the session so the page can show
 * the matching message.
 */
public class PortalAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final Map<LoginPortal, AuthenticationFailureHandler> handlers = new EnumMap<>(LoginPortal.class);

    public PortalAuthenticationFailureHandler() {
        for (LoginPortal portal : LoginPortal.values()) {
            handlers.put(portal, new SimpleUrlAuthenticationFailureHandler("/" + portal.loginRoute() + "?error"));
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        LoginPortal portal = LoginPortal.fromParameter(request.getParameter(LoginPortal.PARAMETER));
        handlers.get(portal).onAuthenticationFailure(request, response, exception);
    }
}
