package com.vaadin.securitystatus.status;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CaseCatalog;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * The case catalogue without components: which group a case is in, where its
 * example lives in this application's addresses, and whether the data still
 * shows what the case names. The catalogue writes its examples in the source
 * page's hash grammar, {@code #version/7.7.0?boot=1} and
 * {@code #cve/CVE-2016-1000027/23.6.19}, which maps one to one onto the
 * routes here.
 */
final class CaseRules {

    /** The source page's three groups, in the order it shows them. */
    enum Group {
        IMPOSSIBLE, NONE_TODAY, RENDERABLE
    }

    /** Where a case's example opens: the view, its route parameters and its query flags. */
    record Target(Class<? extends Component> view, RouteParameters parameters, QueryParameters query) {
    }

    /** One case, with the group it is shown in and the example it links to, when it has a live one. */
    record Placed(CaseCatalog.Case item, Group group, Optional<Target> target) {
    }

    private static final String VERSION_PREFIX = "#version/";
    private static final String CVE_PREFIX = "#cve/";

    private CaseRules() {
    }

    /**
     * Every case in its group, the most frequent first as the prototype orders
     * them, ties in the catalogue's own order, D-195.
     */
    static Map<Group, List<Placed>> grouped(CaseCatalog catalog, CasesIndex index) {
        var groups = new EnumMap<Group, List<Placed>>(Group.class);
        for (Group group : Group.values()) {
            groups.put(group, new ArrayList<>());
        }
        for (CaseCatalog.Case item : catalog.cases()) {
            Placed placed = place(item, index);
            groups.get(placed.group()).add(placed);
        }
        groups.values().forEach(cases -> cases.sort(java.util.Comparator.comparingInt(
                (Placed placed) -> placed.item().count() == null ? 0 : placed.item().count()).reversed()));
        return groups;
    }

    /**
     * A case the catalogue calls impossible stays there. Any other case is
     * renderable only while its example parses and the data still shows what
     * the case names; otherwise it is none today, never a broken link.
     */
    static Placed place(CaseCatalog.Case item, CasesIndex index) {
        if (item.status().filter("impossible"::equals).isPresent()) {
            return new Placed(item, Group.IMPOSSIBLE, Optional.empty());
        }
        Optional<Target> target = item.example().flatMap(CaseRules::target)
                .filter(found -> exhibits(item.id(), item.example().orElseThrow(), index));
        return target.map(found -> new Placed(item, Group.RENDERABLE, Optional.of(found)))
                .orElseGet(() -> new Placed(item, Group.NONE_TODAY, Optional.empty()));
    }

    /** The route an example in the source page's grammar names, or empty for one this application has no screen for. */
    static Optional<Target> target(String example) {
        String path = example;
        String flags = "";
        int question = example.indexOf('?');
        if (question >= 0) {
            path = example.substring(0, question);
            flags = example.substring(question + 1);
        }
        // The catalogue's flags are read and written by the one writer of addresses.
        QueryParameters query = Address.parse(path, flags).query();
        if (path.startsWith(VERSION_PREFIX)) {
            String version = path.substring(VERSION_PREFIX.length());
            if (version.isEmpty() || version.contains("/")) {
                return Optional.empty();
            }
            return Optional.of(new Target(VersionView.class, new RouteParameters(VersionView.VERSION, version), query));
        }
        if (path.startsWith(CVE_PREFIX)) {
            String[] parts = path.substring(CVE_PREFIX.length()).split("/");
            if (parts.length == 0 || parts.length > 2 || parts[0].isEmpty()) {
                return Optional.empty();
            }
            RouteParameters parameters = parts.length == 2
                    ? new RouteParameters(Map.of(CveView.IDS, parts[0], CveView.VERSION, parts[1]))
                    : new RouteParameters(CveView.IDS, parts[0]);
            return Optional.of(new Target(CveView.class, parameters, query));
        }
        return Optional.empty();
    }

    /**
     * Whether the published data still shows the case at its example. The
     * release or the advisory has to be one the index knows, and where the
     * index carries the fact the case is about, that fact has to hold.
     */
    static boolean exhibits(String caseId, String example, CasesIndex index) {
        Optional<Target> target = target(example);
        if (target.isEmpty()) {
            return false;
        }
        RouteParameters parameters = target.get().parameters();
        if (target.get().view() == VersionView.class) {
            Optional<CasesIndex.Summary> summary = summary(index, parameters.get(VersionView.VERSION).orElse(""));
            return summary.filter(versionFact(caseId)).isPresent();
        }
        Optional<String> version = parameters.get(CveView.VERSION);
        if (version.isPresent() && summary(index, version.get()).isEmpty()) {
            return false;
        }
        String advisory = parameters.get(CveView.IDS).orElse("");
        return Optional.ofNullable(index.advisories().get(advisory)).filter(advisoryFact(caseId, advisory))
                .isPresent();
    }

    private static Optional<CasesIndex.Summary> summary(CasesIndex index, String version) {
        try {
            return index.summaryOf(VersionNumber.of(version));
        } catch (IllegalArgumentException notAVersion) {
            return Optional.empty();
        }
    }

    private static Predicate<CasesIndex.Summary> versionFact(String caseId) {
        return switch (caseId) {
            case "version.secure" -> row -> state(row, "secure");
            case "version.fix_available" -> row -> state(row, "fix_available");
            case "version.partial" -> row -> state(row, "unresolved") && row.upgradeTo().isPresent();
            case "version.stuck" -> row -> state(row, "unresolved") && row.upgradeTo().isEmpty();
            case "version.triage" -> row -> positive(row.triage());
            case "version.theirs" -> row -> positive(row.theirs());
            case "version.spring_pinned" -> row -> positive(row.springPinned());
            case "version.discontinued" -> row -> positive(row.discontinued());
            case "version.from_build" -> row -> positive(row.fromBuild());
            case "version.ruled_out" -> row -> positive(row.ruledOut());
            default -> row -> true;
        };
    }

    private static Predicate<CasesIndex.AdvisoryCatalogEntry> advisoryFact(String caseId, String advisory) {
        String applicability = "cve.applicability.";
        if (caseId.startsWith(applicability)) {
            String wanted = caseId.substring(applicability.length());
            return entry -> entry.applicability().filter(wanted::equals).isPresent();
        }
        return switch (caseId) {
            case "cve.vaadin_advisory" -> entry -> Boolean.TRUE.equals(entry.vaadinAdvisory());
            case "cve.historical" -> entry -> Boolean.FALSE.equals(entry.current());
            case "cve.ghsa_only" -> entry -> advisory.startsWith("GHSA-");
            default -> entry -> true;
        };
    }

    private static boolean state(CasesIndex.Summary row, String wanted) {
        return row.state().filter(wanted::equals).isPresent();
    }

    private static boolean positive(Integer count) {
        return count != null && count > 0;
    }
}
