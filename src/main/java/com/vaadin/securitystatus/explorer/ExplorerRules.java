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
 * The explorer's questions answered without components: which products a
 * reader can turn off, what is hidden when they do, the marginal counts, the
 * five groups, the badges and the search. Everything here reads
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
     * The names an address asked to turn off that are products a reader can
     * turn off on this data, in registry order. Anything else is dropped,
     * rule 6.2.7.
     */
    static List<String> known(List<String> asked, List<Product> products) {
        var wanted = new HashSet<>(asked);
        return products.stream().filter(Product::switchable).map(Product::name).filter(wanted::contains).toList();
    }

    /** Whether the product that switches a line to its measurement without Spring is off. */
    static boolean springOff(Set<String> off, List<Product> products) {
        return products.stream().anyMatch(product -> product.switchesShape() && off.contains(product.name()));
    }

    // What is shown

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

    /**
     * Whether a finding is hidden: only when every product it arrives through
     * is off, and never when no product is recorded, rules 6.5.11 and 6.5.12.
     */
    static boolean hidden(Finding finding, Set<Integer> offIndexes) {
        return !finding.modules().isEmpty() && offIndexes.containsAll(finding.modules());
    }

    /** The findings of a line a reader with these products off sees. */
    static List<Finding> visible(Line line, Set<String> off, List<Product> products) {
        Set<Integer> offIndexes = indexes(off, products);
        return measured(line, springOff(off, products)).stream().filter(finding -> !hidden(finding, offIndexes))
                .toList();
    }

    /**
     * The marginal count of a product over some lines, rule 6.5.9: on a product
     * that is on, how many findings turning it off would hide; on one that is
     * off, how many turning it back on would show. It is recomputed against
     * whatever else is off.
     */
    static int marginal(Product product, List<Line> lines, Set<String> off, List<Product> products) {
        boolean isOff = off.contains(product.name());
        var toggled = new HashSet<>(off);
        if (isOff) {
            toggled.remove(product.name());
        } else {
            toggled.add(product.name());
        }
        int count = 0;
        for (Line line : lines) {
            Set<String> now = cves(visible(line, off, products));
            Set<String> then = cves(visible(line, toggled, products));
            if (isOff) {
                then.removeAll(now);
                count += then.size();
            } else {
                now.removeAll(then);
                count += now.size();
            }
        }
        return count;
    }

    /** What the tri state control does when clicked: everything on, or, from everything on, the optional ones off. */
    static Set<String> allToggled(Set<String> off, List<Product> products) {
        if (!off.isEmpty()) {
            return Set.of();
        }
        var optional = new LinkedHashSet<String>();
        products.stream().filter(Product::switchable).filter(Product::optional).map(Product::name)
                .forEach(optional::add);
        return optional;
    }

    /** True when every product a reader can turn off is off. */
    static boolean allOff(Set<String> off, List<Product> products) {
        return products.stream().filter(Product::switchable).allMatch(product -> off.contains(product.name()));
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

    /** What Vaadin's own findings say about a line: everything that is neither Spring Boot's nor only the build's. */
    static Badge vaadinBadge(List<Finding> visible) {
        return badge(visible.stream().filter(finding -> {
            Group group = group(finding);
            return group != Group.SPRING && group != Group.BUILD;
        }).toList());
    }

    /** What the findings that arrive through the reader's Spring Boot say. */
    static Badge springBadge(List<Finding> visible) {
        return badge(visible.stream().filter(finding -> group(finding) == Group.SPRING).toList());
    }

    private static Badge badge(List<Finding> findings) {
        if (findings.stream().anyMatch(finding -> finding.verdict().filter("risk"::equals).isPresent())) {
            return Badge.VULNERABLE;
        }
        if (findings.stream()
                .anyMatch(finding -> finding.verdict().filter(word -> !"in_triage".equals(word)).isEmpty())) {
            return Badge.UNJUDGED;
        }
        return Badge.NOT_AFFECTED;
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

    /** Whether a search names the line itself, by its minor or its version. */
    static boolean namesLine(Line line, String query) {
        String asked = normal(query);
        return !asked.isEmpty() && (line.line().contains(asked) || line.version().toString().contains(asked));
    }

    /**
     * The advisories a search names: by id, or through a dependency whose name
     * contains it. Empty when the search is empty.
     */
    static Set<String> named(ExplorerFront front, String query) {
        String asked = normal(query);
        var advisories = new HashSet<String>();
        if (asked.isEmpty()) {
            return advisories;
        }
        for (Line line : front.lines()) {
            for (Finding finding : line.findings()) {
                if (finding.cve().toLowerCase(Locale.ROOT).contains(asked)) {
                    advisories.add(finding.cve());
                }
            }
        }
        front.packages().forEach((name, ids) -> {
            if (name.toLowerCase(Locale.ROOT).contains(asked)) {
                advisories.addAll(ids);
            }
        });
        return advisories;
    }

    /**
     * The findings a search leaves of a line: all of them when it is empty or
     * names the line, the ones it names otherwise.
     */
    static List<Finding> searched(Line line, List<Finding> findings, String query, Set<String> named) {
        if (normal(query).isEmpty() || namesLine(line, query)) {
            return findings;
        }
        return findings.stream().filter(finding -> named.contains(finding.cve())).toList();
    }

    static String normal(String query) {
        return query.strip().toLowerCase(Locale.ROOT);
    }

    // Small parts

    static Set<Integer> indexes(Set<String> off, List<Product> products) {
        var indexes = new HashSet<Integer>();
        for (Product product : products) {
            if (off.contains(product.name())) {
                indexes.add(product.index());
            }
        }
        return indexes;
    }

    static Set<String> cves(List<Finding> findings) {
        var ids = new HashSet<String>();
        findings.forEach(finding -> ids.add(finding.cve()));
        return ids;
    }

    /** The products a finding arrives through, in registry order, unknown positions left out. */
    static List<Product> through(Finding finding, List<Product> products) {
        return finding.modules().stream().filter(index -> index >= 0 && index < products.size()).distinct()
                .sorted().map(products::get).toList();
    }
}
