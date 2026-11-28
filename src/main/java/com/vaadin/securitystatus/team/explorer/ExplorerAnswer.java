package com.vaadin.securitystatus.team.explorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.History;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.team.explorer.ExplorerRules.Badge;
import com.vaadin.securitystatus.team.explorer.ExplorerRules.Product;

/**
 * What the explorer's front door answers, in the prototype's terms and
 * without components, D-205 to D-209: which products a reader is offered and
 * which are on, which findings are the reader's, the two halves of a series
 * and their three groups, the counts on the rings and on the chips, and how a
 * search is read. Everything here reads {@code data/front.json}.
 */
public final class ExplorerAnswer {

    private static final Pattern NUMBER = Pattern.compile("^[0-9][0-9.]*$");

    private ExplorerAnswer() {
    }

    // The catalogue and the selection

    /**
     * The products a reader is offered, D-206: the profile's {@code on} and
     * {@code off} lists narrowed to what {@code module_names} knows, the fixed
     * ones first, then the choices, then what Vaadin no longer develops.
     */
    public static List<Product> catalogue(List<Product> registry, ExplorerFront front) {
        var named = new LinkedHashSet<String>();
        front.profile().ifPresent(profile -> {
            named.addAll(profile.on());
            named.addAll(profile.off());
        });
        var known = registry.stream().filter(product -> named.contains(product.name())).toList();
        var ordered = new ArrayList<Product>();
        var byName = new java.util.LinkedHashMap<String, Product>();
        known.forEach(product -> byName.put(product.name(), product));
        for (String name : named) {
            Product product = byName.get(name);
            if (product != null && product.fixed()) {
                ordered.add(product);
            }
        }
        for (String name : named) {
            Product product = byName.get(name);
            if (product != null && !product.fixed() && !product.discontinued()) {
                ordered.add(product);
            }
        }
        for (String name : named) {
            Product product = byName.get(name);
            if (product != null && !product.fixed() && product.discontinued()) {
                ordered.add(product);
            }
        }
        return ordered;
    }

    /** What is off when the address says nothing: what Vaadin no longer develops, D-206. */
    public static Set<String> defaultOff(List<Product> catalogue) {
        var off = new LinkedHashSet<String>();
        catalogue.stream().filter(product -> product.discontinued() && !product.fixed())
                .forEach(product -> off.add(product.name()));
        return off;
    }

    /** The names an address may turn off: every offered product that is not fixed, in catalogue order. */
    public static List<String> known(List<String> asked, List<Product> catalogue) {
        var wanted = new HashSet<>(asked);
        return catalogue.stream().filter(Product::switchable).map(Product::name).filter(wanted::contains).toList();
    }

    /** Every product the select all box turns off: the ones counted by default that a reader can switch. */
    public static List<String> countedByDefault(List<Product> catalogue) {
        return catalogue.stream().filter(Product::switchable).filter(product -> !product.discontinued())
                .map(Product::name).toList();
    }

    /**
     * What one reading of the page is about: the products on, the products the
     * shape cannot contain, and whether it is the shape without Spring.
     */
    public record Shape(List<String> names, Set<String> offered, Set<String> on, Set<String> cannot, boolean plain,
            String starter) {

        /** The products a finding's route may not name on this line. */
        public List<String> hidden(Line line) {
            return plain ? line.needsSpring() : List.of();
        }

        /** The same reading with one more product on, as a chip's count asks it. */
        public Shape with(String name) {
            var more = new LinkedHashSet<>(on);
            more.add(name);
            boolean stillPlain = plain && !name.equals(starter);
            return new Shape(names, offered, more, stillPlain ? cannot : Set.of(), stillPlain, starter);
        }
    }

    /** The shape the page reads for these products off. */
    public static Shape shape(ExplorerFront front, List<Product> catalogue, Set<String> off) {
        String starter = catalogue.stream().filter(Product::switchesShape).map(Product::name).findFirst()
                .orElse("");
        boolean plain = !starter.isEmpty() && off.contains(starter);
        var cannot = new LinkedHashSet<String>();
        if (plain) {
            front.lines().forEach(line -> line.needsSpring().stream().filter(name -> !name.equals(starter))
                    .forEach(cannot::add));
        }
        var offered = new LinkedHashSet<String>();
        var on = new LinkedHashSet<String>();
        for (Product product : catalogue) {
            offered.add(product.name());
            if (product.fixed() || (!off.contains(product.name()) && !cannot.contains(product.name()))) {
                on.add(product.name());
            }
        }
        return new Shape(front.moduleNames(), offered, on, cannot, plain, starter);
    }

    // Findings

    /**
     * The products a finding arrives through, as names, without the ones the
     * shape cannot contain. Empty when no route was ever recorded, which is not
     * the same answer as a route through nothing this shape can have.
     */
    public static Optional<List<String>> routes(Finding finding, Shape shape, Line line) {
        return routes(finding, shape, shape.hidden(line));
    }

    /** The same, with the products the shape cannot contain on this line given. */
    public static Optional<List<String>> routes(Finding finding, Shape shape, List<String> hiddenNames) {
        if (finding.modules().isEmpty()) {
            return Optional.empty();
        }
        var hidden = new HashSet<>(hiddenNames);
        var names = new ArrayList<String>();
        for (Integer index : finding.modules()) {
            if (index != null && index >= 0 && index < shape.names().size()) {
                String name = shape.names().get(index);
                if (!hidden.contains(name) && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return Optional.of(names);
    }

    /**
     * Whether anything the reader has brings this: never recorded, so kept;
     * through nothing this shape can contain, so not; otherwise when any of
     * its products is on, or is not a product anybody is offered.
     */
    static boolean routed(Finding finding, Shape shape, Line line) {
        Optional<List<String>> through = routes(finding, shape, line);
        if (through.isEmpty()) {
            return true;
        }
        return through.get().stream().anyMatch(name -> !shape.offered().contains(name) || shape.on().contains(name));
    }

    /** A finding that is the reader's: Vaadin's own, and routed to what they have. */
    public static boolean mine(Finding finding, Shape shape, Line line) {
        return "vaadin".equals(finding.owner().orElse("")) && routed(finding, shape, line);
    }

    /** Whether the shape this page reads was measured on this line. */
    public static boolean measured(Line line, Shape shape) {
        return !shape.plain() || Boolean.TRUE.equals(line.noSpring()) || !line.withoutSpring().isEmpty();
    }

    /** The findings of the shape being read, narrowed to what a search names. */
    public static List<Finding> findingsOf(Line line, Shape shape, Asked asked) {
        List<Finding> all = ExplorerRules.measured(line, shape.plain());
        if (asked.ids().isEmpty()) {
            return all;
        }
        Set<String> ids = asked.ids().get();
        return all.stream().filter(finding -> ids.contains(finding.cve())).toList();
    }

    /** The three groups of one half, worst first in each. */
    public record Groups(List<Finding> risk, List<Finding> pending, List<Finding> ruled) {

        public int size() {
            return risk.size() + pending.size() + ruled.size();
        }
    }

    /**
     * One series split into what the reader is told, D-205: the half a Vaadin
     * release moves and the half their Spring Boot brings, each in three
     * groups; what only a product they turned off brings, counted; the
     * findings no route explains; and what only the build carries, which the
     * prototype leaves out and this application keeps as a group of its own.
     */
    public record Split(boolean measured, Groups ours, Groups theirs, int elsewhere, List<Finding> unattributed,
            List<Finding> build) {
    }

    public static Split split(Line line, Shape shape, Asked asked) {
        if (!measured(line, shape)) {
            return new Split(false, empty(), empty(), 0, List.of(), List.of());
        }
        var ours = new ArrayList<Finding>();
        var theirs = new ArrayList<Finding>();
        var build = new ArrayList<Finding>();
        var unattributed = new ArrayList<Finding>();
        int elsewhere = 0;
        for (Finding finding : findingsOf(line, shape, asked)) {
            if ("build".equals(finding.owner().orElse(""))) {
                build.add(finding);
            } else if (!ExplorerRules.ours(finding, shape.plain())) {
                theirs.add(finding);
            } else if (!mine(finding, shape, line)) {
                elsewhere++;
            } else {
                if (routes(finding, shape, line).isEmpty()) {
                    unattributed.add(finding);
                }
                ours.add(finding);
            }
        }
        build.sort(ExplorerRules.WORST_FIRST);
        return new Split(true, groups(ours), groups(theirs), elsewhere, unattributed, build);
    }

    private static Groups empty() {
        return new Groups(List.of(), List.of(), List.of());
    }

    private static Groups groups(List<Finding> findings) {
        var risk = new ArrayList<Finding>();
        var pending = new ArrayList<Finding>();
        var ruled = new ArrayList<Finding>();
        for (Finding finding : findings) {
            switch (ExplorerRules.stateOf(finding)) {
                case VULNERABLE -> risk.add(finding);
                case UNJUDGED -> pending.add(finding);
                case NOT_AFFECTED -> ruled.add(finding);
            }
        }
        risk.sort(ExplorerRules.WORST_FIRST);
        pending.sort(ExplorerRules.WORST_FIRST);
        ruled.sort(ExplorerRules.WORST_FIRST);
        return new Groups(risk, pending, ruled);
    }

    /**
     * The findings the Vaadin badge and the row's sentence are counted from:
     * the reader's own half, whatever the search, so the badge and the
     * sentence cannot disagree.
     */
    public static List<Finding> stateFindings(Line line, Shape shape) {
        return ExplorerRules.measured(line, shape.plain()).stream()
                .filter(finding -> ExplorerRules.ours(finding, shape.plain()) && mine(finding, shape, line)).toList();
    }

    /** The Vaadin badge of a series: unjudged when the shape was never measured on it. */
    public static Badge state(Line line, Shape shape) {
        if (!measured(line, shape)) {
            return Badge.UNJUDGED;
        }
        return ExplorerRules.worst(stateFindings(line, shape));
    }

    /** The Spring Boot badge: none on the shape without Spring, none when nothing is theirs. */
    public static Optional<Badge> springState(Line line, Shape shape) {
        if (shape.plain()) {
            return Optional.empty();
        }
        List<Finding> theirs = line.findings().stream().filter(ExplorerRules::theirs).toList();
        return theirs.isEmpty() ? Optional.empty() : Optional.of(ExplorerRules.worst(theirs));
    }

    // Rings and chips

    /**
     * The advisories of a series that arrive through one product, both halves,
     * for its ring. A finding only the Spring shaped application has, whose
     * route passes a product that needs Spring, is that product's and not the
     * others' on its path.
     */
    public static Set<String> arrivesThrough(Line line, String product, Shape shape, Asked asked) {
        Set<String> borne = springBorne(line, shape);
        var ids = new LinkedHashSet<String>();
        for (Finding finding : findingsOf(line, shape, asked)) {
            if (!routed(finding, shape, line)) {
                continue;
            }
            Optional<List<String>> through = routes(finding, shape, line);
            if (through.isEmpty()) {
                continue;
            }
            if (borne != null && !borne.contains(finding.cve())) {
                List<String> spring = through.get().stream().filter(line.needsSpring()::contains).toList();
                if (!spring.isEmpty()) {
                    if (spring.contains(product)) {
                        ids.add(finding.cve());
                    }
                    continue;
                }
            }
            if (through.get().contains(product)) {
                ids.add(finding.cve());
            }
        }
        return ids;
    }

    /** The ids the shape without Spring also has, or null when that cannot be told. */
    private static Set<String> springBorne(Line line, Shape shape) {
        if (shape.plain() || Boolean.TRUE.equals(line.noSpring()) || line.withoutSpring().isEmpty()) {
            return null;
        }
        return new HashSet<>(line.withoutSpring());
    }

    /**
     * A chip's number: how many different advisories arrive through the
     * product over every series the search keeps, asked as if it were on, so
     * ticking it moves the rows and not the number, D-207.
     */
    public static int chipCount(ExplorerFront front, String product, Shape shape, Asked asked) {
        Shape withIt = shape.with(product);
        var seen = new HashSet<String>();
        for (Line line : front.lines()) {
            if (asked.keeps(line)) {
                seen.addAll(arrivesThrough(line, product, withIt, asked));
            }
        }
        return seen.size();
    }

    // The search

    public enum Kind {
        NONE, VERSION, ID, PACKAGE
    }

    /**
     * What a search asked for, read once, D-209: a number is a version first,
     * then an advisory id, an advisory no series carries any more, then a
     * coordinate. {@code ids} is empty when the search names no findings;
     * {@code found} is false when nothing anywhere has that name.
     */
    public record Asked(String raw, Kind kind, Optional<Set<String>> ids, List<String> series, Optional<String> release,
            List<History> past, boolean found) {

        public static Asked nothing() {
            return new Asked("", Kind.NONE, Optional.empty(), List.of(), Optional.empty(), List.of(), true);
        }

        /** Whether a series stands under this search: only a version search names series. */
        public boolean keeps(Line line) {
            return kind != Kind.VERSION || series.contains(line.line());
        }
    }

    public static Asked asked(String text, ExplorerFront front) {
        String raw = text == null ? "" : text.strip();
        if (raw.isEmpty()) {
            return Asked.nothing();
        }
        if (NUMBER.matcher(raw).matches()) {
            var series = new ArrayList<String>();
            String exact = null;
            for (Line line : front.lines()) {
                boolean hit = line.line().equals(raw) || line.line().startsWith(raw + ".");
                for (VersionNumber release : line.releases()) {
                    String version = release.toString();
                    if (version.equals(raw)) {
                        exact = raw;
                    }
                    if (version.equals(raw) || version.startsWith(raw + ".")) {
                        hit = true;
                    }
                }
                if (hit) {
                    series.add(line.line());
                }
            }
            if (!series.isEmpty()) {
                return new Asked(raw, Kind.VERSION, Optional.empty(), series, Optional.ofNullable(exact), List.of(),
                        true);
            }
        }
        String needle = raw.toLowerCase(Locale.ROOT);
        var ids = new LinkedHashSet<String>();
        for (Line line : front.lines()) {
            for (Finding finding : line.findings()) {
                if (finding.cve().toLowerCase(Locale.ROOT).contains(needle)) {
                    ids.add(finding.cve());
                }
            }
        }
        List<History> past = front.history().stream()
                .filter(entry -> entry.cve().toLowerCase(Locale.ROOT).contains(needle)).toList();
        if (!ids.isEmpty() || !past.isEmpty()) {
            return new Asked(raw, Kind.ID, Optional.of(ids), List.of(), Optional.empty(), past, true);
        }
        front.packages().forEach((name, advisories) -> {
            if (name.toLowerCase(Locale.ROOT).contains(needle)) {
                ids.addAll(advisories);
            }
        });
        if (!ids.isEmpty()) {
            return new Asked(raw, Kind.PACKAGE, Optional.of(ids), List.of(), Optional.empty(), List.of(), true);
        }
        return new Asked(raw, Kind.NONE, Optional.of(Set.of()), List.of(), Optional.empty(), List.of(), false);
    }

    /** Whether the text is a number, which a miss answers differently from a name. */
    public static boolean number(String raw) {
        return NUMBER.matcher(raw).matches();
    }

    /** The followed series a number falls under, when it names a release we hold no scan of. */
    static Optional<Line> followedPrefix(String raw, ExplorerFront front) {
        return front.lines().stream().filter(line -> raw.startsWith(line.line() + ".")).findFirst();
    }
}
