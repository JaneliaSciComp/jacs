
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.EPTableUtilities;
import org.janelia.it.jacs.web.gwt.common.client.panel.TertiaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;


/**
 * @author Guy Cao
 */
public class PublicationPreviewPanel extends HorizontalPanel {

    private HTMLPanel previewAllPanel = null;

    // parent panel (tabs)
    private PublicationTabPanel parentTabs = null;

    // key components
    private HTML title = null;
    private TertiaryTitledBox infoBox = null;
    private Grid linkGrid = null;
    //private HTML abstractText = null;
    private HTML description = null;

    // infoBox components
    private Link downloadPubLink = null;
    private Widget downloadPubWidget = null;


    public PublicationPreviewPanel(PublicationTabPanel parent) {

        super();

        this.parentTabs = parent;
        parent.add(this, "Preview");

        // project title
        title = HtmlUtils.getHtml("", "BrowseProjectTitle");

        // project more information (links)
        createInfoBox();

        // project's abstract text
        //abstractText = new HTML("");
        //abstractText.setVisible(false);

        // project's html description
        /*
        description = HtmlUtils.getHtml("<span class='prompt'>Synopsis: </span>" +
                parent.getDescriptionPanel().getDescription(), "text");
                */
        description = new HTML("");
        description.setVisible(false);

        previewAllPanel = new HTMLPanel(new StringBuffer()
                .append("<span id='publicationTitle'></span>")
                .append("<span class='MoreInfoLinksBox' id='publicationLinks'></span>")
                .append("<span id='publicationAbstract'></span>")
                .append("<span id='smallSpacer'>&nbsp;</span>")
                .append("<span id='publicationDescription'></span>")
                .toString());

        previewAllPanel.add(title, "publicationTitle");
        previewAllPanel.add(infoBox, "publicationLinks");
        //previewAllPanel.add(abstractText, "publicationAbstract");
        previewAllPanel.add(description, "publicationDescription");

        previewAllPanel.setWidth("90%");
        previewAllPanel.setHeight("500");

        add(previewAllPanel);

    }

    protected void createInfoBox() {

        /*
        ProjectDetailsPanel detailPanelProject = parentTabs.getDetailPanel();
        */

        infoBox = new TertiaryTitledBox("More Information", false);
        linkGrid = new Grid(5, 1);
        int row = 0;


        // download publication link
        downloadPubLink = new Link("Download this publication", "");
        downloadPubLink.setStyleName("prompt");
        downloadPubWidget = EPTableUtilities.getLinkCell(downloadPubLink);
        linkGrid.setWidget(row++, 0, downloadPubWidget);


        infoBox.add(linkGrid);

        /*
        if (detailPanelProject.getName() != null && !detailPanelProject.getName().equals("")) {
            infoBox.setVisible(true);
        }
        else { infoBox.setVisible(false); }
        */

    }


    // refresh Publication Information
    public void updatePublicationView() {


        PublicationDetailsPanel publicationDetailPanel = parentTabs.getDetailPanel();
        PublicationDescriptionPanel publicationDescriptionPanel = parentTabs.getDescriptionPanel();

        // publication title //
        title.setText(publicationDetailPanel.getTitle());

        /*
        updateInfoBox();
        */

        /*
        // refresh publication abstract
        if (publicationDescriptionPanel.getAbstract() != null) {
            abstractText.setHTML("<span class='prompt'>Abstract: </span>" +
                "<span class='EPText'>"+publicationDescriptionPanel.getAbstract()+"</span>");
            abstractText.setVisible(true);

        } else {
            abstractText.setHTML("");
        }
        */

        // refresh publication Description
        if (publicationDescriptionPanel.getDescription() != null) {

            //description.setHTML("<span class='prompt'>Synopsis: </span>");
            description.setHTML("<span class='prompt'>Synopsis: </span>" + "<span class='EPText'>" + publicationDescriptionPanel.getDescription() + "</span>");
            description.setVisible(true);

        }
        else {
            description.setHTML("");
            //addDebugMessage("description IS null");
        }

        infoBox.setVisible(true);
        previewAllPanel.setVisible(true);

    }

    private void addDebugMessage(String message) {

        Label label = new Label(message);
        label.setStyleName("titledBoxLabel");
        this.add(label);

    }


}
