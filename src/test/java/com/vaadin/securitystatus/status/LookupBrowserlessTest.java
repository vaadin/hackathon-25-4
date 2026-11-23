package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.DisableOnClickMode;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.PropertyChangeDeniedException;
import com.vaadin.flow.shared.JsonConstants;

/**
 * HOM-03 to HOM-07: the lookup cleans what is typed, moves a pasted advisory id
 * to its tab, keeps the question across tabs and navigation, withdraws it on
 * Escape, and takes the slash key only outside another field.
 *
 * <p>
 * The keyboard filters are JavaScript the browser runs, so a browserless test
 * cannot run them. What it proves is the server half: an event that passed the
 * declared filter acts, one that did not is ignored. The browser half was
 * verified with Playwright and is described in
 * {@code specs/reviews/03-home-and-lookup/REVIEW.md}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class LookupBrowserlessTest extends SpringBrowserlessTest {

    // HOM-03
    @Test
    void vaadin_24_9_20_typed_and_checked_asks_about_24_9_20() {
        navigate(HomeView.class);
        type("Vaadin 24.9.20");
        assertThat(check().isEnabled()).isTrue();

        test(check()).click();

        assertThat(getCurrentView()).isInstanceOf(VersionView.class);
        assertThat(path()).isEqualTo("version/24.9.20");
        assertThat(versionField().getValue()).isEqualTo("24.9.20");
    }

    // HOM-03
    @Test
    void a_version_committed_with_enter_is_cleaned_the_same_way() {
        navigate(HomeView.class);
        commit("V24.9.20 ");

        test(check()).click();

        assertThat(path()).isEqualTo("version/24.9.20");
    }

    // HOM-03, the not measured answer belongs to the version screen and not to the field
    @Test
    void a_version_nobody_measured_is_still_asked() {
        navigate(HomeView.class);
        assertThat(versionField().getListDataView().getItems()).doesNotContain("24.9.15").contains("24.9.14",
                "24.9.16");
        commit("24.9.15");

        test(check()).click();

        assertThat(path()).isEqualTo("version/24.9.15");
    }

    // HOM-04
    @Test
    void a_cve_pasted_into_the_version_field_lands_on_the_ids_tab_with_the_id() {
        navigate(HomeView.class);

        type("CVE-2026-45292");

        assertThat(test(tabs()).isSelected(1)).isTrue();
        assertThat(idsField().getValue()).isEqualTo("CVE-2026-45292");
        assertThat(idsField().isVisible() && parentVisible(idsField())).isTrue();
        assertThat(versionField().getValue()).isNull();
        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
    }

    // HOM-04
    @Test
    void a_cve_typed_into_the_version_field_moves_when_committed() {
        navigate(HomeView.class);

        commit("cve-2026-45292");

        assertThat(test(tabs()).isSelected(1)).isTrue();
        assertThat(idsField().getValue()).isEqualTo("cve-2026-45292");
    }

    // HOM-05
    @Test
    void a_version_survives_two_tab_switches() {
        navigate(HomeView.class);
        commit("24.9.20");

        test(tabs()).select(1);
        test(tabs()).select(0);

        assertThat(versionField().getValue()).isEqualTo("24.9.20");
        assertThat(check().isEnabled()).isTrue();
    }

    // P-14: a tab chosen under an answer starts a new question from the home page
    @Test
    void choosing_a_tab_under_an_answer_returns_to_the_home_page_and_keeps_the_question() {
        navigate("version/24.9.21", VersionView.class);

        test(tabs()).select(1);

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(test(tabs()).isSelected(1)).isTrue();
        test(tabs()).select(0);
        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(versionField().getValue()).isEqualTo("24.9.21");
    }

    // HOM-05, AC1
    @Test
    void the_ids_survive_tab_switches_and_an_answer_and_back() {
        navigate(HomeView.class);
        test(tabs()).select(1);
        test(idsField()).setValue("CVE-2026-45292");
        test(tabs()).select(0);
        test(tabs()).select(1);
        assertThat(idsField().getValue()).isEqualTo("CVE-2026-45292");

        test(check()).click();
        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        navigate(HomeView.class);

        assertThat(idsField().getValue()).isEqualTo("CVE-2026-45292");
    }

    // AC1
    @Test
    void navigating_to_an_answer_and_back_keeps_the_version() {
        navigate(HomeView.class);
        commit("24.9.20");
        test(check()).click();

        navigate(HomeView.class);

        assertThat(versionField().getValue()).isEqualTo("24.9.20");
    }

    // AC1, the session keeps the question for a second window too
    @Test
    void an_address_puts_its_question_into_the_lookup() {
        navigate("version/24.4.28", VersionView.class);

        assertThat(versionField().getValue()).isEqualTo("24.4.28");

        navigate("cve/CVE-2026-45292,GHSA-ccrj-gw8r-854m/24.9.20", CveView.class);

        assertThat(test(tabs()).isSelected(1)).isTrue();
        assertThat(idsField().getValue()).isEqualTo("CVE-2026-45292, GHSA-ccrj-gw8r-854m");
        assertThat(idsVersionField().getValue()).isEqualTo("24.9.20");
    }

    // HOM-06
    @Test
    void escape_on_an_answer_goes_home_with_the_question_withdrawn() {
        navigate("version/24.9.20", VersionView.class);
        assertThat(versionField().getValue()).isEqualTo("24.9.20");

        keydown(versionField().getElement(), Lookup.KEYS.escapeWithNothingOpen(), true);

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(versionField().getValue()).isNull();
        assertThat(check().isEnabled()).isFalse();
    }

    // HOM-06, the positive control: an Escape the filter did not pass changes nothing
    @Test
    void escape_that_only_closed_the_dropdown_withdraws_nothing() {
        navigate("version/24.9.20", VersionView.class);

        keydown(versionField().getElement(), Lookup.KEYS.escapeWithNothingOpen(), false);

        assertThat(getCurrentView()).isInstanceOf(VersionView.class);
        assertThat(versionField().getValue()).isEqualTo("24.9.20");
    }

    // HOM-06, the clear control
    @Test
    void clearing_the_version_field_on_an_answer_goes_home() {
        navigate("version/24.9.20", VersionView.class);

        test(versionField()).clickClearButton();

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
    }

    // HOM-06, on the ids tab
    @Test
    void emptying_the_ids_on_their_answer_goes_home() {
        navigate("cve/CVE-2026-45292", CveView.class);

        keydown(idsField().getElement(), Lookup.KEYS.escapeWithNothingOpen(), true);

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(idsField().getValue()).isEmpty();
    }

    // HOM-06, the version beside the ids
    @Test
    void emptying_the_version_beside_the_ids_keeps_the_ids_and_drops_the_version() {
        navigate("cve/CVE-2026-45292/24.9.20", CveView.class);

        fire(idsVersionField().getElement(), "change", Lookup.KEYS.emptied(), true);

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(path()).isEqualTo("cve/CVE-2026-45292");
    }

    // HOM-07
    @Test
    void slash_while_typing_in_a_dialog_text_field_steals_no_focus() {
        navigate(HomeView.class);
        var field = new TextField();
        var dialog = new Dialog(field);
        UI.getCurrent().add(dialog);
        dialog.open();
        test(field).focus();
        assertThat(test(field).isFocused()).isTrue();

        keydown(UI.getCurrent().getElement(), Lookup.KEYS.slashOutsideAField(), false);

        assertThat(test(field).isFocused()).isTrue();
        assertThat(test(versionField()).isFocused()).isFalse();
    }

    // HOM-07, the positive control: a slash outside any field focuses the lookup
    @Test
    void slash_outside_a_field_focuses_the_lookup() {
        navigate(HomeView.class);
        assertThat(test(versionField()).isFocused()).isFalse();

        keydown(UI.getCurrent().getElement(), Lookup.KEYS.slashOutsideAField(), true);

        assertThat(test(versionField()).isFocused()).isTrue();
    }

    // AC4
    @Test
    void check_is_disabled_while_the_question_is_empty_and_on_click_until_the_response() {
        navigate(HomeView.class);

        assertThat(check().isEnabled()).isFalse();
        assertThat(check().isDisableOnClick()).isTrue();
        assertThat(check().getDisableOnClickMode()).isEqualTo(DisableOnClickMode.UNTIL_RESPONSE);

        type("24.9");
        assertThat(check().isEnabled()).isTrue();
    }

    // The edge case: garbage among ids
    @Test
    void ids_that_parse_are_asked_and_the_rest_are_reported_in_place() {
        navigate(HomeView.class);
        test(tabs()).select(1);
        test(idsField()).setValue("cve-2026-45292, foo; GHSA-CCRJ-GW8R-854M\nbar");

        test(check()).click();

        assertThat(path()).isEqualTo("cve/CVE-2026-45292,GHSA-ccrj-gw8r-854m");
        assertThat(note().getText()).isEqualTo("Not recognised: foo, bar");
        assertThat(note().isVisible()).isTrue();
    }

    // The edge case: nothing but garbage
    @Test
    void a_list_with_no_id_in_it_is_not_asked() {
        navigate(HomeView.class);
        test(tabs()).select(1);
        test(idsField()).setValue("foo bar");

        test(check()).click();

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(note().getText()).isEqualTo("No CVE or GHSA id in that");
    }

    @Test
    void the_example_in_the_empty_field_is_the_newest_measured_head_and_never_a_value() {
        navigate(HomeView.class);

        assertThat(versionField().getPlaceholder()).isEqualTo("e.g. 25.3.5");
        assertThat(versionField().getValue()).isNull();
        assertThat(versionField().isClearButtonVisible()).isTrue();
    }

    @Test
    void each_tab_offers_its_help_from_a_question_mark() {
        navigate(HomeView.class);

        assertThat(inLookup(Button.class, "help-mark")).extracting(mark -> mark.getAriaLabel().orElse(""))
                .containsExactly("How to enter your version", "How to use this lookup",
                        "What is assessed, and how to produce a report");
    }

    // Helpers

    /** The fields of every tab, the hidden ones too, which a component query leaves out. */
    private <T extends Component> List<T> inLookup(Class<T> type, String className) {
        var found = new ArrayList<T>();
        collect(find(Lookup.class).single(), type, className, found);
        return found;
    }

    private static <T extends Component> void collect(Component from, Class<T> type, String className,
            List<T> found) {
        if (type.isInstance(from) && from instanceof HasStyle styled && styled.hasClassName(className)) {
            found.add(type.cast(from));
        }
        from.getChildren().forEach(child -> collect(child, type, className, found));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> versionField() {
        return inLookup(ComboBox.class, "lookup-version").getFirst();
    }

    private TextField idsField() {
        return inLookup(TextField.class, "lookup-ids").getFirst();
    }

    private TextField idsVersionField() {
        return inLookup(TextField.class, "lookup-ids-version").getFirst();
    }

    private Button check() {
        return find(Button.class).withClassName("lookup-check").single();
    }

    private Tabs tabs() {
        return find(Tabs.class).withClassName("lookup-tabs").single();
    }

    private Span note() {
        return find(Span.class).withClassName("lookup-note").single();
    }

    private static boolean parentVisible(TextField field) {
        return field.getParent().map(parent -> parent.isVisible()).orElse(false);
    }

    private static String path() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }

    /** Text arriving in the version field before it is committed, as the browser reports it. */
    private void type(String text) {
        try {
            versionField().getElement().getNode().getFeature(ElementPropertyMap.class)
                    .deferredUpdateFromClient("filter", text).run();
        } catch (PropertyChangeDeniedException denied) {
            throw new AssertionError("The filter is not synchronised from the browser", denied);
        }
    }

    /** Text committed in the version field with Enter or by leaving it. */
    private void commit(String text) {
        type(text);
        ComponentUtil.fireEvent(versionField(),
                new ComboBoxBase.CustomValueSetEvent<>(versionField(), true, text));
    }

    private static void keydown(Element element, DomEventFilter filter, boolean passed) {
        fire(element, "keydown", filter, passed);
    }

    /** An event as the browser sends it: with the value the declared filter returned. */
    private static void fire(Element element, String type, DomEventFilter filter, boolean passed) {
        var data = JacksonUtils.createObjectNode();
        data.put(JsonConstants.EVENT_SNIPPET_KEY_PREFIX + filter.getSnippetKey(), passed);
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(new DomEvent(element, type, data));
    }
}
