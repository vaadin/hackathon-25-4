package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.answerTable;
import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.layer;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.pairs;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.pair;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.note;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.heading;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.verdictState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableDataCell;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.customer.AdvisoryRules;
import com.vaadin.securitystatus.customer.CveRules;
import com.vaadin.securitystatus.customer.LookupInput;
import com.vaadin.securitystatus.customer.VersionRules;
import com.vaadin.securitystatus.customer.VerdictFor;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CveAnswer;
import com.vaadin.securitystatus.data.model.VersionAnswer;
import com.vaadin.securitystatus.ui.common.AdvisoryIds;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.securitystatus.ui.common.ViewHeading;

/**
 * The answer by advisory, screen 3 of the delivered design: one verdict card
 * per id in the order asked, how every followed line stands against it, and
 * the three layers of details, each answered for the reader's version when
 * they gave one.
 *
 * <p>
 * Every sentence is said only where the data carries it. A field the
 * advisory or the verdict does not carry is left out, never drawn empty.
 */
@Route("cve/:ids/:version?")
@RouteParent(HomeView.class)
@PageTitle("cve.title")
@PermitAll
public final class CveView extends Composite<Div> implements BeforeEnterObserver {

    public static final String IDS = "ids";

    public static final String VERSION = "version";

    /** The address of the answer for these ids, at this version when one is given. */
    public static RouteParameters parameters(String ids, String version) {
        return version.isEmpty() ? new RouteParameters(IDS, ids) : new RouteParameters(Map.of(IDS, ids, VERSION, version));
    }

    /**
     * What the reader asked: the ids as the address carries them, the entries
     * of the address that are no advisory id, and the version beside them.
     */
    record Question(List<String> ids, List<String> unparsed, String version) {
    }

    /**
     * One card: the id asked, the advisory it resolved to, the alias it was
     * asked as, and whether the id is well formed. An entry that is no CVE or
     * GHSA id is never looked up and gets the plain unknown card, D-226.
     */
    record One(String id, @Nullable CveAnswer advisory, @Nullable String askedAs, boolean wellFormed) {
    }

    /** Everything the screen is drawn from. */
    record Loaded(@Nullable CasesIndex index, String version, @Nullable VersionAnswer measured, List<One> ones) {

        boolean unreachable() {
            return index == null;
        }
    }

    private final SecurityDataService data;
    private final ValueSignal<Question> asked = new ValueSignal<>(new Question(List.of(), List.of(), ""));
    private final Div body = new Div();
    /** The address of this answer, for the copied text. Null when no request says where the page lives. */
    private @Nullable String address;

    public CveView(SecurityDataService data, DataRefreshSignal refresh) {
        this.data = data;
        addClassName("cve-view");
        body.addClassNames("version-body", "cve-body");
        // No visible title above the answer, as in the prototype; kept for screen readers, D-270.
        getContent().add(ViewHeading.hiddenAnswer(), body);

        var generatedAt = refresh.generatedAt();
        Signal<Loaded> answer = Signal.computed(() -> {
            generatedAt.get();
            return load(asked.get());
        });
        Signal.effect(this, () -> draw(answer.get()));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        LookupInput.Ids ids = LookupInput.splitIds(event.getRouteParameters().get(IDS).orElse(""));
        String version = LookupInput.cleanVersion(event.getRouteParameters().get(VERSION).orElse(""));
        address = address(event.getLocation().getPathWithQueryParameters());
        asked.set(new Question(ids.ids(), ids.unrecognised(), version));
    }

    private static @Nullable String address(String path) {
        if (VaadinRequest.getCurrent() instanceof VaadinServletRequest request) {
            return ServletUriComponentsBuilder.fromContextPath(request.getHttpServletRequest()).path("/" + path)
                    .build().toUriString();
        }
        return null;
    }

    // Loading

    private Loaded load(Question question) {
        try {
            CasesIndex index = data.index();
            VersionAnswer measured = question.version().isBlank() ? null
                    : data.version(VersionNumber.of(question.version())).orElse(null);
            List<One> ones = new ArrayList<>(resolve(question.ids()));
            question.unparsed().stream().distinct().map(entry -> new One(entry, null, null, false))
                    .forEach(ones::add);
            return new Loaded(index, question.version(), measured, ones);
        } catch (DataUnreachableException unreachable) {
            return new Loaded(null, question.version(), null, List.of());
        }
    }

    /**
     * Each id to its advisory. A GHSA id with no document of its own is answered
     * under another asked advisory that names it as an alias, D-62: the
     * published data carries no alias map to look it up in otherwise.
     */
    private List<One> resolve(List<String> ids) {
        Map<String, CveAnswer> found = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            data.cve(id).ifPresent(advisory -> found.put(id, advisory));
        }
        List<One> ones = new ArrayList<>();
        List<String> answered = new ArrayList<>();
        for (String id : ids) {
            CveAnswer advisory = found.get(id);
            String askedAs = null;
            if (advisory == null) {
                Optional<CveAnswer> tracked = found.values().stream()
                        .filter(candidate -> candidate.aliases().contains(id)).findFirst();
                if (tracked.isPresent()) {
                    if (answered.contains(tracked.get().cve()) || ids.contains(tracked.get().cve())) {
                        continue;
                    }
                    advisory = tracked.get();
                    askedAs = id;
                }
            }
            String shown = advisory == null ? id : advisory.cve();
            if (answered.contains(shown)) {
                continue;
            }
            answered.add(shown);
            ones.add(new One(shown, advisory, askedAs, true));
        }
        return ones;
    }

    // Drawing

    private void draw(Loaded answer) {
        body.removeAll();
        if (answer.unreachable()) {
            body.add(new DataOutageNotice());
            return;
        }
        CasesIndex index = answer.index();
        boolean several = answer.ones().size() > 1;
        if (several) {
            body.add(heading(getTranslation("cve.many.title", answer.ones().size()),
                    answer.version().isBlank() ? getTranslation("cve.many.enter")
                            : getTranslation("cve.many.for", answer.version())));
        }
        if (!answer.version().isBlank() && answer.measured() == null
                && answer.ones().stream().anyMatch(one -> one.advisory() != null)) {
            body.add(unmeasuredNotice(index, answer));
        }
        for (One one : answer.ones()) {
            body.add(oneAnswer(index, answer, one, several));
        }
    }

    /** Said once, above the cards, rather than once per card. */
    private Component unmeasuredNotice(CasesIndex index, Loaded answer) {
        VersionNumber version = VersionNumber.of(answer.version());
        var notice = css(new Div(new Paragraph(getTranslation("cve.unmeasured.notice", answer.version()))), "cve-unmeasured");
        notice.getElement().setAttribute("role", "note");
        VersionRules.nearestMeasured(index, version).ifPresent(nearest -> {
            String ids = String.join(",", answer.ones().stream().map(One::id).toList());
            var link = css(new RouterLink(getTranslation("cve.unmeasured.nearest", nearest.version().toString()),
                    CveView.class, parameters(ids, nearest.version().toString())), "nearest-measured");
            notice.add(new Paragraph(link));
        });
        return notice;
    }

    private Component oneAnswer(CasesIndex index, Loaded answer, One one, boolean several) {
        var section = css(new Section(), "cve-answer");
        if (several) {
            section.addClassName("framed");
        }
        section.getElement().setAttribute("data-advisory", one.id());
        CveAnswer advisory = one.advisory();
        VersionAnswer measured = answer.measured();
        Optional<CveRules.Fix> fix = advisory == null ? Optional.empty()
                : CveRules.fixFor(measured, index, advisory.cve());
        CveRules.State state = CveRules.state(advisory, measured, fix);
        List<VerdictFor> said = advisory == null ? List.of() : CveRules.verdicts(advisory, measured);

        section.add(verdictCard(index, answer, one, state, fix, said));
        if (advisory == null) {
            return section;
        }
        var lines = css(new Div(heading(getTranslation("cve.lines.title"),
                answer.version().isBlank() ? getTranslation("cve.lines.enter")
                        : getTranslation("cve.lines.yours", answer.version())),
                lineCards(index, advisory, answer, fix)), "cve-lines");
        section.add(lines);

        var details = css(new Div(heading(several ? getTranslation("cve.details.for", one.id())
                : getTranslation("cve.details.title"), null)), "cve-details");
        details.add(layers(index, answer, one, advisory, said, several));
        section.add(details);
        return section;
    }

    // The verdict card

    /** The words of a card, kept together so the copied answer is exactly what the card says. */
    private record Words(String word, String title, List<String> why) {
    }

    private Component verdictCard(CasesIndex index, Loaded answer, One one, CveRules.State state,
            Optional<CveRules.Fix> fix, List<VerdictFor> said) {
        var box = css(new Section(), "verdict-card", "state-" + stateSlug(state));
        Words words = words(answer, one, state, fix, said);

        box.add(verdictState(stateIcon(state), words.word()), new H3(words.title()));
        if (!words.why().isEmpty()) {
            box.add(css(new Paragraph(String.join(" ", words.why())), "verdict-why"));
        }
        if (state == CveRules.State.UNKNOWN) {
            return box;
        }

        var actions = css(new Div(), "verdict-actions");
        fix.flatMap(found -> VersionRules.releaseNotes(index, found.to()).map(url -> {
            return css(new Anchor(url, getTranslation("version.action.releaseNotes", found.to().toString())), "verdict-action", "primary");
        })).ifPresent(actions::add);
        Optional<String> scanned = Optional.ofNullable(answer.measured()).flatMap(VersionAnswer::scannedAt)
                .map(at -> getTranslation("version.scanned", at.toLocalDate().toString()));
        if (!answer.version().isBlank()) {
            actions.add(copyButton(one.id(), copyText(words, scanned, Optional.ofNullable(address))));
        }
        scanned.ifPresent(text -> {
            var clock = decorative(VaadinIcon.CLOCK.create());
            actions.add(css(new Span(clock, new Span(text)), "verdict-scanned"));
        });
        box.add(actions);
        return box;
    }

    private Words words(Loaded answer, One one, CveRules.State state, Optional<CveRules.Fix> fix,
            List<VerdictFor> said) {
        String id = one.id();
        String version = answer.version();
        VersionAnswer measured = answer.measured();
        Optional<String> impact = said.stream().map(VerdictFor::assessment).map(Assessment::impact)
                .flatMap(Optional::stream).findFirst().map(String::strip);
        var why = new ArrayList<String>();
        return switch (state) {
            case FIX -> {
                CveRules.Fix found = fix.orElseThrow();
                String to = found.to().toString();
                String reach = found.sameLine() ? "same" : "other";
                Integer remains = found.remains();
                if (found.secure()) {
                    why.add(getTranslation("cve.why.fix." + reach + ".secure", version, to));
                } else if (remains != null && remains > 0) {
                    why.add(getTranslation("cve.why.fix." + reach + (remains == 1 ? ".rest.one" : ".rest.many"),
                            version, to, remains));
                } else {
                    why.add(getTranslation("cve.why.fix." + reach + ".gone", version, to));
                }
                yield new Words(getTranslation("cve.word.fix"), getTranslation("cve.title.fix", id, to), why);
            }
            case AFFECTS -> {
                impact.ifPresent(why::add);
                yield new Words(getTranslation("cve.word.affects"), getTranslation("cve.title.affects", id, version),
                        why);
            }
            case TRIAGE -> {
                why.add(getTranslation("cve.why.triage"));
                yield new Words(getTranslation("state.notAssessed"), getTranslation("cve.title.triage", id), why);
            }
            case BOOT -> {
                impact.ifPresent(why::add);
                why.add(getTranslation("cve.why.boot.pinned"));
                yield new Words(getTranslation("cve.word.affects"), getTranslation("cve.title.boot", id, version), why);
            }
            case DISCONTINUED -> {
                List<String> through = measured == null ? List.of() : measured.discontinuedThrough();
                impact.ifPresent(why::add);
                why.add(getTranslation("cve.why.disc.real"));
                if (through.isEmpty()) {
                    why.add(getTranslation("cve.why.disc.unnamed"));
                } else {
                    why.add(getTranslation("cve.why.disc", String.join(", ", through)));
                }
                if (said.stream().anyMatch(one1 -> one1.assessment().workaround().isPresent())) {
                    why.add(getTranslation("cve.why.disc.workaround"));
                }
                String title = through.isEmpty() ? getTranslation("cve.title.disc.unnamed", id, version)
                        : getTranslation("cve.title.disc", id, version, String.join(", ", through));
                yield new Words(getTranslation("cve.word.affects"), title, why);
            }
            case NOT_AFFECTED -> {
                if (measured != null && measured.fromBuild().contains(id)) {
                    why.add(getTranslation("cve.why.build"));
                }
                impact.ifPresent(why::add);
                if (why.isEmpty()) {
                    why.add(getTranslation("cve.why.notAffected"));
                }
                // A risk verdict whose every route is the reader's own Spring Boot: said, after the answer, D-187.
                if (measured != null && measured.theirsRisk().contains(id)) {
                    why.add(getTranslation("cve.why.boot.theirs"));
                }
                yield new Words(getTranslation("cve.word.notAffected"),
                        getTranslation("cve.title.notAffected", id, version), why);
            }
            case FIXED, NOT_REPORTED -> {
                why.add(getTranslation("cve.why.notReported", version));
                yield new Words(getTranslation(state == CveRules.State.FIXED ? "cve.word.fixed" : "cve.word.notAffected"),
                        getTranslation("cve.title.notReported", id, version), why);
            }
            case NO_VERSION -> {
                why.add(getTranslation(version.isBlank() ? "cve.why.measured" : "cve.why.measured.unmeasured"));
                yield new Words(getTranslation("cve.word.measured"), getTranslation("cve.title.measured", id), why);
            }
            case UNKNOWN -> {
                boolean ghsa = one.wellFormed() && id.startsWith("GHSA-");
                why.add(getTranslation(ghsa ? "cve.why.unknown.ghsa" : "cve.why.unknown", id));
                yield new Words(getTranslation("cve.word.unknown"),
                        getTranslation(ghsa ? "cve.title.unknown.ghsa" : "cve.title.unknown", id), why);
            }
        };
    }

    /** The card's own words, in the order the card shows them, and nothing else. */
    static String copyText(Words words, Optional<String> scanned, Optional<String> address) {
        var text = new StringBuilder(words.word()).append(": ").append(words.title()).append('\n');
        if (!words.why().isEmpty()) {
            text.append(String.join(" ", words.why())).append('\n');
        }
        scanned.ifPresent(line -> text.append(line).append('\n'));
        address.ifPresent(line -> text.append(line).append('\n'));
        return text.toString().strip();
    }

    /**
     * The design's second action, built: the card's words on the clipboard
     * through the platform's Clipboard API, whose observed form reports the
     * browser's answer back here, and one notice saying how it went, D-63.
     */
    private Component copyButton(String id, String text) {
        var button = css(new Button(getTranslation("cve.action.copy"), VaadinIcon.COPY_O.create()), "copy-answer");
        button.addThemeVariants(ButtonVariant.TERTIARY);
        button.getElement().setAttribute("data-answer", text);
        Clipboard.onClick(button).writeText(text, copied -> {
            var done = Notification.show(getTranslation("cve.copy.done", id));
            done.addThemeVariants(NotificationVariant.SUCCESS);
            done.addClassName("copy-notice");
        }, failure -> {
            var failed = Notification.show(getTranslation("cve.copy.failed"));
            failed.addThemeVariants(NotificationVariant.ERROR);
            failed.addClassName("copy-notice");
        });
        return button;
    }

    private static String stateSlug(CveRules.State state) {
        return switch (state) {
            case FIX -> "fix_available";
            case AFFECTS, TRIAGE, BOOT, DISCONTINUED -> "unresolved";
            case NOT_AFFECTED, FIXED, NOT_REPORTED -> "secure";
            case NO_VERSION -> "no_version";
            case UNKNOWN -> "unmeasured";
        };
    }

    private static VaadinIcon stateIcon(CveRules.State state) {
        return switch (state) {
            // The glyph alone: the stylesheet draws the prototype's filled circle round it.
            case FIX, NO_VERSION -> VaadinIcon.ARROW_UP;
            case AFFECTS, BOOT, DISCONTINUED -> VaadinIcon.EXCLAMATION;
            case TRIAGE, UNKNOWN -> VaadinIcon.QUESTION;
            case NOT_AFFECTED, FIXED, NOT_REPORTED -> VaadinIcon.CHECK;
        };
    }

    // Across supported versions

    private Component lineCards(CasesIndex index, CveAnswer advisory, Loaded answer, Optional<CveRules.Fix> fix) {
        String readers = answer.measured() != null ? CveRules.lineOf(answer.measured())
                : answer.version().isBlank() ? "" : VersionNumber.of(answer.version()).line();
        var cards = css(new Div(), "cve-line-cards");
        for (CasesIndex.Line line : VersionRules.tableLines(index, readers, false)) {
            cards.add(lineCard(advisory, line, line.line().equals(readers), fix));
        }
        return cards;
    }

    private Component lineCard(CveAnswer advisory, CasesIndex.Line line, boolean yours, Optional<CveRules.Fix> fix) {
        VersionNumber head = VersionRules.spokenFor(line);
        Optional<CveAnswer.Coverage> coverage = Optional.ofNullable(advisory.coverage().get(line.line()));
        Optional<String> status = coverage.flatMap(CveAnswer.Coverage::status);
        Optional<String> said = Optional.ofNullable(advisory.onVersions().get(head.toString()))
                .flatMap(verdict -> verdict.state());
        Optional<VersionNumber> fixedIn = coverage.flatMap(CveAnswer.Coverage::fixedIn);
        Optional<VersionNumber> mineFixed = yours ? fix.map(CveRules.Fix::to) : Optional.empty();

        String state;
        String word;
        if (mineFixed.isPresent()) {
            // The headline's answer, on the card beside it, D-64.
            state = "fix_available";
            word = getTranslation("cve.word.fix");
        } else if (said.filter("risk"::equals).isPresent()) {
            state = "unresolved";
            word = getTranslation("cve.word.affects");
        } else if (said.filter("in_triage"::equals).isPresent()) {
            state = "unresolved";
            word = getTranslation("state.notAssessed");
        } else if (said.isPresent()) {
            state = "secure";
            word = getTranslation("verdict.short." + said.get());
        } else if (status.filter("fixed"::equals).isPresent()) {
            state = "secure";
            word = getTranslation("cve.line.word.fixed");
        } else if (status.filter("clean"::equals).isPresent()) {
            state = "secure";
            word = getTranslation("cve.line.word.never");
        } else if (status.filter("affected"::equals).isPresent()) {
            state = "unresolved";
            word = getTranslation("cve.line.word.reported");
        } else {
            state = "unmeasured";
            word = getTranslation("cve.line.word.unmeasured");
        }

        Optional<String> detail;
        if (fixedIn.isPresent()) {
            detail = Optional.of(getTranslation("cve.line.in", fixedIn.get().toString()));
        } else if (mineFixed.isPresent()) {
            detail = Optional.of(getTranslation("cve.line.in", mineFixed.get().toString()));
        } else if (said.filter(word1 -> !"risk".equals(word1) && !"in_triage".equals(word1)).isPresent()) {
            detail = Optional.of(getTranslation("cve.line.measuredOn", head.toString()));
        } else if (coverage.flatMap(CveAnswer.Coverage::fixedInPrerelease).isPresent()) {
            detail = Optional.of(getTranslation("cve.line.prerelease",
                    coverage.flatMap(CveAnswer.Coverage::fixedInPrerelease).get().toString()));
        } else if (status.filter("affected"::equals).isPresent()) {
            detail = Optional.of(getTranslation("cve.line.newest", head.toString()));
        } else if (status.filter("clean"::equals).isPresent()) {
            detail = Optional.of(getTranslation("cve.line.nothing"));
        } else {
            detail = Optional.empty();
        }

        var card = css(new RouterLink(VersionView.class, new RouteParameters(VersionView.VERSION, head.toString())), "cve-line-card", "state-" + state);
        card.getElement().setAttribute("data-line", line.line());
        if (yours) {
            card.addClassName("yours");
        }
        var minor = css(new Span(yours ? getTranslation("version.lines.yours", line.line()) : line.line()), "cve-line-minor");
        var verdict = css(new Span(word), "cve-line-word");
        card.add(minor, verdict);
        detail.ifPresent(text -> {
            var span = css(new Span(text), "cve-line-detail");
            card.add(span);
        });
        coverage.flatMap(CveAnswer.Coverage::fixedInEnterprise).ifPresent(enterprise -> {
            card.add(css(new Span(getTranslation("cve.line.enterprise", enterprise.toString())), "cve-line-when"));
        });
        return card;
    }

    // The three layers

    private Component layers(CasesIndex index, Loaded answer, One one, CveAnswer advisory, List<VerdictFor> said,
            boolean several) {
        var layers = css(new Div(), "finding-layers");
        VersionAnswer measured = answer.measured();
        // Without a measured version no verdict is the reader's, so none is quoted as theirs: the
        // verdicts are listed with their coordinates instead, CVE-02.
        Optional<Assessment> first = measured == null ? Optional.empty()
                : said.stream().findFirst().map(VerdictFor::assessment);

        // 1. What this means for you
        var means = pairs();
        Optional<String> impact = first.flatMap(Assessment::impact);
        if (impact.isPresent()) {
            means.add(pair(getTranslation("version.layer.exploited"), new Markdown(impact.get())));
        } else if (measured != null && measured.triage().contains(advisory.cve())) {
            means.add(pair(getTranslation("version.layer.exploited"),
                    new Paragraph(getTranslation("cve.layer.triageOn", answer.version()))));
        }
        // The catalogue's label as published, `not_current` included, as the prototype prints it, D-188.
        CveRules.applicability(advisory, measured).flatMap(word -> AdvisoryRules.label(index, word, this::getTranslation))
                .ifPresent(label -> means.add(pair(getTranslation("version.layer.assessment"), new Paragraph(label))));
        source(index, advisory).ifPresent(text -> means.add(pair(getTranslation("cve.layer.source"), new Paragraph(text))));
        // Shut when several advisories share the page, so each one reads as its card first, D-242.
        layers.add(layer("means", getTranslation("version.layer.means"), means, !several));

        // 2. Affected modules and the fix
        var modules = pairs();
        if (!advisory.modules().isEmpty()) {
            modules.add(pair(getTranslation("cve.layer.modules"), new Paragraph(String.join(", ", advisory.modules()))));
        }
        box(advisory, answer).ifPresent(box -> modules.add(pair(getTranslation("cve.layer.box"), box)));
        first.flatMap(Assessment::workaround)
                .ifPresent(text -> modules.add(pair(getTranslation("version.finding.do"), new Markdown(text))));
        var fix = new ArrayList<Component>();
        advisory.fixOwner().flatMap(owner -> switch (owner) {
            case "upstream" -> Optional.of("cve.fix.upstream");
            case "vaadin" -> Optional.of("cve.fix.vaadin");
            default -> Optional.<String>empty();
        }).ifPresent(key -> fix.add(new Paragraph(getTranslation(key))));
        first.flatMap(Assessment::fixExpected).ifPresent(text -> fix.add(new Markdown(text)));
        first.map(Assessment::fixPrs).filter(list -> !list.isEmpty()).ifPresent(list -> {
            var prs = css(new Paragraph(), "finding-tracking");
            list.forEach(url -> prs.add(FindingCard.pullRequest(url)));
            fix.add(prs);
        });
        first.flatMap(Assessment::handover)
                .ifPresent(text -> fix.add(new Markdown(getTranslation("cve.fix.handover", text))));
        if (!fix.isEmpty()) {
            modules.add(pair(getTranslation("version.finding.fix"), fix.toArray(Component[]::new)));
        }
        var packageVerdicts = new Div();
        if (said.size() > 1 || (measured == null && !said.isEmpty())) {
            packageVerdicts.addClassName("cve-verdicts");
            if (said.size() > 1) {
                var note = note(getTranslation("cve.verdicts.note", said.size()));
                packageVerdicts.add(note);
            }
            packageVerdicts.add(verdictsTable(index, said));
        }
        var modulesBody = new Div(modules);
        if (packageVerdicts.getComponentCount() > 0) {
            modulesBody.add(packageVerdicts);
        }
        if (modules.getComponentCount() > 0 || packageVerdicts.getComponentCount() > 0) {
            layers.add(layer("modules", getTranslation("version.layer.modules"), modulesBody, false));
        }

        // 3. The advisory and the evidence
        var evidence = css(new Div(), "layer-full");
        // The severity word as the data writes it, as the prototype prints it, D-188.
        advisory.severity().flatMap(VersionRules::severity).ifPresent(known -> evidence.add(note(
                getTranslation("cve.evidence.severity", known))));
        advisory.summary().map(String::strip).ifPresent(text -> evidence.add(new Paragraph(text)));
        // The version document's copy of a verdict carries no commentary; the advisory document's copy of the
        // same verdict does, and the prototype prints it, D-196.
        said.stream().map(verdict -> Optional.ofNullable(advisory.assessments().get(verdict.id()))
                .flatMap(Assessment::commentary).or(() -> verdict.assessment().commentary()))
                .flatMap(Optional::stream).distinct().forEach(text -> evidence.add(new Markdown(text)));
        // The measured range and the range the verdict is written for, one paragraph, D-188.
        List<String> scope = new ArrayList<>();
        range(advisory, measured).ifPresent(scope::add);
        first.flatMap(Assessment::affects).ifPresent(affects -> scope.add(getTranslation("cve.evidence.affects", affects)));
        if (!scope.isEmpty()) {
            evidence.add(note(String.join(" ", scope)));
        }
        advisory.firstSeen().ifPresent(seen -> evidence.add(note(getTranslation("cve.evidence.firstSeen",
                seen.toLocalDate().toString()))));
        var record = css(new Paragraph(new Anchor(CveRules.recordUrl(advisory.cve()),
                getTranslation("cve.evidence.record", advisory.cve())), new Text(".")), "cve-record");
        if (!advisory.aliases().isEmpty()) {
            var aliases = new Span();
            AdvisoryIds.fill(aliases, " " + getTranslation("cve.evidence.aliases", AdvisoryIds.HERE), advisory.aliases());
            record.add(aliases);
        }
        if (one.askedAs() != null) {
            var alias = new Span();
            AdvisoryIds.fill(alias, " " + getTranslation("cve.evidence.askedAs", AdvisoryIds.HERE), List.of(one.askedAs()));
            alias.addClassName("asked-as");
            record.add(alias);
        }
        if (Boolean.TRUE.equals(advisory.vaadinAdvisory())) {
            record.add(new Span(" " + getTranslation("cve.evidence.vaadin")));
        }
        evidence.add(record);
        layers.add(layer("evidence", getTranslation("cve.layer.evidence"), evidence, false));
        return layers;
    }

    /** Where the advisory reaches a Vaadin application from, and what it rides in on, rule 6.11.12. */
    private Optional<String> source(CasesIndex index, CveAnswer advisory) {
        List<String> from = advisory.origins().stream().map(origin -> index.originLabels().getOrDefault(origin, origin))
                .toList();
        // As the prototype writes it: the origins with a capital, the unestablished one as published.
        Optional<String> where = from.isEmpty() ? Optional.ofNullable(index.originLabels().get("unknown"))
                : Optional.of(capitalised(String.join(", ", from)));
        List<String> parts = new ArrayList<>();
        where.ifPresent(parts::add);
        if (!advisory.packages().isEmpty()) {
            parts.add(String.join(", ", advisory.packages()));
        }
        return parts.isEmpty() ? Optional.empty() : Optional.of(String.join(" · ", parts));
    }

    /**
     * The dependency as each line resolves it, which is what a scanner reads:
     * the reader's line when a version was given, every line otherwise.
     */
    private Optional<Component> box(CveAnswer advisory, Loaded answer) {
        String mine = answer.measured() != null ? CveRules.lineOf(answer.measured())
                : answer.version().isBlank() ? null : VersionNumber.of(answer.version()).line();
        List<String> lines = mine == null ? new ArrayList<>(advisory.arrives().keySet())
                : advisory.arrives().containsKey(mine) ? new ArrayList<>(List.of(mine)) : new ArrayList<>();
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        if (mine != null) {
            return boxSentences(mine, advisory.arrives().get(mine));
        }
        lines.sort((a, b) -> VersionNumber.of(b).compareTo(VersionNumber.of(a)));
        var table = answerTable(getTranslation("cve.box.col.line"), getTranslation("cve.box.col.package"),
                getTranslation("cve.box.col.scanned"));
        table.addClassName("cve-box");
        for (String line : lines) {
            for (CveAnswer.Arrival arrival : advisory.arrives().get(line)) {
                if (arrival.packageName().isEmpty()) {
                    continue;
                }
                TableRow row = table.addRow();
                row.addRowHeaderCell(line);
                TableDataCell coordinate = row.addDataCell(arrival.packageName().get());
                List<String> facts = new ArrayList<>();
                arrival.version().ifPresent(facts::add);
                arrival.bundledIn().ifPresent(inside -> facts.add(getTranslation("cve.box.inside", inside)));
                arrival.fixedUpstream().ifPresent(upstream -> facts.add(getTranslation("cve.box.upstream", upstream)));
                if (facts.isEmpty()) {
                    coordinate.setColspan(2);
                } else {
                    row.addDataCell(String.join(" · ", facts));
                }
            }
        }
        return table.getBodyRows().isEmpty() ? Optional.empty() : Optional.of(table);
    }

    /**
     * The reader's own line as the prototype says it, one sentence per
     * package: the coordinate and its version as scanned, then the library's
     * fix where the data names one, D-188.
     */
    private Optional<Component> boxSentences(String line, List<CveAnswer.Arrival> arrivals) {
        var says = css(new Div(), "cve-box");
        for (CveAnswer.Arrival arrival : arrivals) {
            if (arrival.packageName().isEmpty()) {
                continue;
            }
            List<String> coordinate = new ArrayList<>(List.of(arrival.packageName().get()));
            arrival.version().ifPresent(coordinate::add);
            arrival.bundledIn().ifPresent(inside -> coordinate.add(getTranslation("cve.box.inside", inside)));
            String sentence = getTranslation("cve.box.line", String.join(" ", coordinate), line);
            if (arrival.fixedUpstream().isPresent()) {
                sentence += " " + getTranslation("cve.box.line.upstream", arrival.fixedUpstream().get());
            }
            says.add(new Paragraph(sentence));
        }
        return says.getComponentCount() == 0 ? Optional.empty() : Optional.of(says);
    }

    /** One row per verdict, each naming the coordinate it answers for with the differing part marked, rule 6.4.9. */
    private Table verdictsTable(CasesIndex index, List<VerdictFor> said) {
        var table = answerTable(getTranslation("cve.verdicts.col.coordinate"), getTranslation("cve.verdicts.col.verdict"),
                getTranslation("cve.verdicts.col.impact"));
        table.addClassName("cve-verdicts-table");
        List<String> coordinates = said.stream().map(one -> one.packages().isEmpty()
                ? one.assessment().purl().orElse(one.id()) : String.join(", ", one.packages())).toList();
        List<CveRules.Marked> marked = CveRules.markDifference(coordinates);
        for (int i = 0; i < said.size(); i++) {
            VerdictFor one = said.get(i);
            TableRow row = table.addRow();
            row.getElement().setAttribute("data-verdict", one.id());
            var coordinate = css(new Span(), "coordinate");
            CveRules.Marked parts = marked.get(i);
            if (!parts.before().isEmpty()) {
                coordinate.add(new Span(parts.before()));
            }
            if (!parts.differs().isEmpty()) {
                coordinate.add(new Mark(parts.differs()));
            }
            if (!parts.after().isEmpty()) {
                coordinate.add(new Span(parts.after()));
            }
            Optional<String> label = one.assessment().applicability().map(word -> getTranslation("verdict.short." + word));
            Optional<String> impact = one.assessment().impact();
            // A field the verdict does not carry has no cell: the one before it spans its column.
            var header = row.addRowHeaderCell(coordinate);
            if (label.isPresent()) {
                var cell = row.addDataCell(label.get());
                if (impact.isEmpty()) {
                    cell.setColspan(2);
                }
            }
            if (impact.isPresent()) {
                var cell = row.addDataCell(new Markdown(impact.get()));
                if (label.isEmpty()) {
                    cell.setColspan(2);
                }
            }
            if (label.isEmpty() && impact.isEmpty()) {
                header.setColspan(3);
            }
        }
        return table;
    }

    /** The measured range of the reader's line, and what the fix actually was where the scans could tell. */
    private Optional<String> range(CveAnswer advisory, @Nullable VersionAnswer measured) {
        if (measured == null) {
            return Optional.empty();
        }
        String line = CveRules.lineOf(measured);
        Optional<CveAnswer.Coverage> coverage = Optional.ofNullable(advisory.coverage().get(line));
        if (coverage.isEmpty()) {
            return Optional.empty();
        }
        CveAnswer.Coverage entry = coverage.get();
        List<String> said = new ArrayList<>();
        Integer count = entry.affectedCount();
        if (count != null && count > 0 && entry.affectedTo().isPresent()) {
            if (count == 1) {
                said.add(getTranslation("cve.evidence.range.one", entry.affectedTo().get().toString(), line));
            } else if (entry.affectedFrom().isPresent()) {
                said.add(getTranslation("cve.evidence.range.many", entry.affectedFrom().get().toString(),
                        entry.affectedTo().get().toString(), count, line));
            }
        }
        entry.fixedIn().ifPresent(fixedIn -> entry.fixMeans().ifPresent(means -> {
            String kind = means.kind().orElse("");
            switch (kind) {
                case "dependency_updated" -> means.changes().forEach(change -> change.packageName().ifPresent(name -> {
                    if (change.to().isEmpty()) {
                        said.add(getTranslation("cve.evidence.fix.removed", name, fixedIn.toString()));
                    } else {
                        said.add(getTranslation("cve.evidence.fix.updated", name, String.join(", ", change.from()),
                                String.join(", ", change.to()), fixedIn.toString()));
                    }
                }));
                case "mitigated" -> said.add(getTranslation("cve.evidence.fix.mitigated", fixedIn.toString()));
                default -> said.add(getTranslation("cve.evidence.fix.unknown", fixedIn.toString()));
            }
        }));
        return said.isEmpty() ? Optional.empty() : Optional.of(String.join(" ", said));
    }

    // Small parts

    private static String capitalised(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
