package com.vaadin.securitystatus.team.assistant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.orchestrator.RequestListener;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ToolException;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CveAnswer;
import com.vaadin.securitystatus.data.model.Verdict;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VersionAnswer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The five tools of the chat assistant, each a thin reading of
 * {@link SecurityDataService} that returns compact JSON and refuses through
 * {@link ToolException} with a sentence the model can correct from.
 *
 * <p>
 * Framework agnostic tools through a controller rather than Spring AI's
 * {@code @Tool} methods, because only a controller's tools relay a
 * {@code ToolException} and nothing else: a vendor annotated tool would hand
 * the model the raw message of any exception.
 *
 * <p>
 * A field the published document does not carry is left out of the result,
 * never sent empty, rule 4.
 */
public final class AssistantTools implements AIController {

    /** The most lines one page of {@code openFindings} holds. */
    static final int PAGE = 25;
    /** The most packages {@code queueHead} gives. */
    static final int QUEUE_MOST = 20;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SecurityDataService data;
    private final AssistantTail tail;
    private final AtomicInteger callsThisTurn = new AtomicInteger();
    private volatile String user = "";

    public AssistantTools(SecurityDataService data, AssistantTail tail) {
        this.data = data;
        this.tail = tail;
    }

    /** Who the turn is for, captured on the request thread because a tool may run on another one. */
    public void forUser(String name) {
        user = name;
    }

    /** How many tool calls the running or the last turn made. */
    public int callsThisTurn() {
        return callsThisTurn.get();
    }

    @Override
    public void onRequest(RequestListener.RequestEvent event) {
        callsThisTurn.set(0);
    }

    @Override
    public void onResponse(ResponseListener.ResponseEvent event) {
        // Nothing staged: every tool only reads.
    }

    @Override
    public List<LLMProvider.ToolSpec> getTools() {
        return List.of(tool("versionStatus", this::versionStatus), tool("cveStatus", this::cveStatus),
                tool("upgradeTarget", this::upgradeTarget), tool("openFindings", this::openFindings),
                tool("queueHead", this::queueHead));
    }

    private LLMProvider.ToolSpec tool(String name, Function<JsonNode, ObjectNode> body) {
        return new LLMProvider.ToolSpec() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return Prompts.toolDescription(name);
            }

            @Override
            public String getParametersSchema() {
                return Prompts.toolParameters(name);
            }

            @Override
            public String execute(JsonNode arguments) {
                callsThisTurn.incrementAndGet();
                String call = name + " " + arguments;
                try {
                    String result = JSON.writeValueAsString(body.apply(arguments));
                    tail.record("chat", user, AssistantTail.Kind.TOOL, call + " -> " + result);
                    return result;
                } catch (ToolException refused) {
                    tail.record("chat", user, AssistantTail.Kind.REFUSAL, call + " -> " + refused.getMessage());
                    throw refused;
                } catch (DataUnreachableException unreachable) {
                    String sentence = Prompts.refusal("unreachable");
                    tail.record("chat", user, AssistantTail.Kind.REFUSAL, call + " -> " + sentence);
                    throw new ToolException(sentence, unreachable);
                }
            }
        };
    }

    // versionStatus

    ObjectNode versionStatus(JsonNode arguments) {
        VersionNumber version = version(arguments);
        CasesIndex index = data.index();
        Optional<VersionAnswer> document = data.version(version);
        ObjectNode out = JSON.createObjectNode();
        if (document.isPresent()) {
            VersionAnswer answer = document.get();
            out.put("source", "cases/v/" + version + ".json");
            out.put("version", version.toString());
            answer.line().ifPresent(line -> out.put("line", line));
            answer.state().ifPresent(state -> out.put("state", state));
            answer.scannedAt().ifPresent(at -> out.put("scanned_at", at.toString()));
            ObjectNode open = out.putObject("open");
            open.put("risk", answer.risk().size());
            open.put("triage", answer.triage().size());
            answer.upgrade().ifPresent(upgrade -> {
                ObjectNode target = out.putObject("upgrade");
                upgrade.to().ifPresent(to -> target.put("to", to.toString()));
                putIfKnown(target, "clears", upgrade.clears());
                putIfKnown(target, "remains", upgrade.remains());
            });
            return out;
        }
        CasesIndex.Summary summary = summary(index, version);
        out.put("source", "cases/index.json");
        out.put("generated_at", index.generatedAt().toString());
        out.put("version", version.toString());
        summary.line().ifPresent(line -> out.put("line", line));
        summary.state().ifPresent(state -> out.put("state", state));
        summary.scannedAt().ifPresent(at -> out.put("scanned_at", at.toString()));
        ObjectNode open = out.putObject("open");
        putIfKnown(open, "risk", summary.risk());
        putIfKnown(open, "triage", summary.triage());
        summary.upgradeTo().ifPresent(to -> out.putObject("upgrade").put("to", to.toString()));
        return out;
    }

    // upgradeTarget

    ObjectNode upgradeTarget(JsonNode arguments) {
        VersionNumber version = version(arguments);
        CasesIndex index = data.index();
        Optional<VersionAnswer> document = data.version(version);
        ObjectNode out = JSON.createObjectNode();
        out.put("version", version.toString());
        newestOfLine(index, version.line()).ifPresent(newest -> out.put("newest_of_line", newest.toString()));
        if (document.isPresent()) {
            out.put("source", "cases/v/" + version + ".json");
            document.get().upgrade().ifPresent(upgrade -> {
                upgrade.to().ifPresent(to -> out.put("to", to.toString()));
                putIfKnown(out, "clears", upgrade.clears());
                putIfKnown(out, "remains", upgrade.remains());
                if (upgrade.measured() != null) {
                    out.put("target_measured", upgrade.measured());
                }
            });
            ArrayNode fixable = out.putArray("cleared_by_upgrade");
            document.get().fixable().forEach(fixable::add);
            ArrayNode stuck = out.putArray("remaining_after_upgrade");
            document.get().stuck().forEach(stuck::add);
            return out;
        }
        CasesIndex.Summary summary = summary(index, version);
        out.put("source", "cases/index.json");
        out.put("generated_at", index.generatedAt().toString());
        summary.upgradeTo().ifPresent(to -> out.put("to", to.toString()));
        return out;
    }

    // openFindings

    public ObjectNode openFindings(JsonNode arguments) {
        VersionNumber version = version(arguments);
        CasesIndex index = data.index();
        int offset = Math.max(0, arguments.path("offset").asInt(0));
        int limit = Math.clamp(arguments.path("limit").asInt(PAGE), 1, PAGE);
        Optional<VersionAnswer> document = data.version(version);
        ObjectNode out = JSON.createObjectNode();
        out.put("version", version.toString());
        if (document.isEmpty()) {
            CasesIndex.Summary summary = summary(index, version);
            out.put("source", "cases/index.json");
            out.put("generated_at", index.generatedAt().toString());
            putIfKnown(out, "risk_count", summary.risk());
            putIfKnown(out, "triage_count", summary.triage());
            out.put("lists_published", false);
            return out;
        }
        VersionAnswer answer = document.get();
        out.put("source", "cases/v/" + version + ".json");
        answer.scannedAt().ifPresent(at -> out.put("scanned_at", at.toString()));
        var all = new ArrayList<ObjectNode>();
        answer.risk().forEach(id -> all.add(finding(index, answer, id, "risk")));
        answer.triage().forEach(id -> all.add(finding(index, answer, id, "triage")));
        out.put("total", all.size());
        out.put("offset", offset);
        ArrayNode page = out.putArray("findings");
        all.stream().skip(offset).limit(limit).forEach(page::add);
        if (offset + limit < all.size()) {
            out.put("next_offset", offset + limit);
        }
        return out;
    }

    private static ObjectNode finding(CasesIndex index, VersionAnswer answer, String id, String list) {
        ObjectNode line = JSON.createObjectNode();
        line.put("id", id);
        line.put("list", list);
        Optional.ofNullable(index.advisories().get(id)).flatMap(AdvisoryCatalogEntry::severity)
                .ifPresent(severity -> line.put("severity", severity));
        Optional.ofNullable(answer.verdicts().get(id)).flatMap(Verdict::state)
                .ifPresent(state -> line.put("verdict", state));
        return line;
    }

    // cveStatus

    ObjectNode cveStatus(JsonNode arguments) {
        String id = arguments.path("id").asString("").strip().toUpperCase(java.util.Locale.ROOT);
        if (id.isEmpty()) {
            throw new ToolException(Prompts.refusal("idMissing"));
        }
        String asked = arguments.path("version").asString("").strip();
        Optional<CveAnswer> document = id.matches("[A-Z0-9][A-Z0-9-]{0,60}") ? data.cve(id) : Optional.empty();
        CasesIndex index = data.index();
        @Nullable
        AdvisoryCatalogEntry entry = index.advisories().get(id);
        if (document.isEmpty() && entry == null) {
            throw new ToolException(Prompts.refusal("unknownAdvisory", id));
        }
        ObjectNode out = JSON.createObjectNode();
        out.put("cve", id);
        if (!asked.isEmpty()) {
            return onVersion(out, id, VersionNumber.of(asked), document, index);
        }
        if (document.isPresent()) {
            CveAnswer answer = document.get();
            out.put("source", "cases/cve/" + id + ".json");
            answer.severity().ifPresent(severity -> out.put("severity", severity));
            answer.summary().ifPresent(summary -> out.put("summary", summary));
            answer.applicability().ifPresent(verdict -> out.put("verdict", verdict));
            answer.fixOwner().ifPresent(owner -> out.put("fix_owner", owner));
            ArrayNode lines = out.putArray("lines");
            answer.coverage().forEach((line, coverage) -> {
                ObjectNode row = lines.addObject();
                row.put("line", line);
                coverage.version().ifPresent(version -> row.put("measured_release", version.toString()));
                coverage.status().ifPresent(status -> row.put("status", status));
                coverage.fixedIn().ifPresent(fixed -> row.put("fixed_in", fixed.toString()));
                coverage.fixedInEnterprise().ifPresent(fixed -> row.put("fixed_in_enterprise", fixed.toString()));
            });
            return out;
        }
        out.put("source", "cases/index.json");
        out.put("generated_at", index.generatedAt().toString());
        entry.severity().ifPresent(severity -> out.put("severity", severity));
        entry.applicability().ifPresent(verdict -> out.put("verdict", verdict));
        entry.fixOwner().ifPresent(owner -> out.put("fix_owner", owner));
        ArrayNode lines = out.putArray("lines");
        index.fixedIn().getOrDefault(id, Map.of()).forEach((line, fixed) -> {
            ObjectNode row = lines.addObject();
            row.put("line", line);
            fixed.fixedIn().ifPresent(version -> row.put("fixed_in", version.toString()));
        });
        return out;
    }

    private ObjectNode onVersion(ObjectNode out, String id, VersionNumber version, Optional<CveAnswer> document,
            CasesIndex index) {
        out.put("version", version.toString());
        Optional<Verdict> fromAdvisory = document.map(CveAnswer::onVersions).map(on -> on.get(version.toString()));
        if (fromAdvisory.isPresent()) {
            out.put("source", "cases/cve/" + id + ".json");
            verdict(out, fromAdvisory.get());
            return out;
        }
        Optional<VersionAnswer> release = data.version(version);
        if (release.isEmpty() && !index.summary().containsKey(version.toString())) {
            throw new ToolException(notMeasured(index, version));
        }
        Optional<Verdict> fromRelease = release.map(answer -> answer.verdicts().get(id));
        if (fromRelease.isEmpty()) {
            throw new ToolException(Prompts.refusal("notOnVersion", id, version));
        }
        out.put("source", "cases/v/" + version + ".json");
        verdict(out, fromRelease.get());
        return out;
    }

    private static void verdict(ObjectNode out, Verdict verdict) {
        verdict.state().ifPresent(state -> out.put("state", state));
        ArrayNode assessments = out.putArray("assessments");
        verdict.assessments().forEach(assessments::add);
    }

    // queueHead

    public ObjectNode queueHead(JsonNode arguments) {
        JsonNode asked = arguments.path("n");
        if (!asked.isIntegralNumber() || asked.asInt() < 1 || asked.asInt() > QUEUE_MOST) {
            throw new ToolException(Prompts.refusal("queueRange", asked.isMissingNode() ? "" : asked.toString()));
        }
        VerdictQueue queue = data.queue();
        ObjectNode out = JSON.createObjectNode();
        out.put("source", "data/queue.json");
        queue.generatedAt().ifPresent(at -> out.put("generated_at", at.toString()));
        out.put("packages_in_queue", queue.packages().size());
        ArrayNode head = out.putArray("head");
        queue.packages().stream().limit(asked.asInt()).forEach(entry -> {
            ObjectNode row = head.addObject();
            row.put("package", entry.packageName());
            putIfKnown(row, "closes", entry.closes());
            row.put("advisories", entry.cves().size());
            ArrayNode lines = row.putArray("lines");
            entry.lines().forEach(lines::add);
        });
        return out;
    }

    // Shared

    private VersionNumber version(JsonNode arguments) {
        String text = arguments.path("version").asString("").strip();
        if (text.isEmpty()) {
            throw new ToolException(Prompts.refusal("versionMissing"));
        }
        return VersionNumber.of(text);
    }

    /** The index's summary of a measured release, or the refusal that names the nearest measured one. */
    private static CasesIndex.Summary summary(CasesIndex index, VersionNumber version) {
        CasesIndex.Summary summary = index.summary().get(version.toString());
        if (summary == null) {
            throw new ToolException(notMeasured(index, version));
        }
        return summary;
    }

    static String notMeasured(CasesIndex index, VersionNumber version) {
        return nearestMeasured(index, version)
                .map(nearest -> Prompts.refusal("notMeasured", version, nearest))
                .orElseGet(() -> Prompts.refusal("notMeasuredLine", version, version.line(),
                        String.join(", ", index.lines().stream().map(CasesIndex.Line::line).toList())));
    }

    /**
     * The measured release nearest to an unmeasured one: in its line, the
     * newest measured release not newer than it, else the oldest newer one.
     * Empty when nothing of its line was measured.
     */
    static Optional<VersionNumber> nearestMeasured(CasesIndex index, VersionNumber version) {
        List<VersionNumber> line = index.summary().keySet().stream().map(VersionNumber::of)
                .filter(measured -> measured.line().equals(version.line())).sorted().toList();
        Optional<VersionNumber> below = line.stream().filter(measured -> measured.compareTo(version) <= 0)
                .max(Comparator.naturalOrder());
        return below.or(() -> line.stream().filter(measured -> measured.compareTo(version) > 0).findFirst());
    }

    private static Optional<VersionNumber> newestOfLine(CasesIndex index, String line) {
        return index.lines().stream().filter(entry -> entry.line().equals(line)).map(CasesIndex.Line::version)
                .findFirst();
    }

    private static void putIfKnown(ObjectNode node, String field, @Nullable Integer value) {
        if (value != null) {
            node.put(field, value.intValue());
        }
    }
}
