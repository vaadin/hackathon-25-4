package com.vaadin.securitystatus.ui.team.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.util.FileSystemUtils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.browserless.internal.ShortcutsKt;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.LabelTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeForm;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.workbench.QueueRules.Row;
import com.vaadin.securitystatus.team.workbench.VerdictDraft;
import com.vaadin.securitystatus.team.workbench.VerdictDrafts;
import com.vaadin.securitystatus.team.workbench.VerdictYamlTest;
import com.vaadin.securitystatus.ui.customer.HomeView;

/**
 * WRK-01, WRK-02, WRK-03 and WRK-06: the queue in the fixture's order with its
 * numbers, a row opening the form prefilled, a half draft that saves and does
 * not export, and a draft that is still there after a restart.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class WorkbenchBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    VerdictDrafts drafts;

    // WRK-01
    @Test
    void the_queue_is_in_the_fixture_order_with_its_numbers() {
        WorkbenchView view = navigate(WorkbenchView.class);

        List<Row> rows = rows(view);
        assertThat(rows).hasSize(101);
        assertThat(rows.subList(0, 5)).extracting(Row::name).containsExactly("com.fasterxml.jackson.core:jackson-core",
                "com.fasterxml.jackson.core:jackson-databind", "highcharts", "org.apache.commons:commons-lang3",
                "org.springframework:spring-core");
        assertThat(rows.getLast().name()).isEqualTo("webpack-dev-server");
        assertThat(rows).extracting(Row::closes).containsOnly(0);
        assertThat(rows.get(4).here()).hasSize(44);
        assertThat(text("workbench-lead"))
                .isEqualTo("Nothing on the heads of the series we follow is waiting for a verdict. "
                        + "The 101 packages below are all answered.");
        // The prototype's row: what it would close, the package, its state and what it holds.
        assertThat(view.row("org.springframework:spring-core").getElement().getTextRecursively()).isEqualTo(
                "0org.springframework:spring-coreanswered44 advisories on 8 series, 24.10, 24.9, 24.7, 24.4, 23.6, "
                        + "14.14, 8.32, 7.7. Spring Boot owns the version, so a bump is not one of the answers here");
        assertThat(text("workbench-hint")).startsWith("A verdict is keyed by a package with no version");
        assertThat(text("workbench-summary-total")).isEqualTo("721 findings over 101 packages, 0 of them with no verdict.");
        assertThat(summary("workbench-summary-severity")).containsExactly("Critical 76 0", "High 308 0",
                "Moderate 289 0", "Low 48 0");
        assertThat(summary("workbench-summary-line")).first().asString().startsWith("25.4 ");
    }

    // WRK-01 and AC1: the series filter narrows and lives in the address
    @Test
    void the_series_filter_narrows_the_queue_and_lives_in_the_address() {
        WorkbenchView view = navigate(WorkbenchView.class);
        RouterLink link = $(RouterLink.class).withClassName("workbench-series-link").all().stream()
                .filter(candidate -> "24.9".equals(candidate.getElement().getAttribute("data-series"))).findFirst()
                .orElseThrow();
        assertThat(link.getHref()).isEqualTo("findings/workbench?series=24.9");
        assertThat(link.getElement().getTextRecursively()).isEqualTo("24.9 0");
        assertThat(Tooltip.forComponent(link).getText()).isEqualTo("0 advisories to judge on 24.9");

        view = navigate("findings/workbench?series=24.9", WorkbenchView.class);

        List<Row> rows = rows(view);
        assertThat(rows).hasSize(38);
        assertThat(rows).allMatch(row -> row.entry().lines().contains("24.9"));
        assertThat(rows.stream().mapToInt(row -> row.here().size()).sum()).isEqualTo(214);
        assertThat(rows.getFirst().name()).isEqualTo("com.fasterxml.jackson.core:jackson-core");
        assertThat(rows.getFirst().here()).hasSize(2);
        assertThat(rows.getFirst().elsewhere()).isEqualTo(3);
        assertThat(text("workbench-lead")).startsWith("Nothing on the head of 24.9 is waiting for a verdict.");
        assertThat(summary("workbench-summary-line")).containsExactly("24.9 214 0");
        assertThat($(RouterLink.class).withClassName("active").single().getElement().getTextRecursively())
                .isEqualTo("24.9 0");

        // A series the queue does not know narrows nothing.
        view = navigate("findings/workbench?series=99.9", WorkbenchView.class);
        assertThat(rows(view)).hasSize(101);
    }

    // WRK-02
    @Test
    void a_selected_row_prefills_the_package_and_the_advisories() {
        WorkbenchView view = navigate("findings/workbench?series=24.9", WorkbenchView.class);

        select(view, "com.fasterxml.jackson.core:jackson-core");

        Map<String, HasValue<?, ?>> fields = view.form().fields();
        assertThat(fields.get("purl").getValue()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-core");
        // Every advisory of the package is answered, so the new verdict starts from all of them.
        assertThat(fields.get("advisories").getValue())
                .isEqualTo("CVE-2025-52999, CVE-2026-18401, CVE-2026-68494, CVE-2026-89407, CVE-2026-89425");
        assertThat(fields.get("verdictId").getValue())
                .isEqualTo("maven-com-fasterxml-jackson-core-jackson-core-24-9-2026-11-21");
        assertThat($(com.vaadin.flow.component.html.H2.class).withClassName("workbench-composing").single().getText())
                .isEqualTo("A verdict on com.fasterxml.jackson.core:jackson-core");
        assertThat(text("workbench-elsewhere")).startsWith("You are looking at 24.9, and 3 advisories of this package");
        assertThat(text("workbench-reach")).startsWith("This closes 5 advisories on 8 series, "
                + "24.10, 24.9, 24.7, 24.4, 23.6, 14.14, 8.32, 7.7, which is 25 findings off the page; 0 of them carry "
                + "no verdict now.");
        assertThat(yaml(view)).contains("purl: pkg:maven/com.fasterxml.jackson.core/jackson-core\ncves:\n"
                + "- CVE-2025-52999\n");
    }

    // D-220, D-221: a row opened shows the evidence the queue carries, and the form follows it
    @Test
    void an_opened_row_shows_the_evidence_and_holds_the_form() {
        WorkbenchView view = navigate(WorkbenchView.class);

        view.open("org.apache.tomcat.embed:tomcat-embed-core");

        com.vaadin.flow.component.details.Details row = view.row("org.apache.tomcat.embed:tomcat-embed-core");
        assertThat(row.isOpened()).isTrue();
        String evidence = $(com.vaadin.flow.component.html.Div.class, row).withClassName("workbench-evidence").single()
                .getElement().getTextRecursively();
        assertThat(evidence).contains("Resolves to", "Arrives by", "declared by spring-boot-starter-tomcat",
                "CPE matched", "netapp:ontap_tools:vmware_vsphere",
                "Reported by bomber, dependency-check, osv-scanner.", "Of 61 advisories here, upstream has fixed");
        assertThat(text("workbench-acknowledged")).endsWith("We could not read the list of 8.32, 7.7, so nothing "
                + "is claimed about them.");
        // The advisories as ticks, each with what is known of it, and the verdicts the package carries.
        List<com.vaadin.flow.component.checkbox.Checkbox> ticks = $(com.vaadin.flow.component.checkbox.Checkbox.class,
                row).withClassName("workbench-pick-box").all();
        assertThat(ticks).hasSize(61);
        assertThat($(Button.class, row).withClassName("workbench-reuse-chip").all()).hasSize(17);
        assertThat($(Button.class, row).withClassName("workbench-save").exists()).isTrue();

        // One row at a time: opening another moves the form there.
        view.open("highcharts");
        assertThat(row.isOpened()).isFalse();
        assertThat($(Button.class, view.row("highcharts")).withClassName("workbench-save").exists()).isTrue();
        assertThat(text("workbench-acknowledged")).startsWith("Already in the check list of 25.4 (5 of 7), 25.3 (5 of 7)");
        assertThat($(com.vaadin.flow.component.html.Div.class, view.row("highcharts"))
                .withClassName("workbench-evidence").single().getElement().getTextRecursively())
                .contains("Frontend. Compare the upstream fix date");
    }

    // D-221: a tick writes the advisories field, and the field moves the ticks
    @Test
    void the_ticks_write_the_advisories_and_follow_them() {
        WorkbenchView view = navigate(WorkbenchView.class);
        view.open("highcharts");
        List<com.vaadin.flow.component.checkbox.Checkbox> ticks = $(com.vaadin.flow.component.checkbox.Checkbox.class)
                .withClassName("workbench-pick-box").all();
        String first = ticks.getFirst().getLabel();
        assertThat(ticks).allMatch(com.vaadin.flow.component.checkbox.Checkbox::getValue);

        test(ticks.getFirst()).click();

        assertThat(view.form().fields().get("advisories").getValue()).asString().doesNotContain(first);
        type(view, "advisories", first);
        assertThat(ticks.getFirst().getValue()).isTrue();
        assertThat(ticks.get(1).getValue()).isFalse();
    }

    // D-222: a package that carries one verdict arrives filled from it, superseding it
    @Test
    void a_package_with_one_verdict_arrives_filled_from_it() {
        WorkbenchView view = navigate(WorkbenchView.class);

        view.open("com.vaadin:vaadin-swing-kit-flow");

        Map<String, HasValue<?, ?>> fields = view.form().fields();
        assertThat(fields.get("supersedes").getValue()).isEqualTo("maven-com-vaadin-vaadin-swing-kit-flow");
        assertThat(fields.get("depth").getValue()).isEqualTo("cursory");
        assertThat(fields.get("applicability").getValue()).isNotNull();
        assertThat((String) fields.get("commentary").getValue()).isNotBlank();
        assertThat(text("workbench-filled")).startsWith("Filled from maven-com-vaadin-vaadin-swing-kit-flow, which "
                + "answers 1 advisory here. Composing now writes a new file that supersedes it");
        assertThat(yaml(view)).contains("supersedes: maven-com-vaadin-vaadin-swing-kit-flow");
    }

    // D-278: a package that carries several verdicts arrives filled from the newest, on a series from the newest speaking there
    @Test
    void a_package_with_several_verdicts_arrives_filled_from_the_newest() {
        WorkbenchView view = navigate(WorkbenchView.class);

        view.open("org.apache.commons:commons-lang3");

        assertThat(view.form().fields().get("supersedes").getValue())
                .isEqualTo("maven-org-apache-commons-commons-lang3-7-8-build");
        assertThat(view.form().fields().get("depth").getValue()).isEqualTo("cursory");
        assertThat(text("workbench-filled")).startsWith("Filled from maven-org-apache-commons-commons-lang3-7-8-build");

        // The 7 and 8 verdict is the newest, but on 24.9 it is the 24.9 one that speaks.
        view = navigate("findings/workbench?series=24.9", WorkbenchView.class);
        view.open("org.apache.commons:commons-lang3");

        assertThat(view.form().fields().get("supersedes").getValue())
                .isEqualTo("maven-org-apache-commons-commons-lang3-classutils-recursion-24-9-up");
    }

    // AC2: the vocabulary is the closed set and nothing else is offered
    @Test
    void the_applicability_offers_the_closed_vocabulary_only() {
        WorkbenchView view = navigate(WorkbenchView.class);

        @SuppressWarnings("unchecked")
        Select<String> applicability = (Select<String>) view.form().fields().get("applicability");
        assertThat(test(applicability).getSuggestionItems()).containsExactly("risk", "not_affected",
                "configuration", "no_risk", "false_positive", "fixed");
        @SuppressWarnings("unchecked")
        Select<String> depth = (Select<String>) view.form().fields().get("depth");
        assertThat(test(depth).getSuggestionItems()).containsExactly("cursory", "reproduced", "committee",
                "automatic");
    }

    // The 25.4 side labels
    @Test
    void the_labels_sit_beside_the_fields_aligned_to_their_end() {
        WorkbenchView view = navigate(WorkbenchView.class);

        FormLayout layout = view.form().layout();
        assertThat(layout.isLabelsAside()).isTrue();
        assertThat(layout.getLabelTextAlign()).isEqualTo(LabelTextAlign.END);
        assertThat(layout.getClassNames()).contains("verdict-form");
    }

    // A save the formats refuse says so and names the field, instead of doing nothing
    @Test
    void a_draft_in_the_wrong_form_is_not_saved_and_the_notice_names_the_field() {
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrong-form");
        type(view, "affects", "all of 24");

        click("workbench-save");

        assertThat(drafts.list()).noneMatch(draft -> draft.getPurl().equals("pkg:npm/wrong-form"));
        assertThat(test($(Notification.class).last()).getText())
                .contains("Not saved: Which Vaadin versions it applies to is not in the expected form.");
    }

    // WRK-03
    @Test
    void a_half_draft_saves_and_its_export_is_refused_naming_the_fields() {
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrk-03");
        type(view, "commentary", "Half a thought.");

        click("workbench-save");

        assertThat(drafts.list()).anyMatch(draft -> draft.getPurl().equals("pkg:npm/wrk-03")
                && draft.getCommentary().equals("Half a thought.") && draft.getSavedBy().equals("team"));
        assertThat(text("workbench-saved")).isEqualTo("Last saved by team, 2026-11-21 12:00 UTC.");

        click("workbench-export");

        assertThat(text("workbench-refused"))
                .isEqualTo("Not exported: Verdict id, Advisories, What we are saying still need an answer.");
        assertThat(((TextField) view.form().fields().get("verdictId")).isInvalid()).isTrue();
        assertThat(((TextField) view.form().fields().get("verdictId")).getErrorMessage())
                .isEqualTo("Required to export");
        assertThat(github().isEnabled()).isFalse();
        assertThat(input("filename").getValue()).isEmpty();
        assertThat(input("value").getValue()).isEmpty();
    }

    @Test
    void enter_in_a_one_line_field_saves_as_the_button_does() {
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrk-enter");

        ShortcutsKt._fireShortcut((Component) view.form().fields().get("purl"), Key.ENTER);

        assertThat(drafts.list()).anyMatch(draft -> draft.getPurl().equals("pkg:npm/wrk-enter"));
    }

    @Test
    void a_malformed_value_is_refused_even_as_a_draft() {
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrk-bad@1.0.0");
        int before = drafts.list().size();

        click("workbench-save");

        assertThat(drafts.list()).hasSize(before);
        assertThat(((TextField) view.form().fields().get("purl")).getErrorMessage())
                .isEqualTo("A purl without a version, starting with pkg:");
    }

    // The edge case: a risk with no impact prose is refused
    @Test
    void a_risk_without_an_impact_is_refused() {
        WorkbenchView view = navigate(WorkbenchView.class);
        fillReady(view, "wrk-risk");
        choose(view, "applicability", "risk");

        click("workbench-export");

        assertThat(text("workbench-refused"))
                .isEqualTo("Not exported: What it means for your application still needs an answer.");
        assertThat(((TextArea) view.form().fields().get("impact")).getErrorMessage())
                .isEqualTo("A risk or no risk verdict answers nothing without an impact sentence");

        type(view, "impact", "Everybody on 24.9 is exposed.");
        click("workbench-export");

        assertThat($(Paragraph.class).withClassName("workbench-refused").exists()).isFalse();
        assertThat(github().isEnabled()).isTrue();
    }

    // WRK-05 at the screen: the form the browser submits
    @Test
    void a_ready_draft_opens_the_github_form_with_the_path_and_the_yaml() {
        WorkbenchView view = navigate(WorkbenchView.class);
        fillReady(view, "wrk-05");

        click("workbench-export");

        NativeForm form = $(NativeForm.class).withClassName("workbench-handover").single();
        assertThat(form.getMethod()).isEqualTo(NativeForm.Method.GET);
        assertThat(form.getAction()).isEqualTo("https://github.com/vaadin/cve-status/new/main");
        assertThat(form.getElement().getAttribute("target")).isEqualTo("_blank");
        assertThat(github().isEnabled()).isTrue();
        assertThat(github().getElement().getAttribute("type")).isEqualTo("submit");
        assertThat(input("filename").getElement().getAttribute("name")).isEqualTo("filename");
        assertThat(input("filename").getValue()).isEqualTo("data/assessments/npm-wrk-05-2026-11-21.yaml");
        assertThat(input("value").getElement().getAttribute("name")).isEqualTo("value");
        assertThat(input("value").getValue()).isEqualTo(yaml(view)).contains("purl: pkg:npm/wrk-05\n");
        assertThat(text("workbench-path")).isEqualTo("It belongs at data/assessments/npm-wrk-05-2026-11-21.yaml. "
                + "The writer refuses to overwrite an existing file, which is how the rule against editing a verdict "
                + "in place is enforced.");

        // An edit withdraws the export: what is on screen is no longer what was approved.
        type(view, "commentary", "Changed after the export.");
        assertThat(github().isEnabled()).isFalse();
        assertThat(input("value").getValue()).isEmpty();
    }

    // AC4: above the cap the link disables with one sentence and the copy control remains
    @Test
    void a_yaml_too_long_for_a_link_disables_github_and_keeps_the_copy() {
        WorkbenchView view = navigate(WorkbenchView.class);
        fillReady(view, "wrk-long");
        type(view, "evidence", "Measured. ".repeat(800));

        click("workbench-export");

        assertThat(github().isEnabled()).isFalse();
        assertThat(text("workbench-too-long")).startsWith("Too long for a link: ")
                .endsWith("where browsers refuse past 7000. Copy the YAML into "
                        + "data/assessments/npm-wrk-long-2026-11-21.yaml instead.");
        Button copy = $(Button.class).withClassName("workbench-copy").single();
        assertThat(copy.isEnabled()).isTrue();
        assertThat(copy.isVisible()).isTrue();
        assertThat(input("yaml-source").getValue()).isEqualTo(yaml(view)).contains("Measured. Measured.");
    }

    // AC3 at the screen: the YAML follows the typing and absent fields have no key
    @Test
    void the_yaml_follows_the_form_and_omits_what_is_empty() {
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrk-live");

        assertThat(yaml(view)).isEqualTo("purl: pkg:npm/wrk-live\nassessed_at: '2026-11-21'\n");

        choose(view, "applicability", "not_affected");
        assertThat(yaml(view)).contains("applicability: not_affected\njustification: code_not_present\n")
                .doesNotContain("impact").doesNotContain("null");
        Markdown preview = $(Markdown.class).withClassName("workbench-yaml").single();
        assertThat(preview.getContent()).startsWith("```yaml\npurl: pkg:npm/wrk-live\n").endsWith("```");
    }

    // The silencing verdict names the platform branches that still go red
    @Test
    void a_silencing_verdict_names_the_platform_branches_still_red() {
        WorkbenchView view = navigate(WorkbenchView.class);
        select(view, "com.fasterxml.jackson.core:jackson-core");

        choose(view, "applicability", "risk");
        assertThat($(Paragraph.class).withClassName("workbench-platform").exists()).isFalse();
        assertThat(text("workbench-reach")).endsWith("It silences nothing: a risk is us saying the reader is exposed, "
                + "so it stays on the page and stays out of the check list.");

        choose(view, "applicability", "false_positive");
        assertThat(text("workbench-platform")).startsWith("This does not turn a build green on its own. "
                + "25.4, 25.3, 24.10, 24.9, 24.7, 24.4, 23.6, 14.14 would still go red");
    }

    // The edge case: two team members drafting the same pair, the last save wins and is named
    @Test
    void the_last_save_of_a_pair_wins_and_the_form_says_who() {
        VerdictDraft first = VerdictYamlTest.minimal();
        first.setPurl("pkg:npm/wrk-pair");
        first.setCommentary("The first word.");
        drafts.save(first);
        WorkbenchView view = navigate(WorkbenchView.class);
        type(view, "purl", "pkg:npm/wrk-pair");
        type(view, "advisories", "CVE-2026-116796");
        type(view, "commentary", "The last word.");

        click("workbench-save");

        List<VerdictDraft> pair = drafts.list().stream().filter(draft -> draft.getPurl().equals("pkg:npm/wrk-pair"))
                .toList();
        assertThat(pair).singleElement().extracting(VerdictDraft::getCommentary).isEqualTo("The last word.");
        assertThat(text("workbench-saved")).startsWith("Last saved by team");
    }

    // W4: the drafts are listed under the form and restored from there
    @Test
    void a_saved_draft_is_listed_and_restored_into_the_form() {
        VerdictDraft kept = VerdictYamlTest.full();
        kept.setPurl("pkg:maven/com.example/wrk-list");
        drafts.save(kept);

        WorkbenchView view = navigate(WorkbenchView.class);
        Grid<VerdictDraft> list = view.draftList();
        int index = items(list).indexOf(items(list).stream()
                .filter(draft -> draft.getPurl().equals("pkg:maven/com.example/wrk-list")).findFirst().orElseThrow());
        test(list).clickRow(index);

        assertThat(view.form().fields().get("purl").getValue()).isEqualTo("pkg:maven/com.example/wrk-list");
        assertThat(view.form().fields().get("commentary").getValue()).isEqualTo(kept.getCommentary());
        assertThat(view.form().fields().get("applicability").getValue()).isEqualTo("configuration");
        assertThat(text("workbench-saved")).isEqualTo("Last saved by team, 2026-11-21 12:00 UTC.");
    }

    @Test
    @WithMockUser(username = "guest", roles = {})
    void a_reader_without_the_role_is_refused() {
        navigate("findings/workbench", HomeView.class);

        assertThat($(com.vaadin.flow.component.html.Div.class).withClassName("workbench-queue").exists()).isFalse();
    }

    /**
     * WRK-06: a draft survives a restart. Each method of this class gets a
     * Spring context of its own over one H2 file, so the second method runs in
     * a new application against what the first one left on disk.
     */
    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
            properties = "spring.datasource.url=jdbc:h2:file:./target/wrk-06/drafts")
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AfterARestart extends SpringBrowserlessTest {

        /** The application of the first method, to prove the second runs in another one. */
        private static @org.jspecify.annotations.Nullable ApplicationContext before;

        @Autowired
        ApplicationContext application;

        @BeforeAll
        static void forgetEarlierRuns() throws Exception {
            FileSystemUtils.deleteRecursively(Path.of("target/wrk-06"));
        }

        @Test
        @Order(1)
        void a_draft_is_saved_before_the_restart() {
            before = application;
            WorkbenchView view = navigate(WorkbenchView.class);
            type(view, "purl", "pkg:npm/wrk-06");
            type(view, "advisories", "CVE-2026-116796");
            type(view, "commentary", "Written before the restart.");

            $(Button.class).withClassName("workbench-save").single().click();

            assertThat(view.draftList().getListDataView().getItems().map(VerdictDraft::getPurl))
                    .contains("pkg:npm/wrk-06");
        }

        @Test
        @Order(2)
        void the_draft_is_still_there_after_the_restart() {
            assertThat(application).isNotNull().isNotSameAs(before);
            assertThat(before).isNotNull();
            WorkbenchView view = navigate(WorkbenchView.class);

            List<VerdictDraft> listed = view.draftList().getListDataView().getItems().toList();
            assertThat(listed).extracting(VerdictDraft::getPurl).containsExactly("pkg:npm/wrk-06");
            test(view.draftList()).clickRow(0);
            assertThat(view.form().fields().get("commentary").getValue()).isEqualTo("Written before the restart.");
            assertThat(view.form().fields().get("advisories").getValue()).isEqualTo("CVE-2026-116796");
        }

        private void type(WorkbenchView view, String property, String text) {
            WorkbenchBrowserlessTest.typeInto(view, property, text);
        }
    }

    // Helpers

    private List<Row> rows(WorkbenchView view) {
        return view.rows();
    }

    private static List<VerdictDraft> items(Grid<VerdictDraft> grid) {
        return grid.getListDataView().getItems().toList();
    }

    private void select(WorkbenchView view, String name) {
        view.open(name);
    }

    private void type(WorkbenchView view, String property, String text) {
        typeInto(view, property, text);
    }

    static void typeInto(WorkbenchView view, String property, String text) {
        @SuppressWarnings("unchecked")
        HasValue<?, String> field = (HasValue<?, String>) view.form().fields().get(property);
        field.setValue(text);
    }

    private void choose(WorkbenchView view, String property, String word) {
        @SuppressWarnings("unchecked")
        Select<String> select = (Select<String>) view.form().fields().get(property);
        test(select).selectItem(select.getItemLabelGenerator().apply(word));
    }

    private void fillReady(WorkbenchView view, String name) {
        type(view, "verdictId", "npm-" + name + "-2026-11-21");
        type(view, "purl", "pkg:npm/" + name);
        type(view, "advisories", "CVE-2026-116796");
        choose(view, "applicability", "fixed");
        type(view, "commentary", "The version we resolve carries the fix.");
    }

    private void click(String className) {
        test($(Button.class).withClassName(className).single()).click();
    }

    private String text(String className) {
        Paragraph paragraph = $(Paragraph.class).withClassName(className).single();
        assertThat(paragraph.isVisible()).as(className + " is shown").isTrue();
        return paragraph.getText();
    }

    private List<String> summary(String className) {
        return $(TableRow.class, $(com.vaadin.flow.component.html.Table.class).withClassName(className).single())
                .all().stream().filter(row -> row.getElement().hasAttribute("data-key"))
                .map(row -> String.join(" ", row.getChildren().map(cell -> cell.getElement().getTextRecursively())
                        .toList()))
                .toList();
    }

    private String yaml(WorkbenchView view) {
        return input("yaml-source").getValue();
    }

    private Input input(String name) {
        return $(Input.class).withClassName("workbench-input-" + name).single();
    }

    private NativeButton github() {
        return $(NativeButton.class).withClassName("workbench-github").single();
    }
}
