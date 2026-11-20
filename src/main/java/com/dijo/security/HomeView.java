package com.dijo.security;

import com.dijo.admin.AdminDashboardView;
import com.dijo.client.ClientDashboardView;
import com.dijo.user.Role;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Root route. Forwards each signed-in user to the dashboard for their role
 * (UC-001 step 7, UC-002 step 8). Anonymous users are redirected to the login
 * page by Spring Security before reaching this view.
 */
@Route(value = "", autoLayout = false)
@PermitAll
public class HomeView extends Div implements BeforeEnterObserver {

    private final AuthenticationContext authenticationContext;

    public HomeView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.hasRole(Role.ADMIN.name())) {
            event.forwardTo(AdminDashboardView.class);
        } else if (authenticationContext.hasRole(Role.CLIENT.name())) {
            event.forwardTo(ClientDashboardView.class);
        } else {
            event.forwardTo(ClientLoginView.class);
        }
    }
}
