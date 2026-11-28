package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.securitystatus.data.FixturePages;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * POL-03, the rule 4 sweep: the page never invents a value. For each screen,
 * one fixture or constructed record that lacks fields is opened, and the whole
 * page is walked: no text holder is rendered empty, no grid cell holds an
 * element with nothing in it, and nothing prints a placeholder such as
 * {@code null}. Then the one omission the record was built for is asserted, and
 * where the same page shows the field for a record that carries it, that is
 * asserted too, so the omission cannot pass by the field never being drawn.
 *
 * <p>
 * A grid cell for an absent value holds no element at all: in a table the
 * column is there for every row, so the cell itself cannot be left out, but
 * nothing is drawn in it, D-159.
 *
 * <p>
 * The sweep and its helpers; the customer's screens are swept in
 * {@code AbsenceSweepBrowserlessTest} and the team's in its own package, so
 * the team's tests go with the team, D-254.
 */
public abstract class AbsenceSweep extends SpringBrowserlessTest {

    /** Elements whose whole purpose is to hold text, so empty means drawn empty. */
    protected static final Set<String> TEXT_HOLDERS = Set.of("span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "li",
            "td", "th", "a", "strong", "em", "dt", "dd", "label", "mark", "code", "small", "b", "summary", "button",
            "vaadin-button", "vaadin-badge", "vaadin-details-summary");

    /** What a value the data does not carry prints as when it leaks through. */
    protected static final Pattern PLACEHOLDER = Pattern
            .compile("\\bnull\\b|\\bundefined\\b|\\bNaN\\b|Optional\\[|Optional\\.empty");

    protected static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    protected FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // The sweep

    /**
     * Walks the page. Fails on an empty text holder, an empty element in a grid
     * cell, or a placeholder anywhere. Returns the cells left blank, grid cells
     * as {@code <grid class> <column key> <row>} and table cells as
     * {@code td <row header> <position>}, for the caller to account for.
     */
    protected List<String> sweep() {
        var problems = new ArrayList<String>();
        var blanks = new ArrayList<String>();
        UI.getCurrent().getChildren().forEach(child -> walk(child.getElement(), problems, blanks));
        String text = pageText();
        var leak = PLACEHOLDER.matcher(text);
        if (leak.find()) {
            problems.add("placeholder " + leak.group());
        }

        for (Grid<?> grid : allGrids()) {
            gridCells(grid, problems, blanks);
        }
        assertThat(problems).isEmpty();
        return blanks;
    }

    protected static void walk(Element element, List<String> problems) {
        walk(element, problems, new ArrayList<>());
    }

    /**
     * A table cell is the table's own, like a grid cell: one with nothing in it
     * is the omission of an absent value, so it goes to {@code blanks}, named by
     * the header cell of its row. Any other empty text holder is a problem.
     */
    protected static void walk(Element element, List<String> problems, List<String> blanks) {
        if (element.isTextNode() || !element.isVisible() || "true".equals(element.getAttribute("aria-hidden"))) {
            return;
        }
        boolean holdsElements = element.getChildren().anyMatch(child -> !child.isTextNode());
        boolean empty = !holdsElements && element.getTextRecursively().isBlank();
        if (empty && "td".equals(element.getTag())) {
            Element rowHeader = element.getParent().getChild(0);
            blanks.add("td " + rowHeader.getTextRecursively().strip() + " " + element.getParent().indexOfChild(element));
        } else if (empty && TEXT_HOLDERS.contains(element.getTag()) && element.getAttribute("aria-label") == null
                && element.getAttribute("title") == null) {
            problems.add("empty <" + element.getTag() + " class=\"" + element.getAttribute("class") + "\"> in "
                    + element.getParent().getTextRecursively());
        }
        element.getChildren().forEach(child -> walk(child, problems, blanks));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void gridCells(Grid grid, List<String> problems, List<String> blanks) {
        if (!grid.isVisible()) {
            return;
        }
        var tester = test(grid);
        List<Grid.Column> columns = grid.getColumns();
        for (int row = 0; row < tester.size(); row++) {
            for (int position = 0; position < columns.size(); position++) {
                Grid.Column column = columns.get(position);
                String where = String.join(" ", grid.getClassNames()) + " "
                        + (column.getKey() == null ? "column" + position : column.getKey()) + " " + row;
                if (column.getRenderer() instanceof ComponentRenderer) {
                    Component cell = (Component) tester.getCellComponent(row, position);
                    if (cell == null || cell.getElement().isTextNode()) {
                        blanks.add(where);
                    } else if (cell.getElement().getTextRecursively().isBlank()
                            && cell.getElement().getChildCount() == 0) {
                        problems.add("empty <" + cell.getElement().getTag() + "> in " + where);
                    }
                } else {
                    String text = tester.getCellText(row, position);
                    if (text == null || text.isBlank()) {
                        blanks.add(where);
                    } else if (PLACEHOLDER.matcher(text).find()) {
                        problems.add("placeholder in " + where + ": " + text);
                    }
                }
            }
        }
    }

    // Helpers

    /**
     * Serves a changed copy of one document from now on. Everything cached is
     * forgotten first, since another test class in the shared context may have
     * read the committed document already; so one test makes one edit.
     */
    protected void edit(String path, Consumer<ObjectNode> change) {
        fixtures.cold();
        var root = (ObjectNode) JSON.readTree(fixtures.transport().fixture(path));
        change.accept(root);
        fixtures.transport().replace(path, JSON.writeValueAsString(root));
        fixtures.revalidate();
    }

    protected List<Grid<?>> allGrids() {
        var grids = new ArrayList<Grid<?>>();
        UI.getCurrent().getChildren().forEach(child -> collectGrids(child, grids));
        return grids;
    }

    protected static void collectGrids(Component from, List<Grid<?>> grids) {
        if (from instanceof Grid<?> grid) {
            grids.add(grid);
        }
        from.getChildren().forEach(child -> collectGrids(child, grids));
    }

    protected static String pageText() {
        return String.join(" ", UI.getCurrent().getChildren().map(child -> child.getElement().getTextRecursively())
                .toList());
    }

    protected static Set<String> classesIn(HasElement from) {
        var names = new TreeSet<String>();
        collectClasses(from.getElement(), names);
        return names;
    }

    protected static void collectClasses(Element element, Set<String> names) {
        if (element.isTextNode()) {
            return;
        }
        names.addAll(element.getClassList());
        element.getChildren().forEach(child -> collectClasses(child, names));
    }

    protected static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }
}
