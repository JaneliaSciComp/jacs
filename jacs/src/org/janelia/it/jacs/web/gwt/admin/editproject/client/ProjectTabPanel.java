
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.Label;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;

/**
 * @author Guy Cao
 */
public class ProjectTabPanel extends RoundedTabPanel {

    private ProjectDetailsPanel projectDetailPanel = null;
    private ProjectPreviewPanel projectPreviewPanel = null;
    private ProjectDescriptionPanel projectDescriptionPanel = null;
    private ProjectPublicationPanel projectPublicationPanel = null;

    public ProjectTabPanel() {

        // detail
        projectDetailPanel = new ProjectDetailsPanel(this);

        // description
        projectDescriptionPanel = new ProjectDescriptionPanel(this);

        // preview
        projectPreviewPanel = new ProjectPreviewPanel(this); //tabs.add(projectPreviewPanel, "Preview");


        // publication
        projectPublicationPanel = new ProjectPublicationPanel(this);


        // preferences
        selectTab(0);
        setWidth("100%");

    }

    public ProjectDetailsPanel getDetailPanel() {
        return projectDetailPanel;
    }

    public ProjectDescriptionPanel getDescriptionPanel() {
        return projectDescriptionPanel;
    }

    public ProjectPreviewPanel getPreviewPanel() {
        return projectPreviewPanel;
    }

    public ProjectPublicationPanel getPublicationPanel() {
        return projectPublicationPanel;
    }


    private void addDebugMessage(String message) {

        Label label = new Label(message);
        label.setStyleName("titledBoxLabel");
        this.add(label);

    }


}
