package com.vaadin.securitystatus.explorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.SpringBootTried;
import com.vaadin.securitystatus.data.model.SpringBootLatest;
import com.vaadin.securitystatus.data.model.SpringBootSupply;
import com.vaadin.securitystatus.explorer.SpringRules.Boot;
import com.vaadin.securitystatus.explorer.SpringRules.Kind;

import jakarta.annotation.security.RolesAllowed;

/**
 * What Spring Boot brings: the findings a Vaadin application carries
 * because of the Spring Boot release it inherits, grouped by that release
 * rather than by the Vaadin line, because that is what the answer depends
 * on. Each release says its supply status, what is available now, and
 * links each advisory to its record. Everything is read from
 * {@code data/front.json}; a supply or an upgrade the data does not carry
 * is left unsaid.
 */
@Route(value = "findings/spring", layout = InternalLayout.class)
@PageTitle("spring.title")
@Menu(order = 1, title = "spring.title")
@RolesAllowed(Roles.SECURITY)
public final class SpringView extends Composite<Div> {

    private static final Set<String> SUPPLY_STATES = Set.of("open", "commercial", "ended");

    private final SecurityDataService data;
    private final Div boots = new Div();

    public SpringView(SecurityDataService data, DataRefreshSignal refresh) {
        this.data = data;
        addClassName("spring-view");
        boots.addClassName("spring-boots");
        var lead = new Paragraph(getTranslation("spring.lead"));
        lead.addClassName("spring-lead");
        getContent().add(ReleaseView.back(), lead, boots);
        var generatedAt = refresh.generatedAt();
        Signal.effect(this, () -> {
            generatedAt.get();
            draw();
        });
    }

    private void draw() {
        boots.removeAll();
        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            boots.add(new DataOutageNotice());
            return;
        }
        List<Boot> all = SpringRules.byBoot(front);
        if (all.isEmpty()) {
            boots.add(lead("spring-none", getTranslation("spring.none")));
            return;
        }
        var differing = new ArrayList<String>();
        for (Boot boot : all) {
            boots.add(boot(front, boot));
            if (boot.differs()) {
                differing.add(boot.release().toString());
            }
        }
        boots.add(lead("spring-caveat", differing.isEmpty() ? getTranslation("spring.caveat.same")
                : getTranslation("spring.caveat.differs", String.join(" and ", differing))));
        boots.add(lead("spring-footer", getTranslation("spring.footer")));
    }

    /**
     * One Spring Boot release as the prototype draws it, D-219: a row shut on
     * arrival naming the release, the Vaadin lines on it, its state, what it
     * brings and, where free patches have ended, its supply in a few words;
     * opened, the supply, the advice and the three groups.
     */
    private Component boot(ExplorerFront front, Boot boot) {
        String release = boot.release().toString();
        Optional<SpringBootSupply> supply = Optional.ofNullable(front.springBootSupply().get(release));
        Optional<SpringBootTried> tried = Optional.ofNullable(front.springBootTried().get(release));
        Map<Kind, List<Finding>> groups = SpringRules.grouped(boot.findings());
        int risks = groups.getOrDefault(Kind.RISK, List.of()).size();
        ExplorerRules.Badge state = ExplorerRules.springBadge(boot.findings());

        var summary = new Div();
        summary.addClassNames("explorer-row-summary", "spring-boot-top");
        var title = new Span(release);
        title.addClassNames("explorer-row-line", "spring-boot-release");
        var lines = new Span(getTranslation("spring.lines", String.join(", ", boot.lines())));
        lines.addClassNames("explorer-row-version", "spring-boot-lines");
        var stateBadge = SeriesParts.badge("spring", state, getTranslation("explorer.state." + state.name()));
        int count = boot.findings().size();
        var says = new Span(risks > 0 ? getTranslation("spring.summary.risk", count, risks)
                : getTranslation("spring.summary", count));
        says.addClassNames("explorer-row-says", "spring-boot-summary");
        summary.add(title, lines, stateBadge, says);
        supply.flatMap(found -> note(found)).ifPresent(note -> {
            var chip = new Span(note);
            chip.addClassName("spring-supply-note");
            summary.add(chip);
        });

        var detail = new Div();
        detail.addClassNames("explorer-detail", "spring-boot-detail");
        supply.ifPresent(found -> {
            supplyTable(boot, found, tried).ifPresent(detail::add);
            found.state().filter(SUPPLY_STATES::contains).ifPresent(
                    word -> detail.add(lead("spring-supply", getTranslation("spring.supply.state." + word))));
        });
        Optional.ofNullable(front.springBootLatest().get(release))
                .ifPresent(latest -> detail.add(advice(latest, supply, tried)));
        groups.forEach((kind, members) -> detail.add(group(kind, members, tried)));

        var section = new Details();
        section.setSummary(summary);
        section.add(detail);
        section.setOpened(false);
        section.addClassNames("explorer-row", "spring-boot", "state-" + state.name().toLowerCase(Locale.ROOT));
        section.getElement().setAttribute("data-boot", release);
        return section;
    }

    /**
     * The supply in a few words, for a reader who has not opened the row;
     * silent while the line still gets free patches, and where nothing could
     * look, D-219.
     */
    private Optional<String> note(SpringBootSupply supply) {
        String state = supply.state().orElse("");
        if ("ended".equals(state)) {
            return Optional.of(getTranslation("spring.supply.note.row.ended"));
        }
        if ("commercial".equals(state)) {
            return supply.commercial()
                    .map(commercial -> getTranslation("spring.supply.note.row.commercial", commercial.toString()));
        }
        return Optional.empty();
    }

    /**
     * The open release and, where it is newer, the commercial one, each with
     * what is known about it. A commercial release is never said to fix
     * anything: nothing here can fetch it.
     */
    private Optional<Component> supplyTable(Boot boot, SpringBootSupply supply, Optional<SpringBootTried> tried) {
        if (supply.open().isEmpty() && supply.commercial().isEmpty()) {
            return Optional.empty();
        }
        var table = new Table();
        table.addClassNames("spring-supply-table", "answer-table", "vaadin-themed-html");
        table.addHeaderRow(getTranslation("spring.supply.column.who"), getTranslation("spring.supply.column.release"),
                getTranslation("spring.supply.column.note"));
        supply.open().ifPresent(open -> {
            String note;
            if (open.equals(boot.release())) {
                note = getTranslation("spring.supply.note.current");
            } else if (tried.flatMap(SpringBootTried::to).filter(open::equals).isPresent()) {
                note = getTranslation("spring.supply.note.tried", String.join(", ",
                        tried.get().measuredOn().stream().map(VersionNumber::toString).toList()),
                        tried.get().closes().size(), boot.findings().size());
            } else {
                note = getTranslation("spring.supply.note.untried");
            }
            row(table, "open", getTranslation("spring.supply.who.open"), open, note);
        });
        supply.commercial().filter(commercial -> supply.open().map(open -> commercial.compareTo(open) > 0).orElse(true))
                .ifPresent(commercial -> row(table, "commercial", getTranslation("spring.supply.who.commercial"),
                        commercial, getTranslation(supply.state().filter("ended"::equals).isPresent()
                                ? "spring.supply.note.ended"
                                : "spring.supply.note.commercial")));
        return Optional.of(table);
    }

    private static void row(Table table, String kind, String who, VersionNumber release, String note) {
        TableRow row = table.addRow();
        row.addClassName("spring-supply-row");
        row.getElement().setAttribute("data-supply", kind);
        row.addDataCell(who);
        row.addDataCell(release.toString());
        row.addDataCell(note);
    }

    /**
     * Where to go: the newest patch of the line and a later minor of the same
     * major, never a newer major, which brings a Vaadin upgrade with it. An
     * entry with neither says nothing newer is published.
     */
    private Component advice(SpringBootLatest latest, Optional<SpringBootSupply> supply,
            Optional<SpringBootTried> tried) {
        var advice = new Div();
        advice.addClassName("spring-advice");
        if (latest.patch().isPresent()) {
            String patch = latest.patch().get().toString();
            String text = supply.isPresent()
                    ? getTranslation("spring.advice.patch.supply")
                            + (tried.isPresent() ? "" : " " + getTranslation("spring.advice.patch.untried"))
                    : getTranslation("spring.advice.patch", patch);
            advice.add(lead("spring-advice-patch", text));
        } else {
            boolean sold = supply.flatMap(SpringBootSupply::commercial).isPresent();
            advice.add(lead("spring-advice-none",
                    getTranslation(sold ? "spring.advice.none.commercial" : "spring.advice.none")));
        }
        latest.minor().ifPresent(minor -> advice.add(
                lead("spring-advice-minor", getTranslation("spring.advice.minor", minor.toString()))));
        var property = lead("spring-advice-property", getTranslation("spring.advice.property"));
        property.addClassName("explorer-hint");
        advice.add(property);
        return advice;
    }

    /** One fold of a release's findings, under the explorer's fold and cap rules, 6.6.2 and 6.6.10. */
    private Component group(Kind kind, List<Finding> findings, Optional<SpringBootTried> tried) {
        var details = new Details(getTranslation("spring.group", findings.size(),
                getTranslation("spring.group." + kind.name())));
        details.addClassNames("spring-group", "group-" + kind.name().toLowerCase(Locale.ROOT));
        details.getElement().setAttribute("data-group", kind.name());
        details.setOpened(kind.startsOpen());
        var list = new Div();
        list.addClassName("spring-findings");
        ExplorerRules.Capped capped = ExplorerRules.capped(findings);
        Set<String> closes = Set.copyOf(tried.map(SpringBootTried::closes).orElse(List.of()));
        for (Finding finding : capped.shown()) {
            list.add(finding(kind, finding, closes.contains(finding.cve())
                    ? tried.flatMap(SpringBootTried::to)
                    : Optional.empty()));
        }
        details.add(list);
        if (capped.more() > 0) {
            details.add(lead("explorer-group-more", getTranslation("explorer.group.more", capped.more())));
        }
        return details;
    }

    private Component finding(Kind kind, Finding finding, Optional<VersionNumber> goneOn) {
        var row = new Div();
        row.addClassName("spring-finding");
        row.getElement().setAttribute("data-cve", finding.cve());
        var record = new Anchor(SpringRules.recordUrl(finding.cve()), finding.cve());
        record.setTarget(AnchorTarget.BLANK);
        record.addClassName("spring-finding-record");
        row.add(record);
        finding.severity().ifPresent(word -> {
            var severity = new Span(getTranslation("severity." + word));
            severity.addClassNames("explorer-severity", "severity-" + word);
            row.add(severity);
        });
        var verdict = new Span(getTranslation("spring.verdict." + kind.name()));
        verdict.addClassName("spring-finding-verdict");
        row.add(verdict);
        goneOn.ifPresent(to -> {
            var gone = new Span(getTranslation("spring.gone", to.toString()));
            gone.addClassName("spring-finding-gone");
            row.add(gone);
        });
        return row;
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}
