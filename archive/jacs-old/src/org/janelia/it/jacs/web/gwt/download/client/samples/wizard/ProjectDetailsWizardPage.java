
package org.janelia.it.jacs.web.gwt.download.client.samples.wizard;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectInfoPanel;

/**
 * @author Michael Press
 */
public class ProjectDetailsWizardPage extends SamplesWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.wizard.DetailsWizardPage");

    private TitledBox _mainPanel;
    private ProjectInfoPanel _projectInfoPanel;

    public static final String HISTORY_TOKEN = "Project Details";
    private static final String PAGE_TITLE = "Project Details";

    public ProjectDetailsWizardPage(SampleInfo data, WizardController controller) {
        super(data, controller);
        init();
    }

    private void init() {
        _projectInfoPanel = new ProjectInfoPanel(TitledBoxFactory.BoxType.SECONDARY_BOX);

        _mainPanel = new TitledBox("Project Details", true);
        _mainPanel.add(_projectInfoPanel);
        _mainPanel.removeActionLinks(); // Remove "hide" link and add a "back" link
        _mainPanel.addActionLink(new BackActionLink("back", new ClickListener() {
            public void onClick(Widget widget) {
                getController().gotoPage(0);
            }
        }));
    }

    public String getPageToken() // used for history
    {
        return HISTORY_TOKEN;
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return PAGE_TITLE;
    }

    protected void preProcess(Integer priorPageNumber) {
        _logger.debug("ProjectDetailsWizardPage.preProcess()");

        _projectInfoPanel.setProject(getData().getCurrentProject());
    }
}