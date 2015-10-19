
package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NextActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * @author Michael Press
 */
public class ViewProjectPage extends WizardPage {
    private TitledBox _mainPanel;
    private ClickListener _nextListener;
    private ClickListener _prevListener;

    protected ViewProjectPage(ProjectsWizardController controller, ClickListener nextListener, ClickListener prevListener) {
        super(/*show buttons*/ false, controller);
        _nextListener = nextListener;
        _prevListener = prevListener;
        init();
    }

    private void init() {

        _mainPanel = new TitledBox("Projects", /*show action links */false);
        _mainPanel.add(new LoadingLabel(/*visible*/ true));

        // Register for a callback when the data's been loaded
        getProjectsController().getDataManager().addDataRetrievedListener(new ProjectsRetrievedListener());
    }

    private class ProjectsRetrievedListener implements DataRetrievedListener {
        public void onSuccess(Object data) {
            displayProject(getCurrentProject());
        }

        public void onFailure(Throwable throwable) {
            //TODO:
        }

        public void onNoData() {
            //TODO:
        }
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    /**
     * Notification that the page is about to be displayed (but unlike preProcess(), the page has been faded out,
     * so we can update it with the current project without it being visible until the page is shown by the wizard controller.
     */
    protected void mainPanelCreated() {
        displayProject(getCurrentProject());
    }

    private void displayProject(Project project) {
        if (project != null) {
            _mainPanel.clear();
            _mainPanel.add(new CustomProjectInfoPanel(project));
        }
    }

    /**
     * Extends the usual ProjectInfoPanel and adds controls for changing the visible project and next/prev project links
     */
    private class CustomProjectInfoPanel extends ProjectInfoPanel {
        public CustomProjectInfoPanel(Project project) {
            super(project, TitledBoxFactory.BoxType.SECONDARY_BOX);
        }

        protected Widget createProjectTitle(String title) {
            DockPanel headerControlPanel = new DockPanel();
            headerControlPanel.setStyleName("ProjectTitlePanel");

            headerControlPanel.add(createChangeProjectLink(), DockPanel.WEST);
            headerControlPanel.add(HtmlUtils.getHtml(title, "BrowseProjectTitle"), DockPanel.CENTER);

            Widget nextPrevPanel = getNextPrevProjectLinks();
            headerControlPanel.add(nextPrevPanel, DockPanel.EAST);
            headerControlPanel.setCellHorizontalAlignment(nextPrevPanel, DockPanel.ALIGN_RIGHT);

            VerticalPanel titlePanel = new VerticalPanel();
            titlePanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
            titlePanel.setWidth("100%");

            titlePanel.add(getSubsetMessage());
            titlePanel.add(headerControlPanel);

            return titlePanel;
        }

        private Widget getSubsetMessage() {
            HTML subsetMessage = HtmlUtils.getHtml("", "hint");
            subsetMessage.addStyleName("ProjectSubsetMessage");
            subsetMessage.setVisible(false);

            ProjectDataManager data = getProjectsController().getDataManager();
            if ((data.getNumProjectsInTable() > 0) && (data.getNumProjectsInTable() != data.getNumProjects())) {
                subsetMessage.setText(
                        "Project " + data.getCurrentProjectIndexInTable() +
                                " of " + data.getNumProjectsInTable() + " selected projects");
                subsetMessage.setVisible(true);
            }

            return subsetMessage;
        }

        private ActionLink createChangeProjectLink() {
            return new ActionLink("View another project",
                    ImageBundleFactory.getControlImageBundle().getProjectImage().createImage(),
                    new ClickListener() {
                        public void onClick(Widget sender) {
                            getController().gotoPage(1); // TODO: use constant from ProjectWizard
                        }
                    });
        }

        private Widget getNextPrevProjectLinks() {
            BackActionLink prevActionLink = new BackActionLink("Previous project", new ClickListener() {
                public void onClick(Widget sender) {
                    notifyPrevListener();
                }
            });
            NextActionLink nextActionLink = new NextActionLink("Next project", new ClickListener() {
                public void onClick(Widget sender) {
                    notifyNextListener();
                }
            });

            // Disable the links if there's 0 or 1 projects in the data table
            ProjectDataManager data = getProjectsController().getDataManager();
            if ((data.getNumProjectsInTable() != -1) && (data.getNumProjectsInTable() < 2) &&
                    (data.getNumProjectsInTable() != data.getNumProjects())) {
                prevActionLink.setEnabled(false);
                nextActionLink.setEnabled(false);
            }

            HorizontalPanel panel = new HorizontalPanel();
            panel.add(prevActionLink);
            panel.add(HtmlUtils.getHtml("|", "smallLinkSeparator"));
            panel.add(nextActionLink);

            return panel;
        }
    }

    private void notifyNextListener() {
        _nextListener.onClick(null);
    }

    private void notifyPrevListener() {
        _prevListener.onClick(null);
    }

    public String getPageTitle() {
        return null;
    }

    public String getPageToken() // used for history
    {
        return "ViewProject";
    }

    private Project getCurrentProject() {
        return getProjectsController().getDataManager().getCurrentProject();
    }

    public ProjectsWizardController getProjectsController() {
        return (ProjectsWizardController) getController();
    }
}
