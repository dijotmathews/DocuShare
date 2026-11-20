package com.dijo.security;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * UC-001: admin login page with username and password fields (anonymous access).
 */
@Route(value = AdminLoginView.ROUTE, autoLayout = false)
@PageTitle("Admin Login | DocuShare")
@AnonymousAllowed
public class AdminLoginView extends AbstractLoginView {

    public static final String ROUTE = "admin/login";

    static final Texts TEXTS = new Texts(
            "Admin Login",
            "Username",
            "Username and password are required.",
            "Invalid username or password.",
            "Check your username and password and try again.");

    public AdminLoginView(AuthenticationContext authenticationContext) {
        super(authenticationContext, LoginPortal.ADMIN, TEXTS);
    }
}
