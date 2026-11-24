package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.server.VaadinService;

/**
 * The heading of a view. It reads the same resolved page title the document
 * title is written from, so the two cannot drift.
 * <p>
 * In the internal shell it is the page's H1. Under the customer shell, which
 * draws the page's one H1 itself, a view's title is the H2 of its answer,
 * {@link #ofAnswer()}.
 */
public final class ViewHeading extends Composite<HtmlContainer> {

    private final boolean answer;

    /** The page's H1. */
    public ViewHeading() {
        this(false);
    }

    private ViewHeading(boolean answer) {
        this.answer = answer;
        addClassName(answer ? "answer-heading" : "view-heading");
        getContent().bindText(UI.getCurrent().routerStateSignal().map(ViewHeading::titleOf));
    }

    /** The H2 a view of the customer shell is titled by, under the shell's own H1. */
    public static ViewHeading ofAnswer() {
        return new ViewHeading(true);
    }

    @Override
    protected HtmlContainer initContent() {
        return answer ? new H2() : new H1();
    }

    private static String titleOf(RouterState state) {
        if (state.navigationTarget() == null) {
            return "";
        }
        return VaadinService.getCurrent().getRouter()
                .resolvePageTitle(state.navigationTarget(), state.routeParameters()).orElse("");
    }
}
