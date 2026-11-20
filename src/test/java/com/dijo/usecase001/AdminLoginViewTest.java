package com.dijo.usecase001;

import com.dijo.admin.AdminDashboardView;
import com.dijo.security.AdminLoginView;
import com.dijo.security.ClientLoginView;
import com.dijo.security.NotAdminException;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.WebAttributes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-001 Admin Login: login page and admin dashboard UI
 * (Main Flow steps 1, 7, AF-1, AF-2, AF-3, BR-01, BR-02).
 */
@SpringBootTest
class AdminLoginViewTest extends SpringBrowserlessTest {

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

    private static String i18nJson(LoginForm form) {
        return String.valueOf(form.getElement().getPropertyRaw("i18n"));
    }

    @Test
    void mainFlowStep1_loginPageShowsUsernamePasswordForm() {
        navigate(AdminLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertFalse(form.isError());
        assertTrue(form.getAction().endsWith("login?portal=admin"), form.getAction());
        String i18n = i18nJson(form);
        assertTrue(i18n.contains("Admin Login"), i18n);
        assertTrue(i18n.contains("Username"), i18n);
        assertTrue(i18n.contains("Password"), i18n);
        assertTrue(i18n.contains("Login"), i18n);
    }

    @Test
    void af3_emptyFieldsShowRequiredMessage() {
        navigate(AdminLoginView.class);

        String i18n = i18nJson($(LoginForm.class).single());
        assertTrue(i18n.contains("Username and password are required."), i18n);
    }

    @Test
    void af1_failedLoginShowsInvalidCredentialsError() {
        VaadinSession.getCurrent().getSession().setAttribute(
                WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Bad credentials"));

        navigate("admin/login?error", AdminLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertTrue(form.isError());
        assertTrue(i18nJson(form).contains("Invalid username or password."));
        assertNull(VaadinSession.getCurrent().getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION));
    }

    @Test
    void af2_nonAdminLoginShowsAccessDeniedAndClearsSessionState() {
        VaadinSession.getCurrent().getSession().setAttribute(
                WebAttributes.AUTHENTICATION_EXCEPTION, new NotAdminException());

        navigate("admin/login?error", AdminLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertTrue(form.isError());
        assertTrue(i18nJson(form).contains("Access denied. Admin privileges required."));
        assertNull(VaadinSession.getCurrent().getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void mainFlowStep7_authenticatedAdminSeesDashboard() {
        signInAs("admin", "ADMIN");

        navigate(AdminDashboardView.class);

        assertInstanceOf(AdminDashboardView.class, getCurrentView());
        assertTrue($(Span.class).all().stream().anyMatch(s -> s.getText().equals("Signed in as admin")));
    }

    @Test
    void mainFlow_authenticatedAdminOnLoginPageIsSentToDashboard() {
        signInAs("admin", "ADMIN");

        navigate("admin/login", AdminDashboardView.class);

        assertInstanceOf(AdminDashboardView.class, getCurrentView());
    }

    @Test
    void br01_br02_anonymousUserCannotOpenDashboard() {
        navigate("admin", ClientLoginView.class);

        assertInstanceOf(ClientLoginView.class, getCurrentView());
    }

    @Test
    void br01_clientUserCannotOpenDashboard() {
        signInAs("ABCDE1234F", "CLIENT");

        try {
            navigate("admin", AdminDashboardView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof AdminDashboardView);
    }
}
