package com.vaadin.securitystatus.explorer;

import java.util.Optional;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;

import jakarta.annotation.security.RolesAllowed;

/**
 * One release on its own page, where the explorer's search lands when a full
 * version is typed. For now it names the release, says whether it was
 * measured, and offers its machine files; epic 09 reads the release in full
 * here.
 */
@Route(value = "findings/release/:line/:version", layout = InternalLayout.class)
@PageTitle("release.title")
@RolesAllowed(Roles.SECURITY)
public final class ReleaseView extends Composite<Div> implements BeforeEnterObserver {

    public static final String LINE = "line";
    public static final String VERSION = "version";

    private final SecurityDataService data;
    private final Div body = new Div();

    public ReleaseView(SecurityDataService data) {
        this.data = data;
        addClassName("release-view");
        body.addClassName("release-body");
        getContent().add(new ViewHeading(), body);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String line = event.getRouteParameters().get(LINE).orElse("");
        VersionNumber version = VersionNumber.of(event.getRouteParameters().get(VERSION).orElse(""));
        body.removeAll();
        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            body.add(new DataOutageNotice());
            return;
        }
        Optional<ExplorerFront.Line> measured = front.lines().stream()
                .filter(candidate -> candidate.line().equals(line) && candidate.line().equals(version.line()))
                .filter(candidate -> candidate.version().equals(version) || candidate.releases().contains(version))
                .findFirst();
        if (measured.isEmpty()) {
            // Rule 6.9.7: answered where it was asked, not sent anywhere.
            body.add(lead("release-unmeasured", getTranslation("release.unmeasured", version.toString())));
        } else if (measured.get().version().equals(version)) {
            body.add(lead("release-newest", getTranslation("release.newest", measured.get().line())));
            if (Boolean.TRUE.equals(measured.get().scannerFiles())) {
                body.add(new ReleaseFiles(data, version));
            }
        } else {
            body.add(lead("release-older",
                    getTranslation("release.older", measured.get().line(), measured.get().version().toString())));
        }
        var back = new RouterLink(getTranslation("release.back"), ExplorerView.class);
        back.addClassName("release-back");
        body.add(back);
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}
