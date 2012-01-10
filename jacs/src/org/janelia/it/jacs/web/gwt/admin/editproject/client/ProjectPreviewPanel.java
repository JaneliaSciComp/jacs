
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TertiaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.MailToLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Guy Cao
 */
public class ProjectPreviewPanel extends HorizontalPanel {

    private HTMLPanel htmlPreviewPanel = null;

    // parent panel (tabs)
    private ProjectTabPanel parentTabs = null;

    // key components
    private HTML title = null;
    private FlexTable infoGrid = null;
    private TertiaryTitledBox infoBox = null;
    private Grid linkGrid = null;
    private HTML description = null;


    // infoBox components
    private Link samplesLink = null;
    private Widget samplesWidget = null;

    private Link publicationLink = null;
    private Widget publicationWidget = null;

    private ExternalLink websiteLink = null;
    private Widget websiteWidget = null;

    private MailToLink emailLink = null;
    private Widget emailWidget = null;


    public ProjectPreviewPanel(ProjectTabPanel parent) {
        super();

        this.parentTabs = parent;
        parent.add(this, "Preview");
        parent.addTabListener(new PreviewTabListener());

        // project title
        title = HtmlUtils.getHtml("", "BrowseProjectTitle");

        // project information
        infoGrid = new FlexTable();

        // project more information (links)
        createInfoBox();

        // project's html description
        description = HtmlUtils.getHtml(parent.getDescriptionPanel().getDescription(), "text");

        htmlPreviewPanel = new HTMLPanel(new StringBuffer()
                .append("<span id='projectTitle'></span>")
                .append("<span class='MoreInfoLinksBox' id='projectLinks'></span>")
                .append("<span id='projectInfo'></span>")
                .append("<span id='projectDescription'></span>")
                .toString());

        htmlPreviewPanel.add(title, "projectTitle");
        htmlPreviewPanel.add(infoGrid, "projectInfo");
        htmlPreviewPanel.add(infoBox, "projectLinks");
        htmlPreviewPanel.add(description, "projectDescription");

        htmlPreviewPanel.setWidth("90%");
        htmlPreviewPanel.setHeight("500");
        //previewPanel.setVisible(false);

        add(htmlPreviewPanel);
    }

    private void createInfoBox() {

        ProjectDetailsPanel detailPanelProject = parentTabs.getDetailPanel();
        infoBox = new TertiaryTitledBox("More Information", false);

        linkGrid = new Grid(4, 1);
        int row = 0;

        // samples link
        samplesLink = new Link("Samples", "");
        samplesLink.setStyleName("prompt");
        samplesWidget = EPTableUtilities.getLinkCell(samplesLink);
        linkGrid.setWidget(row++, 0, samplesWidget);

        // publication and data link
        publicationLink = new Link("Publications", "");
        publicationLink.setStyleName("prompt");
        publicationWidget = EPTableUtilities.getLinkCell(publicationLink);
        linkGrid.setWidget(row++, 0, publicationWidget);

        // website link
        websiteLink = new ExternalLink("Website", (String) null);
        websiteWidget = EPTableUtilities.getLinkCell(websiteLink);
        linkGrid.setWidget(row++, 0, websiteWidget);

        // contact link
        emailLink = new MailToLink("Contact", detailPanelProject.getEmail());
        emailWidget = EPTableUtilities.getLinkCell(emailLink);
        linkGrid.setWidget(row++, 0, emailWidget);


        /*
        if (detailPanelProject.getWebsite() != null && !detailPanelProject.getWebsite().equals(""))
            innerGrid.setWidget(row++, 0, websiteWidget);
         */

        /*
        if (detailPanelProject.getEmail() != null && !detailPanelProject.getEmail().equals(""))
           innerGrid.setWidget(row++, 0, emailWidget);
        */

        infoBox.add(linkGrid);


        if (detailPanelProject.getName() != null && !detailPanelProject.getName().equals("")) {
            infoBox.setVisible(true);
        }
        else {
            infoBox.setVisible(false);
        }

    }


    // refresh Project Information
    public void updateProjectView() {

        ProjectDetailsPanel detailPanel = parentTabs.getDetailPanel();
        ProjectDescriptionPanel descriptionPanel = parentTabs.getDescriptionPanel();

        // project title //
        if (detailPanel.getName() != null) {
            title.setText(detailPanel.getName());
        }
        else {
            //todo: error message: title needed
        }

        htmlPreviewPanel.remove(infoGrid);
        infoGrid = new FlexTable();
        htmlPreviewPanel.add(infoGrid, "projectInfo");

        int row = 0;

        if (detailPanel.getPI() != null && !detailPanel.getPI().equals("")) {
            EPTableUtilities.addTextTextPair(infoGrid, row++, 0, "Principal Investigator(s)", detailPanel.getPI());
        }
        if (detailPanel.getFundingOrganization() != null && !detailPanel.getFundingOrganization().equals("")) {
            EPTableUtilities.addTextTextPair(infoGrid, row++, 0, "Funded by", detailPanel.getFundingOrganization());
        }
        if (detailPanel.getProjectOrganization() != null && !detailPanel.getProjectOrganization().equals("")) {
            EPTableUtilities.addTextTextPair(infoGrid, row++, 0, "Organization", detailPanel.getProjectOrganization());
        }
        if (detailPanel.getAffiliation() != null && !detailPanel.getAffiliation().equals("")) {
            EPTableUtilities.addTextTextPair(infoGrid, row++, 0, "Affiliation", detailPanel.getAffiliation());
        }

        // project information (links)
        updateInfoBox(detailPanel);

        // refresh project Description
        if (descriptionPanel.getDescription() != null) {
            description.setHTML(descriptionPanel.getDescription());
        }
        else {
            description.setHTML("");
        }

        infoBox.setVisible(true);
        htmlPreviewPanel.setVisible(true);

    }


    private void updateInfoBox(ProjectDetailsPanel detailPanel) {

        if (detailPanel.getWebsite() != null && !detailPanel.getWebsite().equals("")) {

            websiteLink = new ExternalLink("Website", "http://" + detailPanel.getWebsite());
            websiteWidget = EPTableUtilities.getLinkCell(websiteLink);
            linkGrid.setWidget(2, 0, websiteWidget);

            websiteWidget.setVisible(true);
        }
        else {
            websiteWidget.setVisible(false);
        }


        if (detailPanel.getEmail() != null && !detailPanel.getEmail().equals("")) {

            emailLink = new MailToLink("Contact", detailPanel.getEmail());
            emailWidget = EPTableUtilities.getLinkCell(emailLink);
            linkGrid.setWidget(3, 0, emailWidget);

            emailWidget.setVisible(true);
        }
        else {
            emailWidget.setVisible(false);
        }


    }

    private class PreviewTabListener implements TabListener {

        /*
        ProjectImpl project = null;

        public PreviewTabListener (ProjectImpl project) {
            this.project = project;
        }
        */

        public boolean onBeforeTabSelected(SourcesTabEvents tabEvents, int i) {
            return true; // don't do anything
        }

        public void onTabSelected(SourcesTabEvents tabEvents, int tabPanelIndex) {
            if (tabPanelIndex == 2) { // Preview Panel Selected
                updateProjectView();
            }
        }

    }


    private void addDebugMessage(String message) {

        Label label = new Label(message);
        label.setStyleName("titledBoxLabel");
        this.add(label);

    }


}
