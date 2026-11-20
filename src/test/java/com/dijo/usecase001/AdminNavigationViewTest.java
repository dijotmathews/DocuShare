package com.dijo.usecase001;

import com.dijo.admin.AdminDashboardView;
import com.dijo.admin.AllDocumentsView;
import com.dijo.admin.ManageClientsView;
import com.dijo.admin.UploadDocumentView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Admin dashboard navigation: Dashboard, Upload Documents, Clients and
 * Documents, in that order, in the header and on the dashboard.
 */
@SpringBootTest
class AdminNavigationViewTest extends SpringBrowserlessTest {

    private static final List<String> ORDER = List.of("Dashboard", "Upload Documents", "Clients", "Documents");

    @BeforeEach
    void signInAsAdmin() {
        var principal = User.withUsername("admin").password("n/a").roles("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private List<RouterLink> headerLinks() {
        return $(RouterLink.class, $(Header.class).single()).all();
    }

    private List<RouterLink> dashboardLinks() {
        Nav nav = $(Nav.class).all().stream()
                .filter(n -> n.hasClassName("admin-nav"))
                .findFirst().orElseThrow();
        return $(RouterLink.class, nav).all();
    }

    @Test
    void headerListsSectionsWithUploadSecond() {
        navigate(AdminDashboardView.class);

        assertEquals(ORDER, headerLinks().stream().map(RouterLink::getText).toList());
    }

    @Test
    void dashboardListsSectionsInTheSameOrder() {
        navigate(AdminDashboardView.class);

        List<RouterLink> links = dashboardLinks();
        assertEquals(ORDER.size(), links.size());
        for (int i = 0; i < ORDER.size(); i++) {
            assertEquals(ORDER.get(i), $(com.vaadin.flow.component.html.Span.class, links.get(i)).first().getText());
        }
        assertEquals(List.of("admin", UploadDocumentView.ROUTE, ManageClientsView.ROUTE, AllDocumentsView.ROUTE),
                links.stream().map(RouterLink::getHref).toList());
    }

    @Test
    void dashboardLinksOpenEachSection() {
        List<Class<? extends Component>> targets = List.of(AdminDashboardView.class, UploadDocumentView.class,
                ManageClientsView.class, AllDocumentsView.class);
        for (int i = 0; i < targets.size(); i++) {
            navigate(AdminDashboardView.class);
            test(dashboardLinks().get(i)).click();
            assertInstanceOf(targets.get(i), getCurrentView());
        }
    }

    @Test
    void headerIsTheSameOnEveryAdminPage() {
        for (Class<? extends Component> page : List.of(UploadDocumentView.class, ManageClientsView.class,
                AllDocumentsView.class)) {
            navigate(page);
            assertEquals(ORDER, headerLinks().stream().map(RouterLink::getText).toList(), page.getSimpleName());
        }
    }
}
