package com.dijo.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;

import java.util.List;

/**
 * The admin sections, in the order they appear in the header and on the
 * dashboard. One list so both stay in step.
 */
enum AdminNavigation {

    DASHBOARD("Dashboard", "Overview and shortcuts", VaadinIcon.DASHBOARD, AdminDashboardView.class),
    UPLOAD_DOCUMENTS("Upload Documents", "Upload a tax document for a client", VaadinIcon.UPLOAD,
            UploadDocumentView.class),
    CLIENTS("Clients", "Add and edit clients, reset passwords", VaadinIcon.USERS, ManageClientsView.class),
    DOCUMENTS("Documents", "Search every client's documents", VaadinIcon.FILE_TEXT, AllDocumentsView.class);

    private final String label;
    private final String description;
    private final VaadinIcon icon;
    private final Class<? extends Component> target;

    AdminNavigation(String label, String description, VaadinIcon icon, Class<? extends Component> target) {
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.target = target;
    }

    String label() {
        return label;
    }

    String description() {
        return description;
    }

    VaadinIcon icon() {
        return icon;
    }

    Class<? extends Component> target() {
        return target;
    }

    /** Header link: highlighted only on its own page, so "Dashboard" (admin) isn't lit on every admin page. */
    RouterLink headerLink() {
        RouterLink link = new RouterLink(label, target);
        link.setHighlightCondition(HighlightConditions.sameLocation());
        return link;
    }

    /** Dashboard entry: icon, label and a one-line description. */
    RouterLink dashboardLink() {
        Span title = new Span(label);
        title.addClassName("admin-nav__label");
        Span text = new Span(description);
        text.addClassName("admin-nav__description");
        RouterLink link = new RouterLink(target);
        link.add(icon.create(), title, text);
        link.addClassName("admin-nav__item");
        link.setHighlightCondition(HighlightConditions.sameLocation());
        return link;
    }

    static List<AdminNavigation> all() {
        return List.of(values());
    }
}
