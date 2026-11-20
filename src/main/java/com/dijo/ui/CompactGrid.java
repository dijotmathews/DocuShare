package com.dijo.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.shared.Registration;

import java.util.List;

/**
 * Collapses a read-only grid to a single summary column on narrow windows,
 * instead of letting it scroll sideways (design system: Grid).
 */
public final class CompactGrid {

    /** Below this window width the grid shows only its compact column. */
    public static final int COMPACT_BELOW_PX = 640;

    private CompactGrid() {
    }

    /**
     * Shows {@code wideColumns} on wide windows and {@code compactColumn} on
     * narrow ones, following the browser window as it is resized.
     */
    public static <T> void install(Grid<T> grid, List<Grid.Column<T>> wideColumns, Grid.Column<T> compactColumn) {
        Runnable wide = () -> apply(wideColumns, compactColumn, false);
        wide.run();
        grid.addAttachListener(attach -> {
            Page page = attach.getUI().getPage();
            page.retrieveExtendedClientDetails(details ->
                    apply(wideColumns, compactColumn, details.getWindowInnerWidth() < COMPACT_BELOW_PX));
            Registration resize = page.addBrowserWindowResizeListener(e ->
                    apply(wideColumns, compactColumn, e.getWidth() < COMPACT_BELOW_PX));
            grid.addDetachListener(detach -> {
                resize.remove();
                detach.unregisterListener();
            });
        });
    }

    private static <T> void apply(List<Grid.Column<T>> wideColumns, Grid.Column<T> compactColumn, boolean compact) {
        wideColumns.forEach(c -> c.setVisible(!compact));
        compactColumn.setVisible(compact);
    }
}
