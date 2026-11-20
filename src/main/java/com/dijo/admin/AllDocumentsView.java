package com.dijo.admin;

import com.dijo.document.AdminDocumentRow;
import com.dijo.document.DocumentFilter;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.dijo.ui.CompactGrid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * UC-007: every document in the system, with filters by PAN / client name,
 * document type and upload date range. Admin only (BR-01).
 */
@Route(value = AllDocumentsView.ROUTE, autoLayout = false)
@PageTitle("All Documents | DocuShare")
@RolesAllowed("ADMIN")
public class AllDocumentsView extends Main {

    public static final String ROUTE = "admin/documents";
    static final String NO_DOCUMENTS = "No documents have been uploaded yet.";
    static final String NO_MATCHES = "No documents match your search criteria. Try different filters.";

    private static final DateTimeFormatter UPLOAD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final DocumentService documentService;
    private final DateTimeFormatter uploadDate;

    private final TextField search = new TextField("PAN or client name");
    private final Select<DocumentType> documentType = new Select<>();
    private final DatePicker uploadedFrom = new DatePicker("Uploaded from");
    private final DatePicker uploadedTo = new DatePicker("Uploaded to");
    private final Button clearFilters = new Button("Clear filters", VaadinIcon.CLOSE_SMALL.create());
    private final Span resultCount = new Span();

    private final Grid<AdminDocumentRow> grid = new Grid<>();
    private final Paragraph emptyText = new Paragraph();
    private final Button emptyClear = new Button("Clear filters");

    public AllDocumentsView(AuthenticationContext authenticationContext, DocumentService documentService,
                            Clock clock) {
        this.documentService = documentService;
        this.uploadDate = UPLOAD_DATE.withZone(clock.getZone());
        addClassName("dashboard");

        H2 title = new H2("All Documents");
        Button upload = new Button("Upload Document", VaadinIcon.UPLOAD.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(UploadDocumentView.class)));
        upload.addThemeVariants(ButtonVariant.PRIMARY);
        Div toolbar = new Div(title, upload);
        toolbar.addClassName("manage-clients__toolbar");

        configureFilters();
        configureGrid();

        Div content = new Div(toolbar, filterBar(), resultCount, grid);
        content.addClassNames("dashboard__content", "all-documents");

        add(new AdminHeader(authenticationContext), content);
        refresh();
    }

    private void configureFilters() {
        search.setPlaceholder("e.g. ABCDE1234F or Acme");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);

        documentType.setLabel("Document Type");
        documentType.setItems(DocumentType.values());
        documentType.setItemLabelGenerator(t -> t == null ? "All types" : t.label());
        documentType.setEmptySelectionAllowed(true);
        documentType.setEmptySelectionCaption("All types");

        uploadedFrom.setClearButtonVisible(true);
        uploadedTo.setClearButtonVisible(true);
        // Keep the range valid: "to" can't be before "from"
        uploadedFrom.addValueChangeListener(e -> uploadedTo.setMin(e.getValue()));
        uploadedTo.addValueChangeListener(e -> uploadedFrom.setMax(e.getValue()));

        search.addValueChangeListener(e -> refresh());
        documentType.addValueChangeListener(e -> refresh());
        uploadedFrom.addValueChangeListener(e -> refresh());
        uploadedTo.addValueChangeListener(e -> refresh());

        clearFilters.addThemeVariants(ButtonVariant.TERTIARY);
        clearFilters.addClickListener(e -> clearFilters());
        emptyClear.addClickListener(e -> clearFilters());
    }

    private Div filterBar() {
        Div filters = new Div(search, documentType, uploadedFrom, uploadedTo, clearFilters);
        filters.addClassName("all-documents__filters");
        resultCount.addClassName("all-documents__count");
        return filters;
    }

    private void configureGrid() {
        grid.addClassName("all-documents__grid");
        grid.addThemeVariants(GridVariant.ROW_STRIPES);
        List<Grid.Column<AdminDocumentRow>> wide = new ArrayList<>();

        wide.add(grid.addColumn(AdminDocumentRow::panNumber).setHeader("PAN")
                .setComparator(AdminDocumentRow::panNumber).setSortable(true).setAutoWidth(true));
        wide.add(grid.addColumn(r -> Objects.toString(r.clientName(), "")).setHeader("Client Name")
                .setComparator(Comparator.comparing(r -> Objects.toString(r.clientName(), ""),
                        String.CASE_INSENSITIVE_ORDER))
                .setSortable(true).setAutoWidth(true).setFlexGrow(1));
        wide.add(grid.addColumn(r -> r.documentType().label()).setHeader("Document Type")
                .setComparator(Comparator.comparing(r -> r.documentType().label()))
                .setSortable(true).setAutoWidth(true));
        wide.add(grid.addColumn(AdminDocumentRow::assessmentYear).setHeader("Assessment Year")
                .setComparator(AdminDocumentRow::assessmentYear).setSortable(true).setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END));
        wide.add(grid.addColumn(AdminDocumentRow::financialYear).setHeader("Financial Year")
                .setComparator(AdminDocumentRow::financialYear).setSortable(true).setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END));
        Grid.Column<AdminDocumentRow> uploaded = grid.addColumn(r -> uploadDate.format(r.uploadedAt()))
                .setHeader("Upload Date")
                .setComparator(Comparator.comparing(AdminDocumentRow::uploadedAt).thenComparing(AdminDocumentRow::id))
                .setSortable(true).setAutoWidth(true);
        wide.add(uploaded);
        wide.add(grid.addColumn(AdminDocumentRow::uploadedBy).setHeader("Uploaded By")
                .setComparator(AdminDocumentRow::uploadedBy).setSortable(true).setAutoWidth(true));
        wide.add(grid.addColumn(AdminDocumentRow::storedPath).setHeader("File")
                .setComparator(AdminDocumentRow::storedPath).setSortable(true).setAutoWidth(true)
                .setTooltipGenerator(AdminDocumentRow::storedPath));

        Grid.Column<AdminDocumentRow> compact = grid.addColumn(LitRenderer.<AdminDocumentRow>of("""
                        <div class="my-documents__item">
                          <span class="my-documents__item-type">${item.title}</span>
                          <span class="my-documents__item-meta">${item.client}</span>
                          <span class="my-documents__item-meta">${item.meta}</span>
                        </div>""")
                        .withProperty("title", r -> r.documentType().label())
                        .withProperty("client", r -> r.clientName() == null ? r.panNumber()
                                : r.clientName() + " · " + r.panNumber())
                        .withProperty("meta", r -> "AY " + r.assessmentYear() + " · FY " + r.financialYear()
                                + " · " + uploadDate.format(r.uploadedAt()) + " · by " + r.uploadedBy()))
                .setHeader("Document")
                .setComparator(Comparator.comparing(AdminDocumentRow::uploadedAt).thenComparing(AdminDocumentRow::id))
                .setSortable(true);
        CompactGrid.install(grid, wide, compact);

        // Step 3: most recent first, shown by the sort indicator
        grid.sort(List.of(new GridSortOrder<>(uploaded, SortDirection.DESCENDING)));

        Div empty = new Div(emptyText, emptyClear);
        empty.addClassName("all-documents__empty");
        grid.setEmptyStateComponent(empty);
    }

    private DocumentFilter currentFilter() {
        return new DocumentFilter(search.getValue(), documentType.getValue(), uploadedFrom.getValue(),
                uploadedTo.getValue());
    }

    /** Step 7: reload the list with the current filters. */
    private void refresh() {
        DocumentFilter filter = currentFilter();
        List<AdminDocumentRow> rows = documentService.findAll(filter);
        grid.setItems(rows);

        boolean filtered = !filter.isEmpty();
        clearFilters.setVisible(filtered);
        // AF-1 / AF-2
        emptyText.setText(filtered ? NO_MATCHES : NO_DOCUMENTS);
        emptyClear.setVisible(filtered);
        resultCount.setText(rows.size() == 1 ? "1 document" : rows.size() + " documents");
    }

    private void clearFilters() {
        search.clear();
        documentType.clear();
        uploadedFrom.clear();
        uploadedTo.clear();
        refresh();
    }
}
