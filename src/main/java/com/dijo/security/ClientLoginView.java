package com.dijo.security;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * UC-002: client login page with PAN number and password fields (anonymous
 * access). This is also where unauthenticated users are redirected.
 */
@Route(value = ClientLoginView.ROUTE, autoLayout = false)
@PageTitle("Login | DocuShare")
@AnonymousAllowed
public class ClientLoginView extends AbstractLoginView {

    public static final String ROUTE = "login";

    static final Texts TEXTS = new Texts(
            "Client Login",
            "PAN number",
            "PAN number and password are required.",
            "Invalid PAN number or password.",
            "Check your PAN number and password and try again.");

    public ClientLoginView(AuthenticationContext authenticationContext) {
        super(authenticationContext, LoginPortal.CLIENT, TEXTS);

        Anchor adminLink = new Anchor(AdminLoginView.ROUTE, "Admin login");
        adminLink.addClassName("login-view__switch");
        add(adminLink);
    }
}
