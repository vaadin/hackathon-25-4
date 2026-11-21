package com.vaadin.securitystatus.explorer;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.securitystatus.base.ui.Downloads;
import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;

/**
 * The six machine files of one release, rule 6.3.13 of the source page: its
 * JSON record, the CSV, VEX, the Dependency-Check suppressions, the
 * osv-scanner configuration and the bomber ignore list. Each is a download
 * streamed from the publisher when the browser asks for it.
 */
final class ReleaseFiles extends Composite<Div> {

    ReleaseFiles(SecurityDataService data, VersionNumber version) {
        var row = getContent();
        row.addClassName("release-files-row");
        var label = new Span(getTranslation("explorer.files", version.toString()));
        label.addClassName("release-files-version");
        row.add(label);
        for (ExportKind kind : ExportKind.values()) {
            var link = Downloads.link(getTranslation("export." + kind.name()), kind.fileName(version),
                    () -> data.export(version, kind).map(
                            file -> new Downloads.Content(file.body(), file.contentType(), file.length())));
            link.addClassName("release-file");
            row.add(link);
        }
    }
}
