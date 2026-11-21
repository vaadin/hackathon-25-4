package com.vaadin.securitystatus.explorer;

import java.util.Optional;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.Downloads;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;

import jakarta.annotation.security.RolesAllowed;

/**
 * The internal front door. For now the machine files of the newest release of
 * each line, each a download proxied from the publisher. Epic 08 builds the
 * explorer around them.
 */
@Route(value = "findings", layout = InternalLayout.class)
@PageTitle("explorer.title")
@Menu(order = 0, title = "explorer.title")
@RolesAllowed(Roles.SECURITY)
public final class ExplorerView extends Composite<Div> {

    private final SecurityDataService data;

    public ExplorerView(SecurityDataService data) {
        this.data = data;
        addClassName("explorer-view");
        getContent().add(new ViewHeading());

        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            getContent().add(new DataOutageNotice());
            return;
        }
        var files = new Div();
        files.addClassName("release-files");
        for (ExplorerFront.Line line : front.lines()) {
            if (Boolean.TRUE.equals(line.scannerFiles())) {
                files.add(filesOf(line.version()));
            }
        }
        getContent().add(files);
    }

    private Div filesOf(VersionNumber version) {
        var release = new Span(version.toString());
        release.addClassName("release-files-version");
        var row = new Div(release);
        row.addClassName("release-files-row");
        for (ExportKind kind : ExportKind.values()) {
            var link = Downloads.link(getTranslation("export." + kind.name()), kind.fileName(version),
                    () -> data.export(version, kind).map(
                            file -> new Downloads.Content(file.body(), file.contentType(), file.length())));
            link.addClassName("release-file");
            row.add(link);
        }
        return row;
    }
}
