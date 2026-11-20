package com.dijo.security;

import com.vaadin.flow.spring.security.UidlRedirectStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * After logout, sends admins back to the admin login page and everyone else
 * to the client login page.
 */
public class RoleBasedLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    public RoleBasedLogoutSuccessHandler() {
        // Logout is triggered from a Vaadin UI request, where a plain HTTP
        // redirect is ignored; this strategy redirects the browser page.
        setRedirectStrategy(new UidlRedirectStrategy());
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + LoginPortal.ADMIN.role().name()).equals(a.getAuthority()));
        LoginPortal portal = admin ? LoginPortal.ADMIN : LoginPortal.CLIENT;
        return request.getContextPath() + "/" + portal.loginRoute();
    }
}
