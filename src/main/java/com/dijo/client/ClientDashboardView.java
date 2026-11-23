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
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * UC-004: the client's "My Documents" page, shown after login (UC-002). The
 * documents uploaded for the signed-in client's PAN are grouped (Filings,
 * Financials, Computations, Misc); a side nav picks the group, which has its
 * own URL (/documents/financials), and the group's documents are shown by
 * financial year, latest first. Only accessible to users with role CLIENT.
 */
@Route(value = ClientDashboardView.ROUTE + "/:" + ClientDashboardView.GROUP_PARAMETER + "?", autoLayout = false)
@PageTitle("My Documents | DocuShare")
@RolesAllowed("CLIENT")
public class ClientDashboardView extends Main implements BeforeEnterObserver {

    public static final String ROUTE = "documents";
    static final String GROUP_PARAMETER = "group";
    static final String EMPTY_MESSAGE = "No documents available yet. Check back soon.";

    /** File types a browser can open and print itself; other files get no Print link. */
    private static final Set<String> PRINTABLE_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp", "txt");

    /**
     * Print link click: fetches the file, shows it in a hidden frame and opens
     * the browser's print dialog for it. The frame loads a blob URL, as the
     * app's pages may not be framed (X-Frame-Options: DENY). Where printing
     * from a frame is unreliable (Safari, touch devices) or a modifier key is
     * held, the link's default applies instead: the file opens in a new tab,
     * to print from there. A failed fetch is reported by the server (the same
     * notification as for Download).
     */
    private static final String PRINT_ON_CLICK = """
            const link = this;
            link.addEventListener('click', async e => {
              const ua = navigator.userAgent;
              const safari = /safari/i.test(ua) && !/chrome|chromium|crios|android/i.test(ua);
              if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey
                  || safari || matchMedia('(pointer: coarse)').matches) {
                return;
              }
              e.preventDefault();
              let response;
              try {
                response = await fetch(link.href, { credentials: 'same-origin' });
              } catch (err) {
                return;
              }
              if (!response.ok) {
                return;
              }
              const url = URL.createObjectURL(await response.blob());
              document.querySelectorAll('iframe.my-documents__print-frame').forEach(f => {
                URL.revokeObjectURL(f.src);
                f.remove();
              });
              const frame = document.createElement('iframe');
              frame.className = 'my-documents__print-frame';
              frame.title = 'Document to print';
              frame.tabIndex = -1;
              frame.setAttribute('aria-hidden', 'true');
              frame.onload = () => {
                try {
                  frame.contentWindow.focus();
                  frame.contentWindow.print();
                } catch (err) {
                  window.open(url, '_blank', 'noopener');
                }
              };
              frame.src = url;
              document.body.appendChild(frame);
            });
            """;

    private static final DateTimeFormatter UPLOAD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final ClientDocumentService clientDocumentService;
    private final DateTimeFormatter uploadDate;

    private final Paragraph intro = new Paragraph();
    /** Everything below the intro; rebuilt on every visit, including when only the group changes. */
    private final Div body = new Div();
    private MyDocuments mine;

    public ClientDashboardView(AuthenticationContext authenticationContext,
                               ClientDocumentService clientDocumentService, Clock clock) {
        this.clientDocumentService = clientDocumentService;
        this.uploadDate = UPLOAD_DATE.withZone(clock.getZone());
        addClassName("dashboard");

        H2 title = new H2("My Documents");
        intro.addClassName("dashboard__intro");
        body.addClassName("my-documents__body");

        Div content = new Div(title, intro, body);
        content.addClassNames("dashboard__content", "my-documents");

        add(new AppHeader(authenticationContext), content);
    }

    /** Shows the group named in the URL; without one, the first group that has documents. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        mine = clientDocumentService.findMyDocuments();
        String who = mine.clientName() == null ? "PAN " + mine.panNumber()
                : mine.clientName() + " (PAN " + mine.panNumber() + ")";
        intro.setText("Tax documents uploaded by your consultant for " + who + ".");
        body.removeAll();

        if (mine.documents().isEmpty()) {
            body.add(emptyState());
            return;
        }
        Optional<DocumentGroup> requested = event.getRouteParameters().get(GROUP_PARAMETER)
                .flatMap(DocumentGroup::fromSlug);
        if (requested.isEmpty()) {
            DocumentGroup first = Arrays.stream(DocumentGroup.values())
                    .filter(g -> mine.count(g) > 0).findFirst().orElseThrow();
            event.forwardTo(ClientDashboardView.class, new RouteParameters(GROUP_PARAMETER, first.slug()));
            return;
        }
        DocumentGroup group = requested.get();
        Div layout = new Div(groupNav(), groupContent(group, mine.byFinancialYear(group)));
        layout.addClassName("my-documents__layout");
        body.add(layout);
    }

    /** Step 2: side nav with one item per group and the number of documents in it. */
    private Component groupNav() {
        SideNav nav = new SideNav();
        nav.addClassName("my-documents__nav");
        nav.setLabel("Document groups");
        for (DocumentGroup group : DocumentGroup.values()) {
            int count = mine.count(group);
            SideNavItem item = new SideNavItem(group.label(),
                    RouteConfiguration.forSessionScope().getUrl(ClientDashboardView.class,
                            new RouteParameters(GROUP_PARAMETER, group.slug())));
            Span badge = new Span(String.valueOf(count));
            badge.addClassName("my-documents__nav-count");
            badge.getElement().setAttribute("aria-label", count == 1 ? "1 document" : count + " documents");
            item.setSuffixComponent(badge);
            nav.addItem(item);
        }
        return nav;
    }

    /** Step 3: a group's documents, one section per financial year, latest first. */
    private Component groupContent(DocumentGroup group, List<FinancialYearDocuments> years) {
        Section groupSection = new Section();
        groupSection.addClassName("my-documents__group");
        H3 groupTitle = new H3(group.label());
        groupTitle.addClassName("my-documents__group-title");
        groupSection.add(groupTitle);
        if (years.isEmpty()) {
            Paragraph empty = new Paragraph(group.emptyMessage());
            empty.addClassName("my-documents__group-empty");
            groupSection.add(empty);
            return groupSection;
        }
        for (FinancialYearDocuments year : years) {
            H4 heading = new H4("FY " + yearRange(year.financialYear()));
            heading.addClassName("my-documents__year");
            Div list = new Div();
            list.addClassName("my-documents__list");
            list.getElement().setAttribute("role", "list");
            year.documents().forEach(d -> list.add(documentRow(d)));
            Div yearSection = new Div(heading, list);
            yearSection.addClassName("my-documents__year-section");
            groupSection.add(yearSection);
        }
        return groupSection;
    }

    /** One document: type, assessment year and upload date, and its Download and Print links (UC-005). */
    private Component documentRow(DocumentSummary document) {
        Span type = new Span(document.documentType().label());
        type.addClassName("my-documents__item-type");
        Span meta = new Span("AY " + yearRange(document.assessmentYear()) + " · Uploaded "
                + uploadDate.format(document.uploadedAt()));
        meta.addClassName("my-documents__item-meta");
        Div text = new Div(type, meta);
        text.addClassName("my-documents__item");
        Div actions = new Div(downloadLink(document));
        actions.addClassName("my-documents__actions");
        if (isPrintable(document)) {
            actions.add(printLink(document));
        }
        Div row = new Div(text, actions);
        row.addClassName("my-documents__row");
        row.getElement().setAttribute("role", "listitem");
        return row;
    }

    /** 2024 → "2024-25": the year a financial or assessment year starts in, as it's usually written. */
    static String yearRange(int startYear) {
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }

    /** UC-005 step 1: link that saves the file; failures are reported in a notification. */
    private Anchor downloadLink(DocumentSummary document) {
        Anchor link = new Anchor(fileHandler(document, false), "");
        link.addClassName("my-documents__download");
        Span text = new Span("Download");
        text.addClassName("my-documents__download-text");
        link.add(VaadinIcon.DOWNLOAD_ALT.create(), text);
        link.getElement().setAttribute("aria-label", "Download " + document.documentType().label()
                + ", assessment year " + document.assessmentYear());
        return link;
    }

    /**
     * UC-005 AF-4: link that opens the browser's print dialog for the file. It
     * fetches the file the same way as Download (same access check and audit
     * entry), but for display rather than saving.
     */
    private Anchor printLink(DocumentSummary document) {
        Anchor link = new Anchor(fileHandler(document, true), AttachmentType.INLINE, "");
        link.setTarget(AnchorTarget.BLANK);
        link.addClassName("my-documents__print");
        Span text = new Span("Print");
        text.addClassName("my-documents__download-text");
        link.add(VaadinIcon.PRINT.create(), text);
        link.getElement().setAttribute("aria-label", "Print " + document.documentType().label()
                + ", assessment year " + document.assessmentYear());
        link.getElement().executeJs(PRINT_ON_CLICK);
        return link;
    }

    static boolean isPrintable(DocumentSummary document) {
        String name = document.fileName() == null ? "" : document.fileName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && PRINTABLE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** Streams the file as an attachment (Download) or inline (Print). */
    private DownloadHandler fileHandler(DocumentSummary document, boolean inline) {
        return event -> {
            try {
                clientDocumentService.download(document.id(), (fileName, contentType, size) -> {
                    disposition(event, fileName, inline);
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
    }

    private static void disposition(DownloadEvent event, String fileName, boolean inline) {
        if (inline) {
            event.inline(fileName);
        } else {
            event.setFileName(fileName);
        }
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
