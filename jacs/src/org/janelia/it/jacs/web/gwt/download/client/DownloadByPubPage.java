
package org.janelia.it.jacs.web.gwt.download.client;

import java.util.*;

import org.gwtwidgets.client.util.Location;
import org.gwtwidgets.client.util.WindowUtils;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.PulldownPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NextActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.formatter.PublicationFormatter;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectInTable;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSelectedInTableListener;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSelectionCancelledListener;
import org.janelia.it.jacs.web.gwt.download.client.project.SelectProjectPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;

/**
 * Entry point for downloading publications and their data from the JaCS website.
 * <p/>
 * User: Lfoster
 * Date: Aug 18, 2006
 * Time: 4:17:49 PM
 */
public class DownloadByPubPage extends BaseEntryPoint {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.DownloadByPubPage");

    private Map _symbolToProjectMap;
    private List _publications;
    private Project _currentProject;
    private ResultReceiver _resultReceiver;
    private PulldownPopup _projectPulldownPopup;
    private SelectProjectPanel _selectProjectPopup;
    private ProjectPopup _projectPopup;
    private LoadingLabel _loadingLabel;
    private LoadingLabel _projectListLoadingLabel;
    private HorizontalPanel _projListPanel;
    private Panel _pubsPanel;
    private List _actionLinks = new ArrayList();
    private BackActionLink _prevActionLink;
    private NextActionLink _nextActionLink;
    private PublicationPanelHelper _panelBuildHelper = new PublicationPanelHelper();
    private HTML _subsetMessage;

    protected static final String LAST_PUB_PROJECT_PREF = "lastProject";
    protected static final String LAST_PUB_PROJECT_PREF_CATEGORY = "publications";

    private static DownloadMetaDataServiceAsync downloadService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    /**
     * Here is where the module associated with download is 'activated'.
     */
    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.DATA_BROWSE_PUBLICATIONS_LABEL), Constants.ROOT_PANEL_NAME);

        // Create the main panel and display the loading label
        Panel mainPanel = getContentPanel();
        _loadingLabel.setVisible(true);

        // Get the projects
        ResultReceiver rcv = new ResultReceiver() {
            public void setResult(Object result) {
                if (result instanceof Map) {
                    _symbolToProjectMap = (Map) result;
                    populateProjects(_symbolToProjectMap);
                }

            }
        };
        PublicationServiceHelper.populateProjects(rcv);
        RootPanel.get(Constants.ROOT_PANEL_NAME).add(mainPanel);
        show();
    }

    /**
     * Callback when the projects have been retrieved.  Populate the list and then retrieve the papers for the
     * first project.
     */
    private void populateProjects(Map<String, Project> symbolToProjectMap) {
        // Fatal error - no projects.  Remove GUI and post a message.
        if (symbolToProjectMap == null || symbolToProjectMap.size() == 0) {
            _projListPanel.setVisible(false);
            _loadingLabel.setVisible(false);
            _projectListLoadingLabel.setVisible(false);

            _pubsPanel.clear();
            _pubsPanel.add(HtmlUtils.getHtml("No projects found.", "text"));
            return;
        }

        // Post the projects to the popup for the table
        _selectProjectPopup.setProjects(symbolToProjectMap);

        // Sort projects and store sorted list
        ArrayList<Project> projects = new ArrayList(symbolToProjectMap.values());
        Collections.sort(projects);

        // Determine the initial project (via URL param or saved preference) or default to the first
        Location location = WindowUtils.getLocation();
        String projectSymbol = location.getParameter("projectSymbol");
        String publicationAccessionNo = location.getParameter("publicationAccessionNo");
        if (projectSymbol == null && publicationAccessionNo == null && projects.size() > 0) {
            SubjectPreference pref = Preferences.getSubjectPreference(LAST_PUB_PROJECT_PREF, LAST_PUB_PROJECT_PREF_CATEGORY);
            if (pref != null) {
                _logger.info("Using publication project " + projectSymbol + " from preference");
                projectSymbol = pref.getValue();
            }
            else { // default to first project
                _logger.info("Defaulting to first publication project " + projectSymbol);
                projectSymbol = projects.get(0).getProjectSymbol();
            }
        }
        else
            _logger.info("Using publication project " + projectSymbol + " from URL");

        // Populate the project list
        for (Project project : projects) {
            boolean isSelectedProject = false;
            if (projectSymbol != null) {
                isSelectedProject = projectSymbol.equals(project.getProjectSymbol());
            }
            else if (publicationAccessionNo != null) {
                // try to select the project based on the specified publication
                List projectPubs = project.getPublications();
                for (Iterator pubItr = projectPubs.iterator(); pubItr.hasNext();) {
                    Publication projectPub = (Publication) pubItr.next();
                    if (publicationAccessionNo.equals(projectPub.getAccessionNumber())) {
                        isSelectedProject = true;
                        break;
                    }
                } // done with all project publications
            }

            if (isSelectedProject)
                setCurrentProject(project);
        } // done w/ all projects

        _projectListLoadingLabel.setVisible(false); // projects have been loaded now

        // Retrieve the papers for the first project
        _resultReceiver = new ResultReceiver() {
            public void setResult(Object result) {
                setCurrentProject((Project) result);
                populatePublications();
            }
        };

        _logger.debug("retrieving project " + _currentProject.getProjectName());
        PublicationServiceHelper.populateProjectByName(_resultReceiver, _currentProject.getProjectName());
    }

    private void setCurrentProject(Project project) {
        Preferences.setSubjectPreference(new SubjectPreference(LAST_PUB_PROJECT_PREF, LAST_PUB_PROJECT_PREF_CATEGORY, project.getProjectSymbol()));
        _projectPulldownPopup.setText(project.getProjectName());
        _currentProject = project;
    }

    /**
     * Setup the list of publications, and (re-)establish the main panel.
     *
     * @param publicationList
     */
    private void setPublicationList(List publicationList) {
        // NOTE: if this was null before, we expect it to get replaced.
        _publications = publicationList;
    }

    /**
     * Content : a large tabbed pane with footer information.
     *
     * @return the content.
     */
    private Panel getContentPanel() {
        VerticalPanel mainPanel = new TitledBox("Publications and Data", false);
        mainPanel.setWidth("100%");

        // Add the project selector
        mainPanel.add(createProjectList());

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        // Add tab pane.
        _pubsPanel = new SimplePanel();
        if (hasPublications()) {
            _pubsPanel.add(getPapersTabs());
        }
        mainPanel.add(_pubsPanel);

        return mainPanel;
    }

    private class PublicationTabSelectedListener implements TabListener {

        private int prevSelection;

        private PublicationTabSelectedListener() {
            prevSelection = -1;
        }

        public boolean onBeforeTabSelected(SourcesTabEvents source, int tabNumber) {
            if (tabNumber == prevSelection) {
                return false;
            }
            else {
                prevSelection = tabNumber;
                return true;
            }
        }

        /**
         * Called when tab is selected.
         *
         * @param source    what was the source.
         * @param tabNumber which tab was selected.
         */
        public void onTabSelected(SourcesTabEvents source, int tabNumber) {
            Publication publication = (Publication) _publications.get(tabNumber);
            setBookmark(publication);
            SystemWebTracker.trackActivity("BrowsePublications",
                    new String[]{
                            _currentProject.getProjectSymbol(),
                            publication.getAccessionNumber()
                    });
        }

    }

    private void setBookmark(Publication selectedPublication) {
        String publicationAccession = selectedPublication.getAccessionNumber();
        Location location = WindowUtils.getLocation();
        String href = location.getHref();
        int i = href.lastIndexOf("/jacs");
        String shortUrl = href.substring(0, i) + "/jacs/id?" + publicationAccession;
        setBookmarkUrl(shortUrl, Constants.ROOT_PANEL_NAME);
    }

    private Widget getPapersTabs() {
        if (_currentProject == null) {
            return HtmlUtils.getHtml("An error occurred retrieving this project.", "error", "DownloadByPubsNoPapersMessage");
        }
        if (_publications != null && _publications.size() == 0) {
            return HtmlUtils.getHtml("This project has no papers.", "text", "DownloadByPubsNoPapersMessage");
        }
        else {
            String publicationAccessionNo = WindowUtils.getLocation().getParameter("publicationAccessionNo");
            WidgetTabBarVerticalTabPanel tabs = new WidgetTabBarVerticalTabPanel();
            tabs.setStyleName("DownloadByPubTabs");

            ArrayList titles = new ArrayList();
            PublicationFormatter formatter = new PublicationFormatter();
            tabs.addTabListener(formatter.getTabWidgetImageSwapper(tabs));
            tabs.addTabListener(new PublicationTabSelectedListener());
            int selectedTab = 0;
            for (int i = 0; i < _publications.size(); i++) {
                Publication publication = (Publication) _publications.get(i);
                if (publicationAccessionNo != null &&
                        publicationAccessionNo.equals(publication.getAccessionNumber())) {
                    selectedTab = i;
                }
                ClickableGrid presentation = formatter.getCitation(publication, titles);
                Widget toAdd = getPanelForPublication(formatter, publication, titles);
                tabs.add(presentation, toAdd);
            }
            tabs.selectTab(selectedTab);
            return tabs;
        }
    }

    private class ProjectPopup extends BasePopupPanel {
        private ProjectPopup(String title) {
            super(title);
        }

        protected void populateContent() {
            add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
            _selectProjectPopup = new SelectProjectPanel(new ProjectSelectionListener(), new ProjectSelectionCanceledListener(),
                    TitledBoxFactory.BoxType.SECONDARY_BOX);
            add(_selectProjectPopup);
        }
    }

    private class ProjectSelectionListener implements ProjectSelectedInTableListener {
        public void onSelect(Project project, int selectedProjectIndex, int numProjectsInTable) {
            _projectPopup.hide();
            updateSubsetMessage(selectedProjectIndex, numProjectsInTable);
            setCurrentProject(project);
            populateWithProject(project);
        }

        public void onUnSelect(Project project) {
        }
    }

    private class ProjectSelectionCanceledListener implements ProjectSelectionCancelledListener {
        public void onCancel(int numProjectsInTable) {
            _projectPopup.hide();
            updateNextPrevLinks(numProjectsInTable);
        }
    }

    private void populateWithProject(Project project) {
        _projectPulldownPopup.setText(project.getProjectName());
        _pubsPanel.clear();
        _loadingLabel.setVisible(true);
        PublicationServiceHelper.populateProjectByName(_resultReceiver, project.getProjectName());
    }

    private Widget createProjectList() {
        // Create loading labels
        _projectListLoadingLabel = new LoadingLabel("Loading project list...  ", true);
        _loadingLabel = new LoadingLabel("Loading papers...", /*visible*/ false);

        // Create the PulldownPopup project selector thingy
        _projectPopup = new ProjectPopup("Select a Project");
        _projectPulldownPopup = new PulldownPopup(_projectPopup);

        // Create the panel to house it all
        _projListPanel = new HorizontalPanel();
        _projListPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        _projListPanel.add(HtmlUtils.getHtml("Project: ", "prompt", "DownloadPublicationProjectPrompt"));
        _projListPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _projListPanel.add(_projectPulldownPopup);
        _projListPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _projListPanel.add(getNextPrevProjectLinks());
        _projListPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _projListPanel.add(getSubsetMesssage());
        _projListPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _projListPanel.add(_projectListLoadingLabel);
        _projListPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _projListPanel.add(_loadingLabel);

        return _projListPanel;
    }

    private Widget getNextPrevProjectLinks() {
        _prevActionLink = new BackActionLink("Previous project", new ClickListener() {
            public void onClick(Widget sender) {
                ProjectInTable prevProjectInTable = _selectProjectPopup.getPrevProjectInTable(_currentProject);
                setCurrentProject(prevProjectInTable.getProject());
                populateWithProject(prevProjectInTable.getProject());
                updateSubsetMessage(prevProjectInTable.getSelectedIndex(), prevProjectInTable.getTotalProjectsInTable());
            }
        });
        _nextActionLink = new NextActionLink("Next project", new ClickListener() {
            public void onClick(Widget sender) {
                ProjectInTable nextProjectInTable = _selectProjectPopup.getNextProjectInTable(_currentProject);
                setCurrentProject(nextProjectInTable.getProject());
                populateWithProject(nextProjectInTable.getProject());
                updateSubsetMessage(nextProjectInTable.getSelectedIndex(), nextProjectInTable.getTotalProjectsInTable());
            }
        });

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_prevActionLink);
        panel.add(HtmlUtils.getHtml("|", "smallLinkSeparator"));
        panel.add(_nextActionLink);

        return panel;
    }

    private void updateSubsetMessage(int selectedIndex, int totalProjectsInTable) {
        // Display the subset message if the number of projects in the selection table has been filtered
        if (totalProjectsInTable > 0 && (totalProjectsInTable != _selectProjectPopup.getProjects().size())) {
            _subsetMessage.setText("Project " + selectedIndex + " of " + totalProjectsInTable + " selected projects");
            _subsetMessage.setVisible(true);
        }
        else
            _subsetMessage.setVisible(false);

        updateNextPrevLinks(totalProjectsInTable);
    }

    private void updateNextPrevLinks(int totalProjectsInTable) {
        // Enable the next/prev links if number of projects in the selection table has been filtered (and is > 1)
        if (totalProjectsInTable > 1 && (totalProjectsInTable != _selectProjectPopup.getProjects().size())) {
            _nextActionLink.setEnabled(true);
            _prevActionLink.setEnabled(true);
        }
        else
            _subsetMessage.setVisible(false);

        if (totalProjectsInTable < 2 && (totalProjectsInTable != _selectProjectPopup.getProjects().size())) {
            _nextActionLink.setEnabled(false);
            _prevActionLink.setEnabled(false);
        }
    }

    public Widget getSubsetMesssage() {
        _subsetMessage = HtmlUtils.getHtml("", "hint"); // will be filled in as needed
        _subsetMessage.addStyleName("ProjectSubsetMessage");
        _subsetMessage.setVisible(false);
        return _subsetMessage;
    }

    /**
     * Setup vertical tabs for all the different papers in the current project.
     */
    private void populatePublications() {
        _loadingLabel.setVisible(false);
        if (_currentProject != null) {
            setPublicationList(_currentProject.getPublications());
            _pubsPanel.add(getPapersTabs());
        }
        else
            _pubsPanel.add(HtmlUtils.getHtml("Error: project not found", "error"));
    }

    private boolean hasPublications() {
        return (_publications != null && _publications.size() > 0);
    }

    /**
     * Build appropriate widget for showing details and downloading the publication. Currently, a
     * horizontal tabbed panel is used.
     *
     * @param formatter    can appropriately style/format paper information for display.
     * @param pub          where the info comes from (model object)
     * @param titleWidgets List of resizable title widgets
     * @return widget suitable for display.
     */
    private Widget getPanelForPublication(PublicationFormatter formatter, Publication pub, List titleWidgets) {
        RoundedTabPanel panel = new RoundedTabPanel(getActionLinks(2, titleWidgets), "DownloadPubsRoundedTabActionLink");
        _panelBuildHelper.populatePublicationTabPanels(panel, pub, _currentProject, formatter);
        return panel;
    }

    /**
     * Nasty but have to create one action link for each tab
     */
    private ActionLink[] getActionLinks(int numLinks, List titleWidgets) {
        ActionLink[] actionLinksForTabs = new ActionLink[numLinks];
        for (int i = 0; i < numLinks; i++) {
            //TODO: can get widths from CSS?
            actionLinksForTabs[i] = new DownloadPublicationWiderActionLink("200px", "300px", titleWidgets, _actionLinks);
            // default to wider state
            ((DownloadPublicationWiderActionLink) actionLinksForTabs[i]).toggleToSecondaryState(true);
            _actionLinks.add(actionLinksForTabs[i]);
        }
        return actionLinksForTabs;
    }

}
