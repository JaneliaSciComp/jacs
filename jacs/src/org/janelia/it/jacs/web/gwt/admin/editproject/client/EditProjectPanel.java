
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel that allow users to create new projects and display and edit existing projects.
 *
 * @author Guy Cao
 */
public class EditProjectPanel extends TitledBox {

    // Project Data //
    protected Map<String, ProjectImpl> _symbolToProjectMap;
    protected ArrayList<ProjectImpl> _projectList;
    protected HashMap<String, String> _nameToSymbolMap;

    protected ProjectImpl project; // selected project 

    // Connect to Database //
    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    // GUI components //
    private ProjectTabPanel tabs = null;
    private ListBox existingProjSelector = null;
    private static final String ExistingProjSelectorPrompt = "-- Please select a project --";
    private RadioButton existingProjButton = null;
    private RadioButton newProjButton = null;
    private RoundedButton saveButton = null;

    public EditProjectPanel() {
        super("Edit Project");
    }


    protected void popuplateContentPanel() {
        // Retrieve Projects from Database
        downloadService.getSymbolToProjectMapping(new PopulateProjectsCallback(this, /*create GUI*/ true));

        //new GetProjectsTimer().schedule(2000);  //same thing as above except timed

        /*
        note: goofy gwt shell error encountered, all widgets except for Panel becomes null
        in subclasses (listeners and callbacks)
        */

    }

    /**
     * Delay needed to give UI the chance to render loading label
     */
    /*
    private class GetProjectsTimer extends Timer {
        public void run() {
            downloadService.getSymbolToProjectMapping(new PopulateProjectsCallback());
        }
    }
    */

    protected class PopulateProjectsCallback implements AsyncCallback {

        LoadingLabel _loadingLabel = null;
        EditProjectPanel parentPanel = null;

        // flag to only reload projects (as opposed to create new tabs and save buttons again)
        // added after calling PopulateProjectsCallback everytime after hitting save button
        boolean createAndLoadGUI = false;

        public PopulateProjectsCallback(EditProjectPanel parentPanel, boolean createAndLoadGUI) {

            this.parentPanel = parentPanel;
            this.createAndLoadGUI = createAndLoadGUI;

            if (createAndLoadGUI) {
                _loadingLabel = new LoadingLabel("Loading projects...", true);
            }
            else {
                _loadingLabel = new LoadingLabel("Refreshing projects...", true);
            }

            this.parentPanel.add(_loadingLabel);
        }

        // TODO: notify user (or not)
        public void onFailure(Throwable throwable) {
            System.out.println("EditProjecPanel: Failed to retrieve projects: " + throwable.getMessage());
        }

        public void onSuccess(Object result) {
            System.out.println("Projects received successfully");

            // invisify "loading projects..."
            _loadingLabel.setVisible(false);
            this.parentPanel.remove(_loadingLabel);

            _symbolToProjectMap = (Map<String, ProjectImpl>) result;
            _projectList = new ArrayList<ProjectImpl>(_symbolToProjectMap.values());

            // create name-to-symbol mapping
            createNameSymbolMap();

            if (createAndLoadGUI) {
                // create projects selector
                createProjectSelection();
            }

            // add Project names to List BOX
            populateProjectNames();

            // populate tabs panel and create save button
            if (createAndLoadGUI) { // only do this once at the beginning of each session

                // add project information (tabs)
                tabs = new ProjectTabPanel();
                add(tabs);

                // saving projects
                createSaveButtonPanel();
            }
        }
    }

    private void createProjectSelection() {

        // Project Selection
        existingProjButton = new RadioButton("projectGroup", "Edit Existing Project");
        existingProjButton.addClickListener(new ExistingProjListener());
        newProjButton = new RadioButton("projectGroup", "Create New Project");
        newProjButton.addClickListener(new NewProjListener());
        existingProjButton.setValue(true);

        existingProjSelector = new ListBox();
        //addDebugMessage("Existing Project Selector CREATED!");
        existingProjSelector.addChangeListener(new ProjectSelectionChangeListener());
        existingProjSelector.addClickListener(new ProjectSelectionClickListener());
        existingProjSelector.setStyleName("EPTextBox");

        // Attach to GUI

        FlexTable projectSelectorGrid = new FlexTable();
        EPTableUtilities.addWidgetWidgetPair(projectSelectorGrid, 0, 0,
                existingProjButton, existingProjSelector);
        EPTableUtilities.addWidgetWidgetPair(projectSelectorGrid, 1, 0,
                newProjButton, null);

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(projectSelectorGrid);
        add(HtmlUtils.getHtml("&nbsp;", "spacer"));

    }


    protected void createNameSymbolMap() {

        if (_symbolToProjectMap != null && _symbolToProjectMap.size() != 0) {

//            this.addDebugMessage("   symbol-to-project NOT NULL");
            _nameToSymbolMap = new HashMap<String, String>();

            for (String o : _symbolToProjectMap.keySet()) {
                ProjectImpl currProject = _symbolToProjectMap.get(o);
                _nameToSymbolMap.put(currProject.getProjectName(), currProject.getProjectSymbol());
            }
        }
        else {
//            this.addDebugMessage("   symbol-to-project IS NULL!!!!");
            // TODO "NO PROJECTS" MESSAGE / ERROR
        }
    }

    protected void populateProjectNames() {

        // if projects retrieved correctly from database
        if (_projectList != null && _projectList.size() != 0) {

            existingProjSelector.clear();

            // always add "select project" prompt
            existingProjSelector.addItem(ExistingProjSelectorPrompt);

            for (Object a_projectList : _projectList) {
                ProjectImpl currProject = (ProjectImpl) a_projectList;
                // *note: possible cause of stack overflow in GWT testing shell
                existingProjSelector.addItem(currProject.getProjectName());
            }
            existingProjSelector.setEnabled(true);

        }
        else {
            existingProjSelector.setEnabled(false);
        }
    }

    protected void createSaveButtonPanel() {

        saveButton = new RoundedButton("Save", new ClickListener() {
            public void onClick(Widget sender) {
                saveProject();
            }
        });

        saveButton.setEnabled(false);
        saveButton.setStyleName("EPSaveButton");

        HorizontalPanel savePanel = new HorizontalPanel();

        savePanel.setStyleName("EPFullPanel");
        savePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        savePanel.add(saveButton);

        this.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        this.add(savePanel);

    }


    protected void saveProject() {
        System.out.println("   Trying to save project!!!!!");

        boolean nameAvailable = false;
        boolean symbolAvailable = false;
        boolean descriptionAvailable = false;

        // check required fields
        if (tabs.getDetailPanel().getName() != null && !tabs.getDetailPanel().getName().equals("")) {
            nameAvailable = true;
        }
        else {
            //TODO: error popup? message for input name needed
            // todo: maybe try putting all error messages together
        }

        if (tabs.getDetailPanel().getSymbol() != null && !tabs.getDetailPanel().getSymbol().equals("")) {
            symbolAvailable = true;
        }
        else {
            //todo: same as above
        }

        if (tabs.getDescriptionPanel().getDescription() != null && !tabs.getDescriptionPanel().getDescription().equals("")) {
            descriptionAvailable = true;
        }
        else {
            //todo: same as above
        }


        // if all required information available, than save or update project
        if (nameAvailable && symbolAvailable && descriptionAvailable) {

            if (project == null) {
                if (existingProjButton.getValue() && existingProjButton.getText().equals(ExistingProjSelectorPrompt)) {
                    System.out.println(" NO PROJECTS SELECTED!");
                }
                else if (newProjButton.getValue()) {
                    project = new ProjectImpl();
                }
                else {
                    System.out.println(" The Sky is Falling!");
                }
            }


            ProjectDetailsPanel detailPanel = tabs.getDetailPanel();
            ProjectDescriptionPanel descriptionPanel = tabs.getDescriptionPanel();
//            ProjectPublicationPanel publicationPanel = tabs.getPublicationPanel();

            // UPDATE all of current project attributes when it changed

            // project details
            project.setProjectName(detailPanel.getName());
            project.setProjectSymbol("CAM_PROJ_" + detailPanel.getSymbol());
            project.setPrincipalInvestigators(detailPanel.getPI());
            project.setInstitutionalAffiliation(detailPanel.getAffiliation());
            project.setOrganization(detailPanel.getProjectOrganization());
            project.setFundedBy(detailPanel.getFundingOrganization());
            project.setWebsite("http://" + detailPanel.getWebsite());
            project.setEmail(detailPanel.getEmail());

            // project description
            project.setDescription(descriptionPanel.getDescription());

            // project publication
            // todo: set project publications
            //project.setPublications();

            downloadService.saveOrUpdateProject(project, new SaveUpdateProjectCallback(this));


        }
        else {
            //todo: error messages here?
            System.out.println("   ! SOMETHING IS NOT FILLED IN: NAME, SYMBOL, DESCRIPTION");
        }

    }


    public class SaveUpdateProjectCallback implements AsyncCallback {

        EditProjectPanel parentPanel = null;

        public SaveUpdateProjectCallback(EditProjectPanel parentPanel) {
            this.parentPanel = parentPanel;
        }

        public void onFailure(Throwable caught) {
            //finishedPopup();
            System.out.println("SaveUpdateProjectCallback: project saved/updated encountered error");

            addDebugMessage("SaveUpdateProjectCallback: project saved/updated encountered error");

            /*
            addDebugMessage("SaveUpdateProjectCallback: project saved/updated encountered error: " + caught.getMessage());
            addDebugMessage("trace: " + caught.getStackTrace());
            */
        }

        public void onSuccess(Object result) {
            //finishedPopup();
            System.out.println("SaveUpdateProjectCallback: project successfully saved/updated");

            new PopupBelowLauncher(new SaveSuccessPopup("", false)).showPopup(parentPanel);

            // reload / refresh  projects (to reflect change)
            downloadService.getSymbolToProjectMapping(new PopulateProjectsCallback(parentPanel, false));
        }
    }

    public class SaveSuccessPopup extends ModalPopupPanel {

        public SaveSuccessPopup(String title, boolean realizeNow) {
            super(title, realizeNow);
        }

        protected void populateContent() {
            add(HtmlUtils.getHtml("Project Saved Successfully.", "text"));
            add(HtmlUtils.getHtml("&nbsp;", "text"));
        }
    }

    // LISTENERS //

    private class ExistingProjListener implements ClickListener {
        public void onClick(Widget widget) {
            existingProjSelector.setEnabled(true);
            existingProjSelector.setSelectedIndex(0);

            tabs.getDetailPanel().clearInputTextBoxes();
            tabs.getDetailPanel().disableAllInput();
            tabs.getDescriptionPanel().disableDescriptionInput();
        }
    }

    private class NewProjListener implements ClickListener {
        public void onClick(Widget widget) {

            saveButton.setEnabled(true);

            if (existingProjSelector != null) {
                existingProjSelector.setSelectedIndex(0);
//                existingProjSelector.setEnabled(false);
            }

            // clear input panels
            tabs.getDetailPanel().clearInputTextBoxes();
            tabs.getDetailPanel().enableAllInput();
            tabs.getDescriptionPanel().clearDescription();
            tabs.getDescriptionPanel().enableDescriptionInput();

            // set sample data (whale scat project example)
            /*
            tabs.getDetailPanel().setSampleNewProject();
            tabs.getDescriptionPanel().setSampleNewProject();
            tabs.getPreviewPanel().updateProjectView();
            tabs.getPublicationPanel().setSampleNewProject();
            */

        }
    }


    private class ProjectSelectionChangeListener implements ChangeListener {

        public void onChange(Widget widget) {
            ListBox projSelector = (ListBox) widget;

            if (!projSelector.getItemText(projSelector.getSelectedIndex()).equals(
                    ExistingProjSelectorPrompt)) {

                // enable Input fields
                tabs.getDetailPanel().enableAllInput();
                tabs.getDescriptionPanel().enableDescriptionInput();

                saveButton.setEnabled(true);

                String selectedProjSymbol =
                        _nameToSymbolMap.get(projSelector.getItemText(projSelector.getSelectedIndex()));

                // open the existing project and fill in all text boxes accordingly
                project = _symbolToProjectMap.get(selectedProjSymbol);

                tabs.getDetailPanel().updateProjectView(project);

                tabs.getDescriptionPanel().updateProjectView(project);

                tabs.getPreviewPanel().updateProjectView();

                tabs.getPublicationPanel().updateProjectView(project);
            }
        }
    }


    private class ProjectSelectionClickListener implements ClickListener {

        public void onClick(Widget widget) {
            System.out.println("    !!! existing proj should be selected!!!!");
            existingProjButton.setEnabled(true);
        }

    }


    // used for debugging when running outside gwt shell
    private void addDebugMessage(String message) {

        Label label = new Label(message);
        label.setStyleName("titledBoxLabel");
        this.add(label);

    }


}
