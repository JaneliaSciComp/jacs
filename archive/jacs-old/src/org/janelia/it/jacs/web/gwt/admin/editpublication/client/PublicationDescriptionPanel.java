
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.EPTableUtilities;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.RequiredPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;


public class PublicationDescriptionPanel extends VerticalPanel {

    //private TextArea abstractTextArea = null;
    private TextArea textArea = null;

    public PublicationDescriptionPanel(PublicationTabPanel parent) {

        super();

        createDescriptionPanel();

        this.setStyleName("EPFullPanel");

        parent.add(this, "Description");
    }


    private void createDescriptionPanel() {

        Label abstractLabel = new Label("Publication Abstract (text only):");
        abstractLabel.setStyleName("prompt");

        /*
        HorizontalPanel abstractPanel = new HorizontalPanel();
            abstractPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
            abstractPanel.add(abstractLabel);
            abstractPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            abstractPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

            abstractPanel.setStyleName("EPDescriptionPrompt");
        */

        // "Project Description" Prompt //
        FlexTable descriptionGrid = new FlexTable();
        EPTableUtilities.addWidgetWidgetPair(
                descriptionGrid, 0, 0, new HTMLPanel("<span class='prompt'>Project Description (HTML):</span>" +
                        "<span class='requiredInformation'>&nbsp;*</span>"),
                EPTableUtilities.getStyleLinkCell(new ExternalLink("style tutorial", "/jacs/styleTutorial.htm")));

        HorizontalPanel descriptionTitlePanel = new HorizontalPanel();
        descriptionTitlePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        descriptionTitlePanel.add(descriptionGrid);
        descriptionTitlePanel.setStyleName("EPDescriptionPrompt");
        descriptionTitlePanel.add(new RequiredPanel());

        // HTML Description TextArea //
        textArea = new TextArea();
        textArea.setStyleName("EPTextPanel");

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(descriptionTitlePanel);
        add(textArea);
    }


    public String getDescription() {
        return textArea.getText();
    }


    public void setSampleNewPublication() {

        textArea.setText("<html>\n" +
                "\n" +
                "<body></br></br></br></br>" +
                "in vitro motility assay  blah blah blah blah blah\n" +
                "in vitro motility assayin vitro motility assay\n" +
                "<br>" + "<br>" + "<br>" + "<br>" +
                "in vitro motility assay  blah blah blah blah blah blah\n" +
                "in vitro motility assayv vitro motilty a  blah blah blah blah\n" +
                //"<IMG SRC=\"/jacs/images/home/samples-thumb.jpg\" align=\"left\">\n" +
                "in vitro yvin vitro motility assayv  blah blah blah\n" +
                "in vo motil   itro motility assayvin vitro motility assayvin vitro motility assayv\n" +
                "o motiliity assayin vitro motility assay  blah blah blah\n" +
                "in vitr blah vitro motilty avitro motilty a blah blah blah blah\n" +
                "<br>" + "<br>" + "<br>" + "<br>" + "<br>" + "<br>" +
                "o motility assay  blah blah blah blah blah blah blah \n" +
                "in vitro motility assayin vitrsayin vitro motility assay\n" +
                "in vitro motilty assay blah blah blah blah blah blah blah\n" +
                "<br>" + "<br>" + "<br>" + "<br>" +
                "in vitro motility assay" +
                "ity assayin vitro motility assay\n" +
                " blah blah blah blah blah blah blah blah blah blah blah blah \n" +
                "</body>\n" +
                "\n" +
                "</html>");


    }

}
