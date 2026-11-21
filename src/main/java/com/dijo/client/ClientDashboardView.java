package com.dijo.client;

import com.dijo.document.ClientDocumentService;
import com.dijo.document.ClientDocumentService.DocumentSummary;
import com.dijo.document.ClientDocumentService.FinancialYearDocuments;
import com.dijo.document.ClientDocumentService.MyDocuments;
import com.dijo.document.DocumentDownloadException;
import com.dijo.document.DocumentGroup;
import com.dijo.ui.AppHeader;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * UC-004: the client's "My Documents" page, shown after login (UC-002). The
 * documents uploaded for the signed-in client's PAN are shown in tabs per
 * group (Filings, Financials, Computations, Misc), and inside each tab by
 * financial year, latest first. Only accessible to users with role CLIENT.
 */
@Route(value = ClientDashboardView.ROUTE, autoLayout = false)
@PageTitle("My Documents | DocuShare")
@RolesAllowed("CLIENT")
public class ClientDashboardView extends Main {

    public static final String ROUTE = "documents";
    static final String EMPTY_MESSAGE = "No documents available yet. Check back soon.";

    private static final DateTimeFormatter UPLOAD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final ClientDocumentService clientDocumentService;
    private final DateTimeFormatter uploadDate;

    public ClientDashboardView(AuthenticationContext authenticationContext,
                               ClientDocumentService clientDocumentService, Clock clock) {
        this.clientDocumentService = clientDocumentService;
        this.uploadDate = UPLOAD_DATE.withZone(clock.getZone());
        addClassName("dashboard");

        MyDocuments mine = clientDocumentService.findMyDocuments();

        H2 title = new H2("My Documents");
        String who = mine.clientName() == null ? "PAN " + mine.panNumber()
                : mine.clientName() + " (PAN " + mine.panNumber() + ")";
        Paragraph intro = new Paragraph("Tax documents uploaded by your consultant for " + who + ".");
        intro.addClassName("dashboard__intro");

        Div content = new Div(title, intro);
        content.addClassNames("dashboard__content", "my-documents");
        content.add(mine.documents().isEmpty() ? emptyState() : groupTabs(mine));

        add(new AppHeader(authenticationContext), content);
    }

    /** Step 2: one tab per group, with the number of documents in it. */
    private TabSheet groupTabs(MyDocuments mine) {
        TabSheet tabs = new TabSheet();
        tabs.addClassName("my-documents__tabs");
        Tab firstWithDocuments = null;
        for (DocumentGroup group : DocumentGroup.values()) {
            int count = mine.count(group);
            Span badge = new Span(String.valueOf(count));
            badge.addClassName("my-documents__tab-count");
            badge.getElement().setAttribute("aria-label", count == 1 ? "1 document" : count + " documents");
            Tab tab = new Tab(new Span(group.label()), badge);
            tab.setId("tab-" + group.name().toLowerCase(Locale.ROOT));
            tabs.add(tab, groupContent(group, mine.byFinancialYear(group)));
            if (firstWithDocuments == null && count > 0) {
                firstWithDocuments = tab;
            }
        }
        // Open on the first group that has something to show
        tabs.setSelectedTab(firstWithDocuments);
        return tabs;
    }

    /** Step 3: a group's documents, one section per financial year, latest first. */
    private Component groupContent(DocumentGroup group, List<FinancialYearDocuments> years) {
        Div content = new Div();
        content.addClassName("my-documents__group");
        if (years.isEmpty()) {
            Paragraph empty = new Paragraph(group.emptyMessage());
            empty.addClassName("my-documents__group-empty");
            content.add(empty);
            return content;
        }
        for (FinancialYearDocuments year : years) {
            H3 heading = new H3("FY " + yearRange(year.financialYear()));
            heading.addClassName("my-documents__year");
            Div list = new Div();
            list.addClassName("my-documents__list");
            list.getElement().setAttribute("role", "list");
            year.documents().forEach(d -> list.add(documentRow(d)));
            Section section = new Section(heading, list);
            section.addClassName("my-documents__year-section");
            content.add(section);
        }
        return content;
    }

    /** One document: type, assessment year and upload date, and its Download link (UC-005). */
    private Component documentRow(DocumentSummary document) {
        Span type = new Span(document.documentType().label());
        type.addClassName("my-documents__item-type");
        Span meta = new Span("AY " + yearRange(document.assessmentYear()) + " · Uploaded "
                + uploadDate.format(document.uploadedAt()));
        meta.addClassName("my-documents__item-meta");
        Div text = new Div(type, meta);
        text.addClassName("my-documents__item");
        Div row = new Div(text, downloadLink(document));
        row.addClassName("my-documents__row");
        row.getElement().setAttribute("role", "listitem");
        return row;
    }

    /** 2024 → "2024-25": the year a financial or assessment year starts in, as it's usually written. */
    static String yearRange(int startYear) {
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }

    /** UC-005 step 1: link that streams the file; failures are reported in a notification. */
    private Anchor downloadLink(DocumentSummary document) {
        DownloadHandler handler = event -> {
            try {
                clientDocumentService.download(document.id(), (fileName, contentType, size) -> {
                    event.setFileName(fileName);
                    event.setContentType(contentType);
                    event.setContentLength(size);
                    return event.getOutputStream();
                });
            } catch (DocumentDownloadException ex) {
                event.getResponse().setStatus(ex.httpStatus());
                // The download request isn't a UI round trip; push the message to the page
                event.getUI().access(() -> showDownloadError(ex.getMessage()));
            }
        };
        Anchor link = new Anchor(handler, "");
        link.addClassName("my-documents__download");
        Span text = new Span("Download");
        text.addClassName("my-documents__download-text");
        link.add(VaadinIcon.DOWNLOAD_ALT.create(), text);
        link.getElement().setAttribute("aria-label", "Download " + document.documentType().label()
                + ", assessment year " + document.assessmentYear());
        return link;
    }

    static void showDownloadError(String message) {
        Notification notification = Notification.show(message, 6000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.ERROR);
    }

    /** AF-1 / AF-2: nothing uploaded yet (e.g. the client's first login). */
    private static Div emptyState() {
        var icon = VaadinIcon.FOLDER_OPEN_O.create();
        icon.addClassName("my-documents__empty-icon");
        H3 message = new H3(EMPTY_MESSAGE);
        Paragraph explanation = new Paragraph("When your consultant uploads documents for you, such as IT "
                + "acknowledgements, balance sheets or tax computations, they will be listed here. If you are "
                + "expecting a document, please contact your consultant.");
        Div empty = new Div(icon, message, explanation);
        empty.addClassName("my-documents__empty");
        return empty;
    }
}
