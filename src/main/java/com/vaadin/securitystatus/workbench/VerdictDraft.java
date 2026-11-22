package com.vaadin.securitystatus.workbench;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;

/**
 * One verdict as it is being written, saved half thought or whole. Its fields
 * mirror the YAML that vaadin/cve-status keeps in {@code data/assessments/},
 * as text the form edits; {@link VerdictYaml} turns them into that YAML.
 *
 * <p>
 * Two validation groups: {@link Draft} checks only the formats, so a draft
 * saves whatever is half written, and {@link Ready} adds what the YAML needs to
 * be a verdict at all. Every format constraint accepts an empty value, because
 * absent is a legal answer for each of them.
 *
 * <p>
 * The constraint messages are bundle keys, translated where they are shown.
 */
@Entity
public class VerdictDraft {

    /** The group a draft is saved under: the formats only. */
    public interface Draft {
    }

    /** The group a verdict is exported under: the formats and the requireds. */
    public interface Ready {
    }

    /** The closed vocabulary, the six words the cve-status loader accepts, D-120. */
    public static final List<String> APPLICABILITY = List.of("risk", "not_affected", "configuration", "no_risk",
            "false_positive", "fixed");

    /** How far the analysis went, the four words the cve-status loader accepts. */
    public static final List<String> DEPTH = List.of("cursory", "reproduced", "committee", "automatic");

    /** The two verdicts that answer nothing without an impact sentence, per the domain model. */
    public static final List<String> NEED_IMPACT = List.of("risk", "no_risk");

    static final String ID_FORMAT = "^$|^[A-Za-z0-9][A-Za-z0-9._-]*$";
    static final String PURL_FORMAT = "^$|^pkg:[a-z]+/[^@\\s]+$";
    static final String ADVISORY = "(?:CVE|GHSA)-[A-Za-z0-9-]+";
    static final String ADVISORIES_FORMAT = "^[\\s,]*$|^[\\s,]*" + ADVISORY + "(?:[\\s,]+" + ADVISORY + ")*[\\s,]*$";
    private static final String CLAUSE = "(?:>=|<=|>|<|==)?\\s*\\d+(?:\\.\\d+)*(?:-(?:alpha|beta|rc)\\d+)?";
    static final String AFFECTS_FORMAT = "^$|^\\*$|^\\s*" + CLAUSE + "(?:\\s*,\\s*" + CLAUSE + ")*\\s*$";
    private static final String PROMISE = "\\d+\\.\\d+\\s*:\\s*\\d+\\.\\d+\\.\\d+(?:[-.](?:alpha|beta|rc)\\d*)?";
    static final String PROMISES_FORMAT = "^\\s*$|^\\s*" + PROMISE + "(?:\\s*,\\s*" + PROMISE + ")*\\s*,?\\s*$";
    private static final String PULL = "https://github\\.com/[\\w.-]+/[\\w.-]+/pull/\\d+";
    static final String PULLS_FORMAT = "^[\\s,]*$|^[\\s,]*" + PULL + "(?:[\\s,]+" + PULL + ")*[\\s,]*$";

    private static final Pattern SEPARATORS = Pattern.compile("[\\s,]+");

    @Id
    @GeneratedValue
    private @Nullable Long draftKey;

    /** The advisory and package pair, the identity a later save of the same pair replaces. */
    @Column(name = "pair_key", unique = true, nullable = false)
    private String pair = "";

    @NotBlank(groups = Ready.class, message = "workbench.invalid.required")
    @jakarta.validation.constraints.Pattern(regexp = ID_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.id")
    private String verdictId = "";

    @NotBlank(groups = Ready.class, message = "workbench.invalid.required")
    @jakarta.validation.constraints.Pattern(regexp = PURL_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.purl")
    private String purl = "";

    @NotBlank(groups = Ready.class, message = "workbench.invalid.required")
    @jakarta.validation.constraints.Pattern(regexp = ADVISORIES_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.advisories")
    @Column(length = 4000)
    private String advisories = "";

    @NotBlank(groups = Ready.class, message = "workbench.invalid.required")
    private String applicability = "";

    private String depth = "";

    @jakarta.validation.constraints.Pattern(regexp = AFFECTS_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.affects")
    private String affects = "";

    @Lob
    private String impact = "";

    @Lob
    private String workaround = "";

    /** The writer of cve-status refuses a verdict without one, as the prototype does, D-223. */
    @NotBlank(groups = Ready.class, message = "workbench.invalid.required")
    @Lob
    private String commentary = "";

    @Lob
    private String fixExpected = "";

    @jakarta.validation.constraints.Pattern(regexp = PROMISES_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.promise")
    private String fixExpectedVersion = "";

    @jakarta.validation.constraints.Pattern(regexp = PULLS_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.pulls")
    @Column(length = 4000)
    private String fixPrs = "";

    @Lob
    private String evidence = "";

    @jakarta.validation.constraints.Pattern(regexp = ID_FORMAT, groups = { Draft.class, Ready.class },
            message = "workbench.invalid.id")
    private String supersedes = "";

    private String assessedBy = "";

    /** Who saved last, the account name. Maintained by {@link VerdictDrafts}. */
    private String savedBy = "";

    /** When it was saved last. Maintained by {@link VerdictDrafts}. */
    private @Nullable Instant updatedAt;

    public VerdictDraft() {
    }

    /** A copy holding the same values, detached from any persistence context. */
    public VerdictDraft copy() {
        var copy = new VerdictDraft();
        copy.copyFrom(this);
        copy.draftKey = draftKey;
        copy.pair = pair;
        copy.savedBy = savedBy;
        copy.updatedAt = updatedAt;
        return copy;
    }

    /** Takes every value a person writes from the other draft. */
    void copyFrom(VerdictDraft other) {
        verdictId = other.verdictId;
        purl = other.purl;
        advisories = other.advisories;
        applicability = other.applicability;
        depth = other.depth;
        affects = other.affects;
        impact = other.impact;
        workaround = other.workaround;
        commentary = other.commentary;
        fixExpected = other.fixExpected;
        fixExpectedVersion = other.fixExpectedVersion;
        fixPrs = other.fixPrs;
        evidence = other.evidence;
        supersedes = other.supersedes;
        assessedBy = other.assessedBy;
    }

    /** The advisory ids, in the order written, each once. */
    public List<String> advisoryIds() {
        return split(advisories).stream().distinct().toList();
    }

    /** The pull request addresses, in the order written. */
    public List<String> pullRequests() {
        return split(fixPrs);
    }

    /** The pair this draft answers: its package and its advisories, in a stable order. */
    public static String pairOf(String purl, List<String> advisories) {
        return purl.trim() + "|" + advisories.stream().sorted().collect(Collectors.joining(","));
    }

    static List<String> split(String text) {
        return Arrays.stream(SEPARATORS.split(text.trim())).filter(part -> !part.isEmpty()).toList();
    }

    public @Nullable Long getDraftKey() {
        return draftKey;
    }

    String getPair() {
        return pair;
    }

    void setPair(String pair) {
        this.pair = pair;
    }

    public String getVerdictId() {
        return verdictId;
    }

    public void setVerdictId(String verdictId) {
        this.verdictId = verdictId;
    }

    public String getPurl() {
        return purl;
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public String getAdvisories() {
        return advisories;
    }

    public void setAdvisories(String advisories) {
        this.advisories = advisories;
    }

    public String getApplicability() {
        return applicability;
    }

    public void setApplicability(String applicability) {
        this.applicability = applicability;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getAffects() {
        return affects;
    }

    public void setAffects(String affects) {
        this.affects = affects;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getWorkaround() {
        return workaround;
    }

    public void setWorkaround(String workaround) {
        this.workaround = workaround;
    }

    public String getCommentary() {
        return commentary;
    }

    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }

    public String getFixExpected() {
        return fixExpected;
    }

    public void setFixExpected(String fixExpected) {
        this.fixExpected = fixExpected;
    }

    public String getFixExpectedVersion() {
        return fixExpectedVersion;
    }

    public void setFixExpectedVersion(String fixExpectedVersion) {
        this.fixExpectedVersion = fixExpectedVersion;
    }

    public String getFixPrs() {
        return fixPrs;
    }

    public void setFixPrs(String fixPrs) {
        this.fixPrs = fixPrs;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(String supersedes) {
        this.supersedes = supersedes;
    }

    public String getAssessedBy() {
        return assessedBy;
    }

    public void setAssessedBy(String assessedBy) {
        this.assessedBy = assessedBy;
    }

    public String getSavedBy() {
        return savedBy;
    }

    void setSavedBy(String savedBy) {
        this.savedBy = savedBy;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }

    void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
