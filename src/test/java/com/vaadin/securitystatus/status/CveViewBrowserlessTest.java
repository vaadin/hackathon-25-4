package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * CVE-01 to CVE-05, CVE-07 and CVE-08: the answer by advisory, from the
 * committed advisory documents and the four committed version documents.
 * 24.9.21 carries one advisory of each state: CVE-2026-89407 fixed by its
 * upgrade, CVE-2026-45292 pinned by Spring Boot, CVE-2026-102276 not
 * assessed, CVE-2026-8763 through a discontinued product, CVE-2026-54285 and
 * CVE-2021-33604 ruled out, and CVE-2019-25028 not reported at all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CveViewBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // CVE-01, AC1: the fix release named is on the reader's line
    @Test
    void a_fixed_advisory_on_an_affected_version_names_the_release_on_the_readers_line() {
        open("cve/CVE-2026-89407/24.9.21");

        Section card = card("CVE-2026-89407");
        assertThat(card.hasClassName("state-fix_available")).isTrue();
        assertThat(word(card)).isEqualTo("Fix available");
        assertThat(title(card)).isEqualTo("CVE-2026-89407 is fixed in 24.9.26");
        assertThat(why(card)).isEqualTo("You are on 24.9.21, which is affected. Upgrade to 24.9.26, a maintenance"
                + " release in the minor you already use, and you are secure.");
        Anchor notes = find(Anchor.class, card).withClassName("verdict-action").single();
        assertThat(notes.getText()).isEqualTo("View 24.9.26 release notes");
        assertThat(notes.getHref()).isEqualTo("https://github.com/vaadin/platform/releases/tag/24.9.26");
        assertThat(copy(card).getText()).isEqualTo("Copy answer for your ticket");
        // No badges on this card, rule 6.11.12.
        assertThat(card.getElement().getTextRecursively()).doesNotContain("Maintenance release", "Vaadin issue");
    }

    // CVE-02
    @Test
    void with_no_version_the_card_is_the_measured_one_and_claims_nothing_about_the_reader() {
        open("cve/CVE-2026-89407");

        Section card = card("CVE-2026-89407");
        assertThat(word(card)).isEqualTo("Measured");
        assertThat(title(card)).isEqualTo("CVE-2026-89407, across the versions we follow");
        assertThat(why(card)).startsWith("Enter your version above and this answers for it.");
        assertThat(find(Anchor.class, card).withClassName("verdict-action").exists()).isFalse();
        // Nothing to copy into a ticket until there is a version to answer for, P-10.
        assertThat(find(Button.class, card).withClassName("copy-answer").exists()).isFalse();
        assertThat(lineCards("CVE-2026-89407")).noneMatch(line -> line.hasClassName("yours"));
        // No verdict is quoted as the reader's: the four verdicts are listed with their coordinates instead.
        Section answer = answer("CVE-2026-89407");
        assertThat(pairHeadings(answer)).doesNotContain("Can your deployed application be exploited?", "Do this now");
        assertThat(find(Table.class, answer).withClassName("cve-verdicts-table").single().getBodyRows()).hasSize(4);
        assertThat(text(answer)).doesNotContain("You are on", "your version is");
    }

    // CVE-03 and the edge case: mixed separators, order kept, duplicates collapsed
    @Test
    void three_ids_with_mixed_separators_answer_in_the_order_asked() {
        navigate(HomeView.class);
        test(find(Tabs.class).withClassName("lookup-tabs").single()).select(1);
        test(inLookup(TextField.class, "lookup-ids").getFirst())
                .setValue("CVE-2021-33604;\nCVE-2026-45292, cve-2016-1000027; CVE-2021-33604");
        test(inLookup(TextField.class, "lookup-ids-version").getFirst()).setValue("24.9.21");
        test(find(Button.class).withClassName("lookup-check").single()).click();

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(answers()).containsExactly("CVE-2021-33604", "CVE-2026-45292", "CVE-2016-1000027");
        assertThat(find(H3.class).all()).extracting(H3::getText).contains("3 advisories", "Details for CVE-2021-33604",
                "Details for CVE-2026-45292", "Details for CVE-2016-1000027");
        assertThat(text(find(com.vaadin.flow.component.html.Div.class).withClassName("cve-body").single()))
                .contains("Answered for 24.9.21");
    }

    // CVE-04, C6
    @Test
    void an_unknown_id_is_answered_plainly_and_nothing_else() {
        open("cve/CVE-2099-1234/24.9.21");

        Section answer = answer("CVE-2099-1234");
        Section card = card("CVE-2099-1234");
        assertThat(word(card)).isEqualTo("Not in our measurements");
        assertThat(title(card)).isEqualTo("CVE-2099-1234 is not in anything we scanned");
        assertThat(answer.getChildren().toList()).containsExactly(card);
        assertThat(find(Button.class, answer).exists()).isFalse();
        assertThat(find(Details.class).exists()).isFalse();
        assertThat(find(RouterLink.class).withClassName("cve-line-card").exists()).isFalse();
    }

    // D-226: an address whose ids do not parse gets the same unknown card
    @Test
    void an_address_whose_id_does_not_parse_is_answered_as_not_in_anything_we_scanned() {
        open("cve/GHSA-aaaa-bbbb-cccc");

        Section answer = answer("GHSA-aaaa-bbbb-cccc");
        Section card = card("GHSA-aaaa-bbbb-cccc");
        assertThat(word(card)).isEqualTo("Not in our measurements");
        assertThat(title(card)).isEqualTo("GHSA-aaaa-bbbb-cccc is not in anything we scanned");
        assertThat(answer.getChildren().toList()).containsExactly(card);
        assertThat(find(Button.class, answer).exists()).isFalse();
    }

    @Test
    void an_id_that_does_not_parse_beside_one_that_does_gets_its_own_card_after_it() {
        open("cve/CVE-2021-33604,foo/24.9.21");

        assertThat(answers()).containsExactly("CVE-2021-33604", "foo");
        assertThat(title(card("foo"))).isEqualTo("foo is not in anything we scanned");
    }

    // CVE-05, AC4
    @Test
    void the_line_cards_follow_the_coverage_and_the_readers_line_is_marked_with_its_release() {
        open("cve/CVE-2026-89407/24.9.21");

        List<RouterLink> cards = lineCards("CVE-2026-89407");
        assertThat(cards).extracting(this::text).containsExactly(
                "25.3 Fixed In 25.3.5 and later",
                "24.10 Affects you Newest is 24.10.14",
                "24.9 · yours Fix available In 24.9.26 and later",
                "24.7 Affects you Newest is 24.7.19",
                "24.4 Never affected Nothing to do",
                "23.6 Never affected Nothing to do",
                "14.14 Never affected Nothing to do",
                "8.32 Not affected Measured on 8.32.3",
                "7.7 Not affected Measured on 7.7.56");
        assertThat(cards.stream().filter(card -> card.hasClassName("yours")).toList()).hasSize(1);
        assertThat(cards.get(2).hasClassName("yours")).isTrue();
        assertThat(cards.get(2).getHref()).isEqualTo("version/24.9.26");
        assertThat(cards.get(0).hasClassName("state-secure")).isTrue();
        assertThat(cards.get(1).hasClassName("state-unresolved")).isTrue();
    }

    // CVE-07, AC3: two verdicts, each naming its coordinate, the difference marked
    @Test
    void a_two_package_advisory_lists_both_verdicts_with_their_coordinates_marked() {
        open("cve/CVE-2026-54285/24.9.21");

        Section answer = answer("CVE-2026-54285");
        Details modules = layer(answer, "modules");
        Table verdicts = find(Table.class, modules).withClassName("cve-verdicts-table").single();
        List<TableRow> rows = verdicts.getBodyRows();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.getCells().get(0).getElement().getTextRecursively())
                .containsExactly("io.opentelemetry:opentelemetry-api", "@opentelemetry/core");
        assertThat(rows).extracting(row -> row.getCells().get(1).getElement().getTextRecursively())
                .containsExactly("Scanner error", "No risk here");
        // Nothing in common between the two, so each is marked whole.
        assertThat(find(Mark.class, verdicts).all()).extracting(Mark::getText)
                .containsExactly("io.opentelemetry:opentelemetry-api", "@opentelemetry/core");
        assertThat(text(modules)).contains("This advisory reaches Vaadin through 2 packages and carries a verdict for each.");
    }

    @Test
    void the_marked_part_is_only_where_the_coordinates_differ() {
        assertThat(CveRules.markDifference(List.of("com.vaadin:flow-server", "com.vaadin:flow-client")))
                .extracting(CveRules.Marked::differs).containsExactly("server", "client");
        assertThat(CveRules.markDifference(List.of("com.fasterxml.jackson.core:jackson-core",
                "com.fasterxml.jackson.core:jackson-databind"))).extracting(CveRules.Marked::differs)
                .containsExactly("core", "databind");
        assertThat(CveRules.markDifference(List.of("one:thing"))).extracting(CveRules.Marked::differs)
                .containsExactly("");
    }

    // CVE-08, AC3: absent fields are omitted, and only the first layer is open
    @Test
    void an_advisory_with_no_workaround_has_no_workaround_row_and_only_the_first_layer_open() {
        open("cve/CVE-2021-33604/24.9.21");

        Section answer = answer("CVE-2021-33604");
        assertThat(find(Details.class, answer).all()).extracting(Details::isOpened).containsExactly(true, false, false);
        assertThat(pairHeadings(answer)).doesNotContain("Do this now");
        assertThat(pairHeadings(answer)).contains("Can your deployed application be exploited?", "Vaadin's assessment",
                "Source");
        // Nothing drawn empty: every paragraph and markdown block says something.
        assertThat(find(Paragraph.class, answer).all()).allSatisfy(
                paragraph -> assertThat(paragraph.getElement().getTextRecursively()).isNotBlank());
        assertThat(find(Markdown.class, answer).all()).allSatisfy(md -> assertThat(md.getContent()).isNotBlank());
        assertThat(find(Table.class, answer).all()).flatExtracting(Table::getBodyRows)
                .flatExtracting(TableRow::getCells)
                .allSatisfy(cell -> assertThat(cell.getElement().getTextRecursively()).isNotBlank());
    }

    // AC1: every state in the fixture set renders its wording
    @Test
    void every_settled_state_on_the_fixtures_has_its_words() {
        open("cve/CVE-2026-45292,CVE-2026-102276,CVE-2026-8763,CVE-2021-33604,CVE-2019-25028/24.9.21");

        assertThat(word(card("CVE-2026-45292"))).isEqualTo("Affects you");
        assertThat(title(card("CVE-2026-45292"))).isEqualTo("CVE-2026-45292 affects 24.9.21 through Spring Boot");
        assertThat(why(card("CVE-2026-45292"))).endsWith("no Vaadin release moves it.");

        assertThat(word(card("CVE-2026-102276"))).isEqualTo("Not assessed yet");
        assertThat(title(card("CVE-2026-102276"))).isEqualTo("CVE-2026-102276 is still being reviewed");

        assertThat(title(card("CVE-2026-8763"))).isEqualTo("CVE-2026-8763 affects 24.9.21 through control-center-starter");
        assertThat(card("CVE-2026-8763").hasClassName("state-unresolved")).isTrue();

        assertThat(word(card("CVE-2021-33604"))).isEqualTo("Does not affect you");
        assertThat(title(card("CVE-2021-33604"))).isEqualTo("CVE-2021-33604 does not affect 24.9.21");
        assertThat(why(card("CVE-2021-33604"))).startsWith("None. The advisory is about flow-server");

        // Never reported on the 24.9 line: not reported, and not called fixed, D-66.
        assertThat(word(card("CVE-2019-25028"))).isEqualTo("Does not affect you");
        assertThat(title(card("CVE-2019-25028"))).isEqualTo("CVE-2019-25028 is not reported on 24.9.21");
    }

    // D-187: a risk verdict whose every route is the reader's own Spring Boot does not affect them
    // through Vaadin, as the prototype answers it, and the card says where the verdict lives.
    @Test
    void a_risk_only_through_the_readers_spring_boot_does_not_affect_them() {
        fixtures.cold();
        String version = "cases/v/24.9.21.json";
        fixtures.transport().replace(version,
                fixtures.transport().fixture(version).replace("CVE-2026-47886", "CVE-2016-1000027"));

        open("cve/CVE-2016-1000027/24.9.21");

        Section card = card("CVE-2016-1000027");
        assertThat(card.hasClassName("state-secure")).isTrue();
        assertThat(word(card)).isEqualTo("Does not affect you");
        assertThat(title(card)).isEqualTo("CVE-2016-1000027 does not affect 24.9.21");
        assertThat(why(card)).endsWith("Every route to it comes from the Spring Boot your application declares, "
                + "so the fix is a newer Spring Boot or a version property in your own pom; no Vaadin release moves it.");
    }

    @Test
    void an_advisory_on_a_version_with_no_fix_says_it_affects_it() {
        open("cve/CVE-2026-45292/24.7.19");

        Section card = card("CVE-2026-45292");
        assertThat(word(card)).isEqualTo("Affects you");
        assertThat(title(card)).isEqualTo("CVE-2026-45292 affects 24.7.19");
        assertThat(find(Anchor.class, card).withClassName("verdict-action").exists()).isFalse();
    }

    // The edge case: an id fixed below the reader's version
    @Test
    void an_advisory_fixed_below_the_readers_version_is_fixed_for_them() {
        // Reported on 25.3.0 to 25.3.4 and gone in 25.3.5.
        open("cve/CVE-2026-89407/25.3.5");

        Section card = card("CVE-2026-89407");
        assertThat(word(card)).isEqualTo("Fixed for you");
        assertThat(title(card)).isEqualTo("CVE-2026-89407 is not reported on 25.3.5");
        assertThat(text(layer(answer("CVE-2026-89407"), "evidence"))).contains(
                "Reported by our scans on 25.3.0 to 25.3.4, 5 releases, of the 25.3 line.",
                "com.fasterxml.jackson.core:jackson-core updated from 2.21.5 to 2.21.7 in 25.3.5.");
    }

    // D-196: the commentary of the reader's verdict, which only the advisory document carries
    @Test
    void the_evidence_layer_carries_the_commentary_of_the_readers_verdict() {
        open("cve/CVE-2026-89407/24.9.21");

        Details evidence = layer(answer("CVE-2026-89407"), "evidence");
        assertThat(find(com.vaadin.flow.component.markdown.Markdown.class, evidence).all())
                .extracting(com.vaadin.flow.component.markdown.Markdown::getContent)
                .anySatisfy(text -> assertThat(text).startsWith("The version is your Spring Boot's: 3.5.16"));
        // The measured range and the range the verdict is written for are one paragraph, D-188.
        assertThat(text(evidence)).contains("of the 24.9 line. The verdict is written for Vaadin");
    }

    // The edge case: an alias asked, answered under the tracked id
    @Test
    void a_ghsa_alias_beside_its_cve_is_answered_once_under_the_tracked_id() {
        open("cve/GHSA-rcgg-9c38-7xpx,CVE-2026-45292/24.9.21");

        assertThat(answers()).containsExactly("CVE-2026-45292");
        assertThat(find(Paragraph.class, answer("CVE-2026-45292")).withClassName("cve-record").single().getElement()
                .getTextRecursively()).contains("Your scanner may print it as GHSA-rcgg-9c38-7xpx.");
    }

    @Test
    void a_ghsa_we_do_not_track_is_not_said_to_be_in_nothing_we_scanned() {
        open("cve/GHSA-4wrc-f8pq-fpqp");

        Section card = card("GHSA-4wrc-f8pq-fpqp");
        assertThat(word(card)).isEqualTo("Not in our measurements");
        assertThat(title(card)).isEqualTo("We do not track GHSA-4wrc-f8pq-fpqp under that id");
        assertThat(text(card)).doesNotContain("not in anything we scanned");
    }

    // The edge case: a version given that we never measured
    @Test
    void an_unmeasured_version_is_said_once_above_the_cards() {
        open("cve/CVE-2026-89407,CVE-2021-33604/24.9.99");

        assertThat(find(com.vaadin.flow.component.html.Div.class).withClassName("cve-unmeasured").all()).hasSize(1);
        assertThat(text(find(com.vaadin.flow.component.html.Div.class).withClassName("cve-unmeasured").single()))
                .startsWith("We have not measured Vaadin 24.9.99.");
        assertThat(word(card("CVE-2026-89407"))).isEqualTo("Measured");
        assertThat(word(card("CVE-2021-33604"))).isEqualTo("Measured");
        RouterLink nearest = find(RouterLink.class).withClassName("nearest-measured").single();
        assertThat(nearest.getHref()).isEqualTo("cve/CVE-2026-89407,CVE-2021-33604/24.9.26");
        assertThat(lineCards("CVE-2026-89407").get(2).hasClassName("yours")).isTrue();
    }

    // AC2, and the epic's definition of done: the copied answer holds only what the card shows
    @Test
    void the_copied_answer_is_the_cards_own_words_and_nothing_else() {
        open("cve/CVE-2026-89407/24.9.21");

        Section card = card("CVE-2026-89407");
        String copied = copy(card).getElement().getAttribute("data-answer");
        assertThat(copied).startsWith("Fix available: CVE-2026-89407 is fixed in 24.9.26\n"
                + "You are on 24.9.21, which is affected. Upgrade to 24.9.26, a maintenance release in the minor you"
                + " already use, and you are secure.\nScanned 2026-11-15\n");
        // The last line is the address of this answer, so a ticket can come back to it, P-10.
        String address = copied.substring(copied.lastIndexOf('\n') + 1);
        assertThat(address).endsWith("/cve/CVE-2026-89407/24.9.21");
        copied = copied.substring(0, copied.lastIndexOf('\n'));
        // It names the id, the verdict, the reader's version and the move.
        assertThat(copied).contains("CVE-2026-89407", "Fix available", "24.9.21", "24.9.26");
        // Every line of it is a text the card shows.
        String shown = text(card);
        for (String line : copied.split("\n")) {
            assertThat(shown).contains(line.replace("Fix available: ", ""));
        }
    }

    @Test
    void with_the_data_out_of_reach_the_screen_says_so_and_invents_nothing() {
        fixtures.coldAndDown();

        open("cve/CVE-2026-89407/24.9.21");

        assertThat(find(DataOutageNotice.class).exists()).isTrue();
        assertThat(find(Section.class).withClassName("verdict-card").exists()).isFalse();
    }

    // Helpers

    /** Leaves for home first, so a second open in one test is a new question as it would be in a browser. */
    private void open(String address) {
        navigate(HomeView.class);
        navigate(address, CveView.class);
    }

    private List<String> answers() {
        return find(Section.class).withClassName("cve-answer").all().stream()
                .map(section -> section.getElement().getAttribute("data-advisory")).toList();
    }

    private Section answer(String id) {
        return find(Section.class).withClassName("cve-answer").all().stream()
                .filter(section -> id.equals(section.getElement().getAttribute("data-advisory"))).findFirst()
                .orElseThrow();
    }

    private Section card(String id) {
        return find(Section.class, answer(id)).withClassName("verdict-card").single();
    }

    private Button copy(Section card) {
        return find(Button.class, card).withClassName("copy-answer").single();
    }

    private List<RouterLink> lineCards(String id) {
        return find(RouterLink.class, answer(id)).withClassName("cve-line-card").all();
    }

    private Details layer(Section answer, String key) {
        return find(Details.class, answer).all().stream()
                .filter(details -> key.equals(details.getElement().getAttribute("data-layer"))).findFirst()
                .orElseThrow();
    }

    private List<String> pairHeadings(Component within) {
        List<H4> found = new ArrayList<>();
        collect(within, H4.class, found);
        return found.stream().map(H4::getText).toList();
    }

    private String word(Section card) {
        return find(com.vaadin.flow.component.html.Div.class, card).withClassName("verdict-state").single()
                .getElement().getTextRecursively();
    }

    private String title(Section card) {
        return find(H3.class, card).single().getText();
    }

    private String why(Section card) {
        return find(Paragraph.class, card).withClassName("verdict-why").single().getText();
    }

    /** The words of a component as a reader reads them, one space between parts. */
    private String text(Component component) {
        List<String> parts = new ArrayList<>();
        words(component.getElement(), parts);
        return String.join(" ", parts);
    }

    private static void words(com.vaadin.flow.dom.Element element, List<String> parts) {
        if (element.isTextNode()) {
            String text = element.getText().strip();
            if (!text.isEmpty()) {
                parts.add(text);
            }
            return;
        }
        element.getChildren().forEach(child -> words(child, parts));
    }

    /** Collected by walking the tree, because closed layers hide their content from a component query. */
    private static <T extends Component> void collect(Component from, Class<T> type, List<T> found) {
        if (type.isInstance(from)) {
            found.add(type.cast(from));
        }
        from.getChildren().forEach(child -> collect(child, type, found));
    }

    private <T extends Component> List<T> inLookup(Class<T> type, String className) {
        var found = new ArrayList<T>();
        collectStyled(find(Lookup.class).single(), type, className, found);
        return found;
    }

    private static <T extends Component> void collectStyled(Component from, Class<T> type, String className,
            List<T> found) {
        if (type.isInstance(from) && from instanceof HasStyle styled && styled.hasClassName(className)) {
            found.add(type.cast(from));
        }
        from.getChildren().forEach(child -> collectStyled(child, type, className, found));
    }

    @SuppressWarnings("unused")
    private static String path() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }
}
