package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * VER-01 to VER-09: the version screen answers from the four committed
 * version documents, 25.3.5 secure, 24.9.21 with a fix available, 24.4.28
 * unresolved with 25 findings stuck and 24.7.19 unresolved with one, and
 * from versions nobody measured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class VersionViewBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // VER-01
    @Test
    void the_secure_version_says_so_and_has_no_fixes_section() {
        open("25.3.5");

        assertThat(card().hasClassName("state-secure")).isTrue();
        assertThat(texts(card(), Span.class)).contains("Secure");
        assertThat(cardTitle()).isEqualTo("Vaadin 25.3.5 has nothing open");
        // It says explicitly that it counts findings judged not to apply.
        assertThat(cardWhy()).startsWith(
                "Every finding on this release is either fixed or judged not to affect a Vaadin application.")
                .contains("Spring Boot findings are listed separately below.");
        assertThat(slot("version-fixes").getComponentCount()).isZero();
        assertThat(slot("version-nofix").getComponentCount()).isZero();
        assertThat(slot("version-triage").getComponentCount()).isZero();
        assertThat(find(Button.class).withClassName("what-it-fixes").exists()).isFalse();
        assertThat(find(Button.class).withClassName("notify-me").exists()).isFalse();
        assertThat(cardText()).contains("Scanned 2026-11-21");
    }

    // VER-02
    @Test
    void the_fix_available_version_names_the_upgrade_and_lists_what_it_fixes() {
        open("24.9.21");

        assertThat(card().hasClassName("state-fix_available")).isTrue();
        assertThat(cardTitle()).isEqualTo("Upgrade to 24.9.26");
        assertThat(cardWhy()).isEqualTo("Vaadin 24.9.21 is affected by 12 findings. All of them are gone in 24.9.26,"
                + " a maintenance release in the minor you already use.");
        assertThat(texts(card(), Badge.class)).contains("Maintenance release", "Same minor: 24.9",
                "Extended maintenance", "Free maintenance ended 2026-06-16");
        var notes = find(Anchor.class, card()).withClassName("verdict-action").single();
        assertThat(notes.getText()).isEqualTo("View 24.9.26 release notes");
        assertThat(notes.getHref()).isEqualTo("https://github.com/vaadin/platform/releases/tag/24.9.26");

        // The rows are the fixture's fixable list, worst first, each with its answer.
        List<RouterLink> rows = find(RouterLink.class, slot("version-fixes")).withClassName("fix-row").all();
        assertThat(rows).extracting(row -> row.getElement().getAttribute("data-advisory")).containsExactly(
                "CVE-2026-102276", "CVE-2026-102278", "CVE-2026-102554", "CVE-2026-118250", "CVE-2026-118251",
                "CVE-2026-89407", "CVE-2026-93749", "CVE-2026-102277", "CVE-2026-66010", "CVE-2026-75838",
                "CVE-2026-109344", "CVE-2026-109345");
        assertThat(texts(rows.get(0), Span.class)).contains("CVE-2026-102276", "High");
        assertThat(texts(rows.get(0), Badge.class)).contains("Fixed in 24.9.22");
        assertThat(texts(rows.get(3), Badge.class)).contains("Gone by 24.9.26");
        assertThat(headings(slot("version-fixes"))).containsExactly("What 24.9.26 fixes for you");
        assertThat(texts(slot("version-fixes"), Span.class)).contains("12 of the 12 findings on 24.9.21");

        // Eleven advisories nobody has judged are said out loud, all of them named under the cap of twelve.
        assertThat(texts(slot("version-triage"), Paragraph.class).get(0))
                .startsWith("11 advisories on this release are still being reviewed.");
        assertThat(find(RouterLink.class, slot("version-triage")).all()).hasSize(11);
        // Named as the prototype names them, separated by commas.
        assertThat(find(Paragraph.class, slot("version-triage")).withClassName("triage-ids").single().getElement()
                .getTextRecursively()).startsWith("CVE-2026-102276, CVE-2026-102277, ");
        // Each fix row ends with the decorative chevron.
        assertThat(rows).allSatisfy(row -> assertThat(row.getChildren()
                .filter(child -> child.getElement().getClassList().contains("fix-chevron")).count()).isOne());
    }

    // VER-11: the prototype's distribution, one H1 from the shell, the answer's title as H2, everything under it H3.
    @Test
    void the_page_reads_in_heading_order() {
        open("24.7.19");

        assertThat(find(H1.class).all()).extracting(H1::getText).containsExactly("Security status");
        assertThat(find(H2.class).all()).extracting(H2::getText).containsExactly("Vaadin 24.7.19");
        assertThat(cardTitle()).isEqualTo("1 vulnerability has no fix yet");
        assertThat(find(H3.class).all()).extracting(H3::getText).contains("1 vulnerability has no fix yet",
                "Details for CVE-2026-45292", "Findings we looked at and ruled out", "All supported versions");
        // A section heading never jumps back above the answer's title.
        assertThat(find(H2.class, find(Div.class).withClassName("version-body").single()).exists()).isFalse();
    }

    // VER-12: the managed property of the Spring Boot tip and the discontinued product are set in code type.
    @Test
    void the_tips_set_their_property_and_product_in_code_type() {
        open("24.7.19?boot=1&disc=1");

        Paragraph bootTip = find(Paragraph.class, slot("version-boot")).withClassName("section-tip").first();
        assertThat(find(Code.class, bootTip).all()).extracting(Code::getText).containsExactly("<tomcat.version>");
        assertThat(bootTip.getElement().getTextRecursively())
                .startsWith("Not managed by Vaadin. Upgrade Spring Boot, or override the managed version in your"
                        + " build, for example <tomcat.version>.");

        Paragraph discTip = find(Paragraph.class, slot("version-disc")).withClassName("section-tip").single();
        assertThat(find(Code.class, discTip).all()).extracting(Code::getText).isNotEmpty()
                .allSatisfy(name -> assertThat(name).isNotBlank().doesNotContain(","));
        Table disc = find(Table.class, slot("version-disc")).single();
        assertThat(disc.getBodyRows()).isNotEmpty().allSatisfy(row -> assertThat(row.getElement().getTextRecursively())
                .endsWith("Override the dependency version in your own build; the advisory's page says the measured one"));
    }

    // VER-03
    @Test
    void two_or_more_stuck_findings_are_a_list_of_cards_with_their_layers() {
        open("24.4.28");

        assertThat(card().hasClassName("state-unresolved")).isTrue();
        assertThat(cardTitle()).isEqualTo("25 vulnerabilities have no fix yet");
        assertThat(cardWhy()).contains("Vaadin 24.4.28 is the newest release on its line")
                .contains("What you can do today is below, finding by finding.");
        assertThat(find(FindingCard.class, card()).exists()).isFalse();

        List<FindingCard> cards = find(FindingCard.class, slot("version-nofix")).all();
        assertThat(cards).hasSize(25);
        assertThat(headings(slot("version-nofix"))).containsExactly("Findings with no fix yet");
        FindingCard first = cards.get(0);
        assertThat(find(Details.class, first).all()).extracting(Details::getSummaryText).containsExactly(
                "What this means for you", "Affected modules and the fix", "Full advisory and technical evidence");
        // The first card is the one the screen is about, its first layers open; the full record stays folded.
        assertThat(find(Details.class, first).all()).extracting(Details::isOpened).containsExactly(true, true,
                false);
        assertThat(find(Details.class, cards.get(1)).all().get(0).isOpened()).isFalse();
        assertThat(find(H4.class, first).all()).extracting(H4::getText).contains("Do this now", "Fix",
                "Can your deployed application be exploited?");
        assertThat(find(Markdown.class, first).all()).allSatisfy(text -> assertThat(text.getContent()).isNotBlank());

        // No details section at the foot: that is for a single finding.
        assertThat(slot("version-details").getComponentCount()).isZero();
        assertThat(find(Button.class).withClassName("notify-me").exists()).isTrue();
    }

    // VER-04
    @Test
    void one_stuck_finding_is_drawn_inside_the_card_with_its_details_at_the_foot() {
        open("24.7.19");

        assertThat(cardTitle()).isEqualTo("1 vulnerability has no fix yet");
        assertThat(cardWhy()).startsWith("A workaround is available until a fix ships.")
                .doesNotContain("finding by finding");
        FindingCard inside = find(FindingCard.class, card()).single();
        assertThat(inside.advisory()).isEqualTo("CVE-2026-45292");
        assertThat(texts(inside, Badge.class)).contains("Vulnerable, workaround available");
        assertThat(find(Details.class, inside).exists()).isFalse();
        assertThat(slot("version-nofix").getComponentCount()).isZero();

        assertThat(headings(slot("version-details"))).containsExactly("Details for CVE-2026-45292");
        assertThat(find(Details.class, slot("version-details")).all()).isNotEmpty();
    }

    // AC1: the secure move never leaves the major and names the minor when it leaves the line
    @Test
    void the_secure_move_stays_in_the_major_and_names_the_minor_it_leaves() {
        open("24.7.19");

        RouterLink move = find(RouterLink.class, card()).withClassName("secure-move").single();
        assertThat(move.getHref()).startsWith("version/24.");
        assertThat(move.getText()).isEqualTo("Or move to " + move.getHref().substring("version/".length())
                + ", which is secure, since no 24.7 release is");
    }

    // AC1: Notify me opens the not built dialog
    @Test
    void notify_me_opens_the_not_built_dialog() {
        open("24.4.28");

        test(find(Button.class).withClassName("notify-me").single()).click();

        Dialog dialog = find(Dialog.class).all().stream().filter(Dialog::isOpened).findFirst().orElseThrow();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Not built yet");
    }

    // VER-05
    @Test
    void the_spring_boot_link_switches_the_shape_writes_the_address_and_redraws_only_that_section() {
        open("24.9.21");
        Component cardBefore = card();
        Component firstFix = find(RouterLink.class, slot("version-fixes")).withClassName("fix-row").first();
        assertThat(find(Table.class, slot("version-boot")).exists()).isFalse();

        Anchor show = find(Anchor.class, slot("version-boot")).withClassName("section-link").single();
        assertThat(show.getText()).isEqualTo("See Spring Boot findings");
        assertThat(show.getHref()).isEqualTo("version/24.9.21?boot=1");
        clickInPlace(show);

        assertAddress("version/24.9.21?boot=1");
        assertThat(headings(slot("version-boot"))).containsExactly("Spring Boot findings");
        Table table = find(Table.class, slot("version-boot")).single();
        assertThat(table.getBodyRows()).hasSize(12);
        assertThat(texts(slot("version-boot"), Paragraph.class).get(0)).contains(
                "This release declares Spring Boot 3.5.15.",
                "Free patches of the Spring Boot 3.5 line have ended: 3.5.16 is the last release anybody can"
                        + " download, and later ones, 3.5.18 today, exist only under Broadcom's commercial Spring"
                        + " support.");
        // The rest of the answer is the same components: nothing was redrawn, so nothing moved.
        assertThat(card()).isSameAs(cardBefore);
        assertThat(find(RouterLink.class, slot("version-fixes")).withClassName("fix-row").first()).isSameAs(firstFix);

        Anchor hide = find(Anchor.class, slot("version-boot")).withClassName("section-link").single();
        assertThat(hide.getText()).isEqualTo("Hide them");
        clickInPlace(hide);
        assertAddress("version/24.9.21?boot=0");
        assertThat(find(Table.class, slot("version-boot")).exists()).isFalse();
    }

    // AC3: a pasted address with flags opens with those sections open
    @Test
    void a_pasted_address_opens_with_its_sections_open() {
        navigate("version/24.9.21?boot=1&ruled=1&disc=1", VersionView.class);

        assertThat(find(Table.class, slot("version-boot")).exists()).isTrue();
        assertThat(find(Table.class, slot("version-ruled")).single().getBodyRows()).hasSize(77);
        assertThat(find(Table.class, slot("version-disc")).single().getBodyRows()).hasSize(4);
    }

    // Rule 4: a ruled out row whose advisory carries no applicability has no reason cell at all
    @Test
    void a_ruled_out_row_without_a_reason_has_no_reason_cell() {
        String index = fixtures.transport().fixture("cases/index.json");
        String entry = "\"CVE-2021-33604\":{\"cve\":\"CVE-2021-33604\",\"severity\":\"low\",\"applicability\":\"false_positive\",";
        assertThat(index).contains(entry);
        fixtures.transport().replace("cases/index.json",
                index.replace(entry, "\"CVE-2021-33604\":{\"cve\":\"CVE-2021-33604\",\"severity\":\"low\","));
        fixtures.revalidate();

        navigate(HomeView.class);
        navigate("version/25.3.5?ruled=1", VersionView.class);

        Table table = find(Table.class, slot("version-ruled")).single();
        TableRow bare = table.getBodyRows().stream()
                .filter(row -> "CVE-2021-33604".equals(row.getElement().getAttribute("data-advisory"))).findFirst()
                .orElseThrow();
        assertThat(bare.getCells()).hasSize(1);
        assertThat(bare.getCells().get(0).getColspan()).isEqualTo(2);
        // The positive control: a row whose advisory carries a verdict keeps its reason beside it.
        TableRow said = table.getBodyRows().stream().filter(row -> row != bare).findFirst().orElseThrow();
        assertThat(said.getCells()).hasSize(2);
        assertThat(said.getCells().get(1).getElement().getText()).isNotBlank();
    }

    // AC3: toggling ruled and disc rewrites the query and keeps the other flags
    @Test
    void the_ruled_out_and_discontinued_controls_write_their_flags() {
        navigate("version/24.9.21?boot=1", VersionView.class);
        Table springTable = find(Table.class, slot("version-boot")).single();

        clickInPlace(find(Anchor.class, slot("version-ruled")).withClassName("section-link").single());
        assertAddress("version/24.9.21?boot=1&ruled=1");
        clickInPlace(find(Anchor.class, slot("version-disc")).withClassName("section-link").single());
        assertAddress("version/24.9.21?boot=1&ruled=1&disc=1");
        // Only the section whose flag moved is drawn again.
        assertThat(find(Table.class, slot("version-boot")).single()).isSameAs(springTable);

        clickInPlace(find(Anchor.class, slot("version-ruled")).withClassName("section-link").single());
        assertAddress("version/24.9.21?boot=1&disc=1");
        assertThat(find(Table.class, slot("version-ruled")).exists()).isFalse();
    }

    // VER-06
    @Test
    void an_open_address_opens_that_finding_and_marks_it() {
        navigate("version/24.4.28?open=CVE-2026-59314", VersionView.class);
        roundTrip();

        List<FindingCard> cards = find(FindingCard.class, slot("version-nofix")).all();
        FindingCard named = cards.stream().filter(card -> card.advisory().equals("CVE-2026-59314")).findFirst()
                .orElseThrow();
        assertThat(find(Details.class, named).all().get(0).isOpened()).isTrue();
        assertThat(named.getElement().getClassList()).contains(VersionView.REVEALED);
        // The finding the screen would otherwise open is left closed for the one the link names.
        assertThat(find(Details.class, cards.get(0)).all().get(0).isOpened()).isFalse();
    }

    // Edge case: ?open naming a finding not on this version is ignored
    @Test
    void an_open_address_naming_another_finding_is_ignored() {
        navigate("version/24.4.28?open=CVE-1999-0001", VersionView.class);
        roundTrip();

        List<FindingCard> cards = find(FindingCard.class, slot("version-nofix")).all();
        assertThat(cards).noneMatch(card -> card.getElement().getClassList().contains(VersionView.REVEALED));
        assertThat(find(Details.class, cards.get(0)).all().get(0).isOpened()).isTrue();
    }

    // AC3: opening a finding writes it into the address
    @Test
    void opening_a_finding_writes_open_into_the_address() {
        open("24.4.28");
        FindingCard second = find(FindingCard.class, slot("version-nofix")).all().get(1);

        test(find(Details.class, second).all().get(0)).openDetails();

        assertAddress("version/24.4.28?open=" + second.advisory());
        // Nothing was drawn again, and the link the reader made is not marked as if it had arrived.
        assertThat(find(FindingCard.class, slot("version-nofix")).all().get(1)).isSameAs(second);
        assertThat(second.getElement().getClassList()).doesNotContain(VersionView.REVEALED);
    }

    // VER-07
    @Test
    void an_unmeasured_version_is_answered_in_place_with_the_nearest_measured_release() {
        open("24.9.99");

        assertAddress("version/24.9.99");
        assertThat(card().hasClassName("state-unmeasured")).isTrue();
        assertThat(cardTitle()).isEqualTo("We have not measured 24.9.99");
        assertThat(cardWhy()).contains("the answer for 24.9.26, the newest release of 24.9");
        RouterLink nearest = find(RouterLink.class, card()).withClassName("nearest-measured").single();
        assertThat(nearest.getText()).isEqualTo("See 24.9.26 instead");
        assertThat(nearest.getHref()).isEqualTo("version/24.9.26");
        assertThat(find(Table.class, slot("version-lines")).exists()).isTrue();
    }

    // VER-07, a minor nobody follows and a version below every followed line
    @Test
    void an_unfollowed_minor_gets_the_newest_of_its_major_and_an_old_one_the_oldest_measured() {
        open("24.8.3");
        assertThat(cardWhy()).contains("24.8 is not one of them. The newest release of Vaadin 24 we do measure is"
                + " 24.10.14");

        open("6.0.1");
        RouterLink oldest = find(RouterLink.class, card()).withClassName("nearest-measured").single();
        assertThat(oldest.getHref()).startsWith("version/7.");
        assertThat(cardWhy()).contains("which is not the same as it being safe");
    }

    // VER-08 and AC2: absent verdict fields are omitted, not rendered empty
    @Test
    void an_absent_workaround_leaves_no_workaround_row() {
        open("24.4.28");

        // Two verdicts answer CVE-2025-31672 here and only the second carries a workaround and a fix.
        FindingCard card = find(FindingCard.class, slot("version-nofix")).all().stream()
                .filter(one -> one.advisory().equals("CVE-2025-31672")).findFirst().orElseThrow();
        assertThat(find(H4.class, card).all()).extracting(H4::getText).filteredOn("Do this now"::equals).hasSize(1);
        assertThat(find(Markdown.class, card).all()).allSatisfy(text -> assertThat(text.getContent()).isNotBlank());
        assertThat(find(Paragraph.class, card).all()).allSatisfy(
                paragraph -> assertThat(paragraph.getText() + paragraph.getComponentCount()).isNotEqualTo("0"));
    }

    // VER-09
    @Test
    void the_versions_table_marks_the_readers_line() {
        open("24.9.21");

        Table table = find(Table.class, slot("version-lines")).single();
        List<TableRow> rows = table.getBodyRows();
        assertThat(rows).extracting(row -> row.getCells().get(0).getElement().getText()).containsExactly("25.3",
                "24.10", "24.9 · yours", "24.7", "24.4", "23.6", "14.14", "8.32", "7.7");
        assertThat(rows.stream().filter(row -> row.hasClassName("yours")).toList()).hasSize(1);
        assertThat(rows.get(2).hasClassName("yours")).isTrue();
        assertThat(headings(slot("version-lines"))).containsExactly("All supported versions");
    }

    // AC2: the caps count their remainders
    @Test
    void the_spring_boot_table_is_capped_and_counts_the_rest() {
        open("24.4.28");

        // Something of ours is stuck, so the table is the shape this state draws.
        Table table = find(Table.class, slot("version-boot")).single();
        assertThat(table.getBodyRows()).hasSize(VersionRules.BOOT_ROWS);
        assertThat(texts(slot("version-boot"), Paragraph.class)).contains(
                "64 more arrive the same way. The ones listed are the ones we judged a real risk.");
    }

    // AC2: every section renders only when its list is non empty
    @Test
    void sections_with_nothing_in_them_are_not_drawn() {
        open("25.3.5");

        assertThat(slot("version-disc").getComponentCount()).isZero();
        assertThat(slot("version-details").getComponentCount()).isZero();
        assertThat(slot("version-ruled").getComponentCount()).isPositive();
    }

    @Test
    void with_the_data_out_of_reach_the_screen_says_so_and_invents_nothing() {
        fixtures.coldAndDown();

        open("24.9.21");

        assertThat(find(DataOutageNotice.class).exists()).isTrue();
        assertThat(find(Section.class).withClassName("verdict-card").exists()).isFalse();
        assertThat(slot("version-lines").getComponentCount()).isZero();
    }

    // Helpers

    /** Leaves for home first, so a second open in one test is a new question as it would be in a browser. */
    private void open(String version) {
        navigate(HomeView.class);
        navigate("version/" + version, VersionView.class);
    }

    /** A plain click on a section link: the listener the page answers in place, as the browser sends it. */
    private void clickInPlace(Anchor link) {
        // The tester's own fireDomEvent is protected, so the event goes in the way the client's would.
        // The browser half, the filter that lets a modified click be a link, is checked in a browser: here
        // the event arrives with that filter answered true, as the browser would send it.
        var data = JacksonUtils.createObjectNode();
        data.put("@" + VersionView.CLICKS.plainClick().getSnippetKey(), true);
        link.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(link.getElement(), "click", data));
    }

    /** The address the window is on, the query compared as a set because its order is not ours to keep. */
    private static void assertAddress(String expected) {
        var location = UI.getCurrent().getInternals().getActiveViewLocation();
        var wanted = new com.vaadin.flow.router.Location(expected);
        assertThat(location.getPath()).isEqualTo(wanted.getPath());
        assertThat(location.getQueryParameters().getParameters())
                .isEqualTo(wanted.getQueryParameters().getParameters());
    }

    private Div slot(String className) {
        return find(Div.class).withClassName(className).single();
    }

    private Section card() {
        return find(Section.class).withClassName("verdict-card").single();
    }

    private String cardTitle() {
        return find(H3.class, card()).first().getText();
    }

    private String cardWhy() {
        return find(Paragraph.class, card()).withClassName("verdict-why").single().getText();
    }

    private String cardText() {
        return String.join(" ", texts(card(), Span.class));
    }

    private List<String> headings(Component within) {
        return find(H3.class, within).all().stream().map(H3::getText).toList();
    }

    private <T extends Component> List<String> texts(Component within, Class<T> type) {
        return find(type, within).all().stream().map(component -> component.getElement().getTextRecursively())
                .filter(Objects::nonNull).filter(text -> !text.isEmpty()).toList();
    }
}
