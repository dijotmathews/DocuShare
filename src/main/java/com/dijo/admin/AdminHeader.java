package com.dijo.admin;

import com.dijo.ui.AppHeader;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Header for admin pages, with links to the admin sections ({@link AdminNavigation}).
 */
class AdminHeader extends AppHeader {

    AdminHeader(AuthenticationContext authenticationContext) {
        super(authenticationContext, AdminNavigation.all().stream()
                .map(AdminNavigation::headerLink)
                .toArray(RouterLink[]::new));
    }
}
