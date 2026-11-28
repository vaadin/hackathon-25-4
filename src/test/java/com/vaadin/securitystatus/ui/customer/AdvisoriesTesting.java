package com.vaadin.securitystatus.ui.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.internal.GridContextMenuSupport;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenuTester;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;

import tools.jackson.databind.node.ObjectNode;

/**
 * What the tests of the advisories list share: opening a row's menu, its
 * rows, its verdict pills and where the browser is. The team's copy of the list
 * is tested in its own package with these, so its tests go with the team, D-254.
 */
public abstract class AdvisoriesTesting extends SpringBrowserlessTest {

    /**
     * Opens the context menu on a row the way the browser asks for it, with the
     * row's key and the column's internal id in the event detail.
     * {@code GridContextMenuTester.open} sends the key alone, and the 25.4
     * dynamic content provider reads the column too, so the tester's open ends
     * in a NullPointerException inside {@code GridContextMenu.onBeforeOpenMenu}.
     * specs/FEEDBACK-25.4.md, draft 18.
     */
    protected GridContextMenuTester<GridContextMenu<AdvisoryCatalogEntry>, AdvisoryCatalogEntry> openMenu(
            AdvisoriesView view, int row) {
        Grid<AdvisoryCatalogEntry> grid = view.grid();
        String key = GridContextMenuSupport.getItemKey(grid, rows(grid).get(row));
        String column = GridContextMenuSupport.getColumnInternalId(grid, "id");
        GridContextMenuSupport.setTargetItem(grid, key, column);
        ObjectNode detail = JacksonUtils.createObjectNode();
        detail.put("key", key);
        detail.put("internalColumnId", column);
        ObjectNode data = JacksonUtils.createObjectNode();
        data.set("event.detail", detail);
        grid.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(grid.getElement(), GridContextMenuSupport.BEFORE_OPEN_EVENT, data));
        roundTrip();
        assertThat(view.menu().isAttached()).as("the menu opened").isTrue();
        return test(view.menu());
    }

    protected static List<AdvisoryCatalogEntry> rows(Grid<AdvisoryCatalogEntry> grid) {
        return grid.getLazyDataView().getItems().toList();
    }

    protected List<RouterLink> pills() {
        return $(RouterLink.class).withClassName("verdict-pill").all();
    }

    protected String currentLocation() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPathWithQueryParameters();
    }
}
