package com.vaadin.securitystatus.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;

/**
 * The questions the explorer and the release page share, answered without
 * components: the registry of products, the measurement a line is read from,
 * the five groups of the release page, the two halves the badges answer for,
 * D-205, and the full version a search jumps to. The front door's own
 * questions are in {@link ExplorerAnswer}. Everything here reads
 * {@code data/front.json} and nothing else.
 */
final class ExplorerRules {

    /** The colours a ring can take, assigned by registry order. The palette itself is in the stylesheet. */
    static final int PALETTE = 12;

    /** A group shows at most this many findings and counts the rest, rule 6.6.10. */
    static final int GROUP_CAP = 150;

    private static final List<String> SEVERITY_ORDER = List.of("critical", "high", "moderate", "low");

    private static final Pattern FULL_VERSION = Pattern
            .compile("^(?:vaadin\\s+)?v?(\\d+\\.\\d+\\.\\d+(?:[.\\-]?[a-z]+\\d*)?)$", Pattern.CASE_INSENSITIVE);

    /**
     * One product of the registry. {@code index} is its position in
     * {@code module_names}, which the findings use; {@code optional}, {@code
     * discontinued}, {@code fixed} and {@code switchesShape} come from
     * {@code profile}.
     */
    record Product(int index, String name, boolean optional, boolean discontinued, boolean fixed,
            boolean switchesShape) {

        /** The ring colour, from one fixed palette by registry order. */
        int ring() {
            return index % PALETTE;
        }

        /** A product every application has cannot be turned off, so it has no chip. */
        boolean switchable() {
            return !fixed;
        }
    }

    /** The five groups of rule 6.6.1, in their order. The two that ask for work start open, rule 6.6.2. */
    enum Group {
        RISK(true), UNJUDGED(true), SPRING(false), BUILD(false), ANSWERED(false);

        private final boolean startsOpen;

        Group(boolean startsOpen) {
            this.startsOpen = startsOpen;
        }

        boolean startsOpen() {
            return startsOpen;
        }
    }

    /** What a badge says about one side of a line. */
    enum Badge {
        VULNERABLE, UNJUDGED, NOT_AFFECTED
    }

    /** A capped group: what is drawn and how many are left out. */
    record Capped(List<Finding> shown, int more) {
    }

    private ExplorerRules() {
    }

    // The registry

    /** Every product of the registry, in usage order, most widely used first. */
    static List<Product> products(ExplorerFront front) {
        Optional<ExplorerFront.Profile> profile = front.profile();
        Set<String> optional = new HashSet<>(profile.map(ExplorerFront.Profile::off).orElse(List.of()));
        Set<String> discontinued = new HashSet<>(
                profile.map(ExplorerFront.Profile::discontinued).orElse(List.of()));
        Set<String> fixed = new HashSet<>(profile.map(ExplorerFront.Profile::fixed).orElse(List.of()));
        Optional<String> shape = profile.flatMap(ExplorerFront.Profile::switchesShape);
        var products = new ArrayList<Product>();
        List<String> names = front.moduleNames();
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            products.add(new Product(index, name, optional.contains(name), discontinued.contains(name),
                    fixed.contains(name), shape.filter(name::equals).isPresent()));
        }
        return products;
    }

    /**
     * The findings of the measurement a line is read from: the one without
     * Spring when the Spring Boot starter is off and the line has one, the
     * full one otherwise.
     */
    static List<Finding> measured(Line line, boolean springOff) {
        if (!springOff || Boolean.TRUE.equals(line.noSpring()) || line.withoutSpring().isEmpty()) {
            return line.findings();
        }
        var without = new HashSet<>(line.withoutSpring());
        return line.findings().stream().filter(finding -> without.contains(finding.cve())).toList();
    }

    // Groups

    /**
     * The group a finding belongs to. Whose it is comes first, because a
     * finding that is somebody else's is not ours to judge, rule 6.6.6, and the
     * owner the data records is already the one every route agrees on, rule
     * 6.6.5a.
     */
    static Group group(Finding finding) {
        String owner = finding.owner().orElse("");
        if ("build".equals(owner)) {
            return Group.BUILD;
        }
        if ("spring-boot".equals(owner)) {
            return Group.SPRING;
        }
        Optional<String> verdict = finding.verdict().filter(word -> !"in_triage".equals(word));
        if (verdict.isEmpty()) {
            return Group.UNJUDGED;
        }
        return "risk".equals(verdict.get()) ? Group.RISK : Group.ANSWERED;
    }

    /** The findings by group, worst first in each, groups with nothing left out, rule 6.6.3. */
    static Map<Group, List<Finding>> grouped(List<Finding> findings) {
        var groups = new LinkedHashMap<Group, List<Finding>>();
        for (Group group : Group.values()) {
            var members = new ArrayList<Finding>();
            for (Finding finding : findings) {
                if (group(finding) == group) {
                    members.add(finding);
                }
            }
            if (!members.isEmpty()) {
                members.sort(WORST_FIRST);
                groups.put(group, members);
            }
        }
        return groups;
    }

    /** Worst first, then by id so the order is stable, rule 6.4.22 without its age step, D-81. */
    static final Comparator<Finding> WORST_FIRST = Comparator
            .comparingInt((Finding finding) -> severityRank(finding.severity())).thenComparing(Finding::cve);

    static int severityRank(Optional<String> severity) {
        int rank = severity.map(word -> SEVERITY_ORDER.indexOf(word.toLowerCase(Locale.ROOT))).orElse(-1);
        return rank < 0 ? SEVERITY_ORDER.size() : rank;
    }

    /** A group cut at {@link #GROUP_CAP}, with the remainder counted. */
    static Capped capped(List<Finding> findings) {
        if (findings.size() <= GROUP_CAP) {
            return new Capped(findings, 0);
        }
        return new Capped(findings.subList(0, GROUP_CAP), findings.size() - GROUP_CAP);
    }

    // Badges

    /**
     * What Vaadin's own half of a list of findings says: the ones Vaadin can
     * move, owner {@code vaadin} and a version Spring Boot does not pin, D-205.
     */
    static Badge vaadinBadge(List<Finding> findings) {
        return worst(findings.stream().filter(ExplorerRules::ours).toList());
    }

    /** What the other half says: Spring Boot's own findings and the versions Spring Boot pins, D-205. */
    static Badge springBadge(List<Finding> findings) {
        return worst(findings.stream().filter(ExplorerRules::theirs).toList());
    }

    /** The verdicts that answer a finding. Anything else that is not a risk is not assessed yet. */
    static final Set<String> RULED_OUT = Set.of("not_affected", "configuration", "no_risk", "false_positive",
            "fixed");

    /** The worst state of a set of findings: a named risk, then one nobody judged, then answered. */
    static Badge worst(List<Finding> findings) {
        Badge worst = Badge.NOT_AFFECTED;
        for (Finding finding : findings) {
            Badge state = stateOf(finding);
            if (state.ordinal() < worst.ordinal()) {
                worst = state;
            }
        }
        return worst;
    }

    /** The state one finding gives a release. */
    static Badge stateOf(Finding finding) {
        String verdict = finding.verdict().orElse("");
        if ("risk".equals(verdict)) {
            return Badge.VULNERABLE;
        }
        return RULED_OUT.contains(verdict) ? Badge.NOT_AFFECTED : Badge.UNJUDGED;
    }

    /** A finding a Vaadin release moves: Vaadin's, on a version the release itself publishes. */
    static boolean ours(Finding finding) {
        return "vaadin".equals(finding.owner().orElse("")) && !Boolean.TRUE.equals(finding.springPins());
    }

    /** A finding the reader's Spring Boot brings or pins, which no Vaadin release moves. */
    static boolean theirs(Finding finding) {
        String owner = finding.owner().orElse("");
        return "spring-boot".equals(owner) || ("vaadin".equals(owner) && Boolean.TRUE.equals(finding.springPins()));
    }

    // Search

    /**
     * The version a search names in full, {@code 24.9.21}, {@code V24.9.21} or
     * {@code Vaadin 24.9.21}, which is a jump to the release rather than a
     * filter.
     */
    static Optional<VersionNumber> fullVersion(String query) {
        var matcher = FULL_VERSION.matcher(query.strip());
        return matcher.matches() ? Optional.of(VersionNumber.of(matcher.group(1))) : Optional.empty();
    }

    // Small parts

    /** The products a finding arrives through, in registry order, unknown positions left out. */
    static List<Product> through(Finding finding, List<Product> products) {
        return finding.modules().stream().filter(index -> index >= 0 && index < products.size()).distinct()
                .sorted().map(products::get).toList();
    }
}
