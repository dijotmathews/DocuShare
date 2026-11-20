package com.dijo.usecase002;

import com.dijo.admin.AdminDashboardView;
import com.dijo.client.ClientDashboardView;
import com.dijo.security.ClientLoginView;
import com.dijo.security.NotClientException;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.html.Paragraph;
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
 * UC-002 Client Login: client login page and client dashboard UI
 * (Main Flow steps 1, 8, AF-1, AF-2, AF-3, BR-01, BR-03, BR-04).
 */
@SpringBootTest
class ClientLoginViewTest extends SpringBrowserlessTest {

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
    void mainFlowStep1_loginPageShowsPanAndPasswordForm() {
        navigate(ClientLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertFalse(form.isError());
        assertTrue(form.getAction().endsWith("login?portal=client"), form.getAction());
        String i18n = i18nJson(form);
        assertTrue(i18n.contains("Client Login"), i18n);
        assertTrue(i18n.contains("PAN number"), i18n);
        assertTrue(i18n.contains("Password"), i18n);
    }

    @Test
    void af3_emptyFieldsShowRequiredMessage() {
        navigate(ClientLoginView.class);

        assertTrue(i18nJson($(LoginForm.class).single()).contains("PAN number and password are required."));
    }

    @Test
    void af1_failedLoginShowsInvalidPanOrPasswordError() {
        VaadinSession.getCurrent().getSession().setAttribute(
                WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Bad credentials"));

        navigate("login?error", ClientLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertTrue(form.isError());
        assertTrue(i18nJson(form).contains("Invalid PAN number or password."));
        assertNull(VaadinSession.getCurrent().getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION));
    }

    @Test
    void af2_adminOnClientLoginShowsAccessDeniedAndClearsSessionState() {
        VaadinSession.getCurrent().getSession().setAttribute(
                WebAttributes.AUTHENTICATION_EXCEPTION, new NotClientException());

        navigate("login?error", ClientLoginView.class);

        LoginForm form = $(LoginForm.class).single();
        assertTrue(form.isError());
        assertTrue(i18nJson(form).contains("Access denied. This account is for admin use only."));
        assertNull(VaadinSession.getCurrent().getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void mainFlowStep8_clientAfterLoginIsForwardedToDocumentDashboard() {
        signInAs("ABCDE1234F", "CLIENT");

        // After a successful login Spring Security redirects to "/"
        navigate("", ClientDashboardView.class);

        assertInstanceOf(ClientDashboardView.class, getCurrentView());
        assertTrue($(Span.class).all().stream().anyMatch(s -> s.getText().equals("Signed in as ABCDE1234F")));
    }

    @Test
    void br03_dashboardIsScopedToTheSignedInClientsPan() {
        signInAs("ABCDE1234F", "CLIENT");

        navigate(ClientDashboardView.class);

        assertTrue($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("ABCDE1234F")));
        assertFalse($(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("PQRST6789K")));
    }

    @Test
    void mainFlow_signedInClientOnLoginPageIsSentToDashboard() {
        signInAs("ABCDE1234F", "CLIENT");

        navigate("login", ClientDashboardView.class);

        assertInstanceOf(ClientDashboardView.class, getCurrentView());
    }

    @Test
    void br01_br04_anonymousUserCannotOpenClientDashboard() {
        navigate("documents", ClientLoginView.class);

        assertInstanceOf(ClientLoginView.class, getCurrentView());
    }

    @Test
    void br01_adminCannotOpenClientDashboard() {
        signInAs("admin", "ADMIN");

        try {
            navigate("documents", ClientDashboardView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof ClientDashboardView);
    }

    @Test
    void br01_clientCannotOpenAdminDashboard() {
        signInAs("ABCDE1234F", "CLIENT");

        try {
            navigate("admin", AdminDashboardView.class);
        } catch (Exception | AssertionError expected) {
            // navigation is blocked; asserted below
        }

        assertFalse(getCurrentView() instanceof AdminDashboardView);
    }
}
