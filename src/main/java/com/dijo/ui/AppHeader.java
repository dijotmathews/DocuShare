package com.dijo.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Top bar for signed-in pages: app name, optional navigation links, the
 * signed-in user, and Logout.
 */
public class AppHeader extends Header {

    public AppHeader(AuthenticationContext authenticationContext, RouterLink... navLinks) {
        addClassName("app-header");

        H1 appName = new H1("DocuShare");
        appName.addClassName("app-header__app-name");

        Div brandArea = new Div(appName);
        brandArea.addClassName("app-header__brand-area");
        if (navLinks.length > 0) {
            Nav nav = new Nav(navLinks);
            nav.addClassName("app-header__nav");
            brandArea.add(nav);
        }

        Span signedInAs = new Span("Signed in as " + authenticationContext.getPrincipalName().orElse(""));
        signedInAs.addClassName("app-header__user");

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> authenticationContext.logout());
        logout.addThemeVariants(ButtonVariant.TERTIARY);

        Div userArea = new Div(signedInAs, logout);
        userArea.addClassName("app-header__user-area");

        add(brandArea, userArea);
    }
}
