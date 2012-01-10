
package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class ResearchEntryPointsPanel extends TitledBox {
    protected static final String CANVAS_STYLENAME = "IconPanel";
    protected static final String CANVAS_CORNER_STYLENAME = "IconPanelRounding";
    protected static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    public ResearchEntryPointsPanel() {
        super("Tools", true);

        setWidth("300px"); // min width when contents are hidden

        // Add Blast Link
        Link blastLink = new Link("BLAST", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getAdvancedBlastUrl(), "_self", "");
            }
        });

        // Add PSI-Blast link
        Link psiBlastLink = new Link("PSI-BLAST", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getPsiBlastUrl(), "_self", "");
            }
        });

        // Add reverse PSI-Blast link
        Link revPsiBlastLink = new Link("Reverse PSI-BLAST", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getReversePsiBlastUrl(), "_self", "");
            }
        });

        // Add FRV link
        Link frvLink = new Link("Fragment Recruitment Viewer", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getFrvUrl(), "_self", "");
            }
        });

        // Add job results link
        Link myJobResultsLink = new Link("My Job Results", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getStatusUrl(), "_self", "");
            }
        });

        // Add all the links to the titlebox.
        add(blastLink);
        add(psiBlastLink);
        add(revPsiBlastLink);
        add(frvLink);
        add(myJobResultsLink);
    }

}

//   protected void popuplateContentPanel() {
//     CategoryImageBundle imageBundle = ImageBundleFactory.getCategoryImageBundle();
//   MenuIconPanel researchEntryIconPanel = new MenuIconPanel();

/*// add Search icon
MenuIcon searchMenuIcon = new MenuIcon(imageBundle.getAccessionIconLarge().createImage(),
    "&nbsp;<br>Search", "Search JaCS for key words, accessions, proteins, clusters, projects, etc.", "Search");
searchMenuIcon.addClickListener(new ClickListener() {
    public void onClick(Widget sender) {
        Window.open(UrlBuilder.getSearchUrl(),"_self","");
    }
});
researchEntryIconPanel.addIcon(searchMenuIcon);*/

/*
        // add BLAST icon
        MenuIcon blastMenuIcon = new MenuIcon(imageBundle.getBlastIconLarge().createImage(), "&nbsp;<br>BLAST",
            "Configure and execute BLAST jobs.", "BLAST");
        blastMenuIcon.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getAdvancedBlastUrl(), "_self","");
            }
        });
        researchEntryIconPanel.addIcon(blastMenuIcon);
*/

/*    // add Projects icon
        MenuIcon projectsMenuIcon = new MenuIcon(imageBundle.getProjectsIconLarge().createImage(), "&nbsp;<br>Projects",
            "Browse the metagenomic projects hosted by JaCS", "Projects");
        projectsMenuIcon.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getProjectsUrl(),"_self","");
            }
        });
        researchEntryIconPanel.addIcon(projectsMenuIcon);

        // add Publications icon
        MenuIcon publicationsMenuIcon = new MenuIcon(imageBundle.getPublicationsIconLarge().createImage(), "&nbsp;<br>Publications",
            "Browse metagenomic publications and download publication data.", "Publications");
        publicationsMenuIcon.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getPubsUrl(),"_self","");
            }
        });
        researchEntryIconPanel.addIcon(publicationsMenuIcon);

        // add Samples icon
        MenuIcon samplesMenuIcon = new MenuIcon(imageBundle.getSamplesIconLarge().createImage(),"&nbsp;<br>Samples",
            "Browse and download the data samples available for each project.", "Samples");
        samplesMenuIcon.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getSamplesUrl(),"_self","");
            }
        });
        researchEntryIconPanel.addIcon(samplesMenuIcon);
*/
// add FRV icon
/* MenuIcon frvMenuIcon = new MenuIcon(imageBundle.getFragmentRecruitmentIconLarge().createImage(),"Fragment<br>Recruitment",
            "Recruit sequences to microbial genomes using the Fragment Recruitment Viewer.", "FRV");



        frvMenuIcon.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getFrvUrl(),"_self","");
            }
        });
        researchEntryIconPanel.addIcon(frvMenuIcon);
*/
//      add(researchEntryIconPanel);
//}

