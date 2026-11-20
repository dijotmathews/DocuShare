package com.dijo.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

/**
 * UC-001: admin dashboard, shown after a successful admin login.
 * Only accessible to authenticated users with role ADMIN (BR-01).
 */
@Route(value = AdminDashboardView.ROUTE, autoLayout = false)
@PageTitle("Admin Dashboard | DocuShare")
@RolesAllowed("ADMIN")
public class AdminDashboardView extends Main {

    public static final String ROUTE = "admin";

    public AdminDashboardView(AuthenticationContext authenticationContext) {
        addClassName("dashboard");

        String username = authenticationContext.getPrincipalName().orElse("");

        H2 title = new H2("Admin Dashboard");
        Paragraph welcome = new Paragraph("Welcome, " + username
                + ". From here you manage clients and their tax documents.");
        welcome.addClassName("dashboard__intro");

        // Navigation to every admin section, same order as the header
        Nav navigation = new Nav();
        navigation.addClassName("admin-nav");
        navigation.getElement().setAttribute("aria-label", "Admin sections");
        AdminNavigation.all().forEach(item -> navigation.add(item.dashboardLink()));

        Div content = new Div(title, welcome, navigation);
        content.addClassName("dashboard__content");

        add(new AdminHeader(authenticationContext), content);
    }
}
