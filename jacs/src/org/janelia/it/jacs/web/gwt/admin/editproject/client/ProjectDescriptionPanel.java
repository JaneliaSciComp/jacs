
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;


public class ProjectDescriptionPanel extends VerticalPanel {

    private TextArea textArea = null;

    public ProjectDescriptionPanel(ProjectTabPanel parent) {

        super();

        createDescriptionPanel();

        this.setStyleName("EPFullPanel");

        parent.add(this, "Description");
    }


    private void createDescriptionPanel() {

        // "Project Description" Prompt
        FlexTable descriptionGrid = new FlexTable();
        EPTableUtilities.addWidgetWidgetPair(
                descriptionGrid, 0, 0, new HTMLPanel("<span class='prompt'>Project Description (HTML):</span>" +
                        "<span class='requiredInformation'>&nbsp;*</span>"),
                new ExternalLink("style tutorial", "/jacs/styleTutorial.htm"));

        HorizontalPanel descriptionTitlePanel = new HorizontalPanel();
        descriptionTitlePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        descriptionTitlePanel.add(descriptionGrid);
        descriptionTitlePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        descriptionTitlePanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        descriptionTitlePanel.add(new RequiredPanel());
        descriptionTitlePanel.setStyleName("EPDescriptionPrompt");

        // TextArea for html project description
        textArea = new TextArea();
        textArea.setStyleName("EPTextPanel");
        disableDescriptionInput();

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(descriptionTitlePanel);
        add(textArea);
    }

    protected void disableDescriptionInput() {
        textArea.setEnabled(false);
    }

    protected void enableDescriptionInput() {
        textArea.setEnabled(true);
    }

    protected String getDescription() {
        return textArea.getText();
    }

    protected void updateProjectView(ProjectImpl project) {
        clearDescription();

        // update description view
        textArea.setText(project.getDescription());
    }

    protected void clearDescription() {
        // clear description text area
        textArea.setText("");
    }


    protected void setSampleNewProject() {

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
