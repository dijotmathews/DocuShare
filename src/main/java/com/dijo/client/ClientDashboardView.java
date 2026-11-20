package com.dijo.client;

import com.dijo.document.ClientDocumentService;
import com.dijo.document.ClientDocumentService.DocumentSummary;
import com.dijo.document.ClientDocumentService.MyDocuments;
import com.dijo.document.DocumentDownloadException;
import com.dijo.ui.AppHeader;
import com.dijo.ui.CompactGrid;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UC-004: the client's "My Documents" page, shown after login (UC-002). Lists
 * the documents uploaded for the signed-in client's PAN, most recent first.
 * Only accessible to users with role CLIENT.
 */
@Route(value = ClientDashboardView.ROUTE, autoLayout = false)
@PageTitle("My Documents | DocuShare")
@RolesAllowed("CLIENT")
public class ClientDashboardView extends Main {

    public static final String ROUTE = "documents";
    static final String EMPTY_MESSAGE = "No documents available yet. Check back soon.";

    private static final DateTimeFormatter UPLOAD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final ClientDocumentService clientDocumentService;

    public ClientDashboardView(AuthenticationContext authenticationContext,
                               ClientDocumentService clientDocumentService, Clock clock) {
        this.clientDocumentService = clientDocumentService;
        addClassName("dashboard");

        MyDocuments mine = clientDocumentService.findMyDocuments();

        H2 title = new H2("My Documents");
        String who = mine.clientName() == null ? "PAN " + mine.panNumber()
                : mine.clientName() + " (PAN " + mine.panNumber() + ")";
        Paragraph intro = new Paragraph("Tax documents uploaded by your consultant for " + who + ".");
        intro.addClassName("dashboard__intro");

        Div content = new Div(title, intro);
        content.addClassNames("dashboard__content", "my-documents");
        content.add(mine.documents().isEmpty() ? emptyState() : documentGrid(mine.documents(), clock));

        add(new AppHeader(authenticationContext), content);
    }

    private Grid<DocumentSummary> documentGrid(List<DocumentSummary> documents, Clock clock) {
        DateTimeFormatter uploadDate = UPLOAD_DATE.withZone(clock.getZone());
        Grid<DocumentSummary> grid = new Grid<>();
        grid.addClassName("my-documents__grid");
        grid.addThemeVariants(GridVariant.ROW_STRIPES);
        grid.setAllRowsVisible(true);
        List<Grid.Column<DocumentSummary>> wideColumns = new ArrayList<>();

        wideColumns.add(grid.addColumn(d -> d.documentType().label()).setHeader("Document Type")
                .setComparator(Comparator.comparing(d -> d.documentType().label()))
                .setSortable(true).setAutoWidth(true).setFlexGrow(1));
        wideColumns.add(grid.addColumn(DocumentSummary::assessmentYear).setHeader("Assessment Year")
                .setComparator(DocumentSummary::assessmentYear).setSortable(true).setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END));
        wideColumns.add(grid.addColumn(DocumentSummary::financialYear).setHeader("Financial Year")
                .setComparator(DocumentSummary::financialYear).setSortable(true).setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END));
        Grid.Column<DocumentSummary> uploaded = grid.addColumn(d -> uploadDate.format(d.uploadedAt()))
                .setHeader("Upload Date")
                .setComparator(Comparator.comparing(DocumentSummary::uploadedAt)
                        .thenComparing(DocumentSummary::id))
                .setSortable(true).setAutoWidth(true);
        wideColumns.add(uploaded);

        // Phones: one column with the type and, below it, years and upload date
        Grid.Column<DocumentSummary> compactColumn = grid.addColumn(LitRenderer.<DocumentSummary>of("""
                        <div class="my-documents__item">
                          <span class="my-documents__item-type">${item.type}</span>
                          <span class="my-documents__item-meta">${item.meta}</span>
                        </div>""")
                        .withProperty("type", d -> d.documentType().label())
                        .withProperty("meta", d -> "AY " + d.assessmentYear() + " · FY " + d.financialYear()
                                + " · " + uploadDate.format(d.uploadedAt())))
                .setHeader("Document")
                .setComparator(Comparator.comparing(DocumentSummary::uploadedAt).thenComparing(DocumentSummary::id))
                .setSortable(true);

        // UC-005: download link per row, shown in both layouts
        grid.addComponentColumn(this::downloadLink).setHeader("")
                .setAutoWidth(true).setFlexGrow(0).setTextAlign(ColumnTextAlign.END);

        CompactGrid.install(grid, wideColumns, compactColumn);
        grid.setItems(documents);
        // BR-02: most recent first by default, shown by the sort indicator
        grid.sort(List.of(new GridSortOrder<>(uploaded, SortDirection.DESCENDING)));
        return grid;
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
