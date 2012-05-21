package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class ProjectsPanel extends TitledBox {
    protected static final String CANVAS_STYLENAME = "IconPanel";
    protected static final String CANVAS_CORNER_STYLENAME = "IconPanelRounding";
    protected static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    public ProjectsPanel() {
        super("Projects", true);

        setWidth("300px"); // min width when contents are hidden

        // Add GECI Link
        Link geciLink = new Link("Geci - Neuronal Assay Analysis", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getNeuronalAssayAnalysisUrl(), "_self", "");
            }
        });

        // Add TIC Link
        Link ticLink = new Link("Transcription Imaging Consortium", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getTICUrl(), "_self", "");
            }
        });

//        // Add Zlatic Lab link
//        Link zlaticLabLink = new Link("Zlatic Lab", new ClickListener() {
//            public void onClick(Widget sender) {
//                Window.open(UrlBuilder.getZlaticLabUrl(), "_self", "");
//            }
//        });

        // Add all the links to the titlebox.
//        add(geciLink);
        add(ticLink);
//        add(zlaticLabLink);
    }

}

