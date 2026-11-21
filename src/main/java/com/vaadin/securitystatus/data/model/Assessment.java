package com.vaadin.securitystatus.data.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One verdict as Vaadin wrote it. The same shape arrives in the index's
 * {@code recent}, in a version's and an advisory's {@code assessments} and in
 * the queue, each carrying a different subset of these fields.
 */
public record Assessment(Optional<String> id, Optional<String> purl, List<String> cves,
        Optional<String> applicability, Optional<String> justification, Optional<String> impact,
        Optional<String> workaround, Optional<String> commentary, Optional<String> fixExpected,
        Map<String, Object> fixExpectedVersion, List<String> fixPrs, Optional<String> handover,
        Optional<String> depth, Optional<String> affects, Optional<String> assessedBy,
        Optional<LocalDate> assessedAt, Optional<String> supersedes, Optional<String> importedFrom) {

    public Assessment {
        id = Wire.text(id);
        purl = Wire.text(purl);
        cves = Wire.list(cves);
        applicability = Wire.text(applicability);
        justification = Wire.text(justification);
        impact = Wire.text(impact);
        workaround = Wire.text(workaround);
        commentary = Wire.text(commentary);
        fixExpected = Wire.text(fixExpected);
        fixExpectedVersion = Wire.map(fixExpectedVersion);
        fixPrs = Wire.list(fixPrs);
        handover = Wire.text(handover);
        depth = Wire.text(depth);
        affects = Wire.text(affects);
        assessedBy = Wire.text(assessedBy);
        assessedAt = Wire.value(assessedAt);
        supersedes = Wire.text(supersedes);
        importedFrom = Wire.text(importedFrom);
    }
}
