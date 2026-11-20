package com.dijo.security;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;

/**
 * Shared layout and error handling for the admin and client login pages.
 */
abstract class AbstractLoginView extends Main implements BeforeEnterObserver {

    /** Texts that differ between the login pages. */
    record Texts(String title, String usernameLabel, String requiredFields,
                 String invalidCredentials, String invalidCredentialsHint) {
    }

    private final AuthenticationContext authenticationContext;
    private final Texts texts;
    private final LoginForm loginForm = new LoginForm();

    protected AbstractLoginView(AuthenticationContext authenticationContext, LoginPortal portal, Texts texts) {
        this.authenticationContext = authenticationContext;
        this.texts = texts;
        addClassName("login-view");

        H1 appName = new H1("DocuShare");
        appName.addClassName("login-view__app-name");
        Paragraph tagline = new Paragraph("Tax document storage and distribution");
        tagline.addClassName("login-view__tagline");

        loginForm.setI18n(createI18n(texts));
        loginForm.setAction(portal.formAction());
        loginForm.setForgotPasswordButtonVisible(false);

        add(appName, tagline, loginForm);
    }

    private static LoginI18n createI18n(Texts texts) {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle(texts.title());
        i18n.getForm().setUsername(texts.usernameLabel());
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Login");
        // AF-3: empty fields are highlighted with this message before submit
        i18n.getErrorMessage().setUsername(texts.requiredFields());
        i18n.getErrorMessage().setPassword(texts.requiredFields());
        i18n.getErrorMessage().setTitle(texts.invalidCredentials());
        i18n.getErrorMessage().setMessage(texts.invalidCredentialsHint());
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            // Already signed in: HomeView forwards to the right dashboard
            event.forwardTo(HomeView.class);
            return;
        }
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            Object exception = consumeAuthenticationException();
            if (exception instanceof NotAdminException || exception instanceof NotClientException) {
                // AF-2: account exists but belongs to the other portal
                loginForm.showErrorMessage(((AuthenticationException) exception).getMessage(),
                        "Use the login page for your account type.");
            } else {
                // AF-1: the form is re-rendered after the failed attempt, so
                // the password field is empty.
                loginForm.setError(true);
            }
        }
    }

    /**
     * Reads and removes the last authentication failure stored by Spring
     * Security, so the error isn't shown again and nothing about the failed
     * attempt is left in the session.
     */
    private static Object consumeAuthenticationException() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null || vaadinSession.getSession() == null) {
            return null;
        }
        WrappedSession session = vaadinSession.getSession();
        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        return exception;
    }

    Texts getTexts() {
        return texts;
    }
}
