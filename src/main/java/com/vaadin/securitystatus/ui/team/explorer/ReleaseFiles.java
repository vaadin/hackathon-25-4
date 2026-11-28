package com.vaadin.securitystatus.ui.team.explorer;

import static com.vaadin.securitystatus.ui.common.Parts.css;

import java.util.EnumSet;
import java.util.Set;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.ui.common.Downloads;

/**
 * The six machine files of one release, rule 6.3.13 of the source page: its
 * JSON record, the CSV, VEX, the Dependency-Check suppressions, the
 * osv-scanner configuration and the bomber ignore list. Each is a download
 * streamed from the publisher when the browser asks for it.
 */
final class ReleaseFiles extends Composite<Div> {

    ReleaseFiles(SecurityDataService data, VersionNumber version) {
        this(data, version, EnumSet.allOf(ExportKind.class), false);
    }

    /**
     * The files, and with {@code tooling} the explorer's row in the
     * prototype's words: "For your own tooling:", the short names, and what
     * each is for on hover, D-212.
     */
    ReleaseFiles(SecurityDataService data, VersionNumber version, Set<ExportKind> kinds, boolean tooling) {
        var row = getContent();
        row.addClassName("release-files-row");
        var label = css(new Span(tooling ? getTranslation("explorer.files.tooling")
                : getTranslation("explorer.files", version.toString())), "release-files-version");
        row.add(label);
        for (ExportKind kind : EnumSet.copyOf(kinds)) {
            var link = Downloads.link(getTranslation((tooling ? "explorer.file." : "export.") + kind.name()),
                    kind.fileName(version),
                    () -> data.export(version, kind).map(
                            file -> new Downloads.Content(file.body(), file.contentType(), file.length())));
            link.addClassName("release-file");
            if (tooling) {
                Tooltip.forComponent(link).setText(getTranslation("explorer.file." + kind.name() + ".title"));
            }
            row.add(link);
        }
    }
}
