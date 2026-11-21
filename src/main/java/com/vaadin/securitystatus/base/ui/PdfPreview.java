package com.vaadin.securitystatus.base.ui;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.pdfviewer.PdfViewerFeatureFlagProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * The one door to the PDF Viewer, an experimental component that throws when
 * its feature flag is off. A screen asks {@link #available()} first and offers
 * the plain download when the answer is no, rule 1 of
 * {@code specs/CHANGELOG-RISK.md}.
 */
public final class PdfPreview {

    private PdfPreview() {
    }

    /** Whether the viewer may be put on a page in this application. */
    public static boolean available() {
        var service = VaadinService.getCurrent();
        return service != null && FeatureFlags.get(service.getContext())
                .isEnabled(PdfViewerFeatureFlagProvider.FEATURE_FLAG_ID);
    }

    /** A viewer of the document the handler serves. Only when {@link #available()}. */
    public static PdfViewer of(DownloadHandler document, String fileName) {
        var viewer = new PdfViewer(document);
        viewer.setFileName(fileName);
        return viewer;
    }
}
