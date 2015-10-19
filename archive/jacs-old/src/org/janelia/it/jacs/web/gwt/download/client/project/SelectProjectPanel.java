
package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.InfoActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOracleListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class SelectProjectPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserBaseTab");

    protected Map<String, Project> _projects;
    private SortableTable _table;
    private PagingPanel _pagingPanel;
    private SearchOraclePanel _oraclePanel;
    private Project _selectedProject;
    private ProjectSelectedInTableListener _projectSelectionListener;
    private ProjectSelectionCancelledListener _cancelListener;
    private RoundedButton _applyButton;

    private static final int DEFAULT_NUM_ROWS = 10;
    private static final int PROJECT_SYMBOL_COLUMN = 0;
    private static final int PROJECT_NAME_COLUMN = 1;
    private static final int PI_COLUMN = 2;
    private static final int ORG_COLUMN = 3;
    //private static final int HABITAT_COLUMN        = 4;

    private static final String PROJECT_NAME_HEADING = "Project";
    private static final String PI_HEADING = "Principal Investigator";
    private static final String ORG_HEADING = "Organization";
    //private static final String HABITAT_HEADING      = "Habitat";
    private static final int ORG_SIZE = 50;

    public SelectProjectPanel(ProjectSelectedInTableListener projectSelectedListener, ProjectSelectionCancelledListener cancelListener) {
        this(projectSelectedListener, cancelListener, TitledBoxFactory.BoxType.CLEAR_BOX);
    }

    public SelectProjectPanel(ProjectSelectedInTableListener projectSelectedListener, ProjectSelectionCancelledListener cancelListener,
                              TitledBoxFactory.BoxType searchOracleBoxType) {
        _projectSelectionListener = projectSelectedListener;
        _cancelListener = cancelListener;
        init(searchOracleBoxType);
    }

    private void init(TitledBoxFactory.BoxType boxType) {
        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "tinySpacer"));
        mainPanel.add(getSearchArea(boxType));
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(getTable());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        mainPanel.add(createButtons());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer")); // need gap under buttons

        initWidget(mainPanel);
    }

    public void clearSelect() {
        _table.clearHover();
        _table.clearSelect();
    }

    public void setProjects(Map<String, Project> projects) {
        _projects = projects;
        populateTable(_projects.values());
        populateSuggestOracle(_projects.values());
    }

    private Widget createButtons() {
        _applyButton = new RoundedButton("Apply", new ClickListener() {
            public void onClick(Widget sender) {
                notifySelectionListener();
            }
        });
        _applyButton.setEnabled(false);

        RoundedButton cancelButton = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget sender) {
                notifyCancelListener();
            }
        });

        Panel panel = new CenteredWidgetHorizontalPanel();
        panel.add(_applyButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(cancelButton);

        return panel;
    }

    /**
     * Converts a selected table row index to a Project by retrieving the project symbol from the selected
     * row and using it as the key into the projects map
     */
    private Project getSelectedProject() {
        return _projects.get(_table.getSelectedRow().getRowObject().getTableCell(PROJECT_SYMBOL_COLUMN).getValue().toString());
    }

    private Widget getSearchArea(TitledBoxFactory.BoxType boxType) {
        // Create the oracle and hook up callbacks that will repopulate the table
        _oraclePanel = new SearchOraclePanel("Search Projects", "Project Name", boxType, /*show content*/ false,
                new SearchOracleListener() {
                    public void onRunSearch(String searchString) {
                        populateTable(findMatchingProjects(searchString));
                    }

                    public void onShowAll() {
                        populateTable(_projects.values());
                    }
                });
        _oraclePanel.addSuggestBoxStyleName("ProjectsSuggestBox ");

        //TODO: make this configurable without being too ugly
        _oraclePanel.getTitledBox().setActionLinkBackgroundStyleName("secondaryTitledBoxActionLinkBackground");

        return _oraclePanel;
    }

    protected List<Project> findMatchingProjects(String searchString) {
        //TODO: make the oracle return a non-SQL search string or make this code better
        //TODO: make a generic comparison for the oracle when using local paginator?
        boolean useContains = false;
        if (searchString.startsWith("%")) // oracle returns "%x%" if value typed or x% if index letter clicked
            useContains = true;
        searchString = searchString.replaceAll("%", "").toLowerCase();
        List<Project> matchingProjects = new ArrayList();
        for (Project project : _projects.values()) {
            if (useContains) {
                if (project.getProjectName().toLowerCase().contains(searchString))
                    matchingProjects.add(project);
            }
            else if (project.getProjectName().toLowerCase().startsWith(searchString))
                matchingProjects.add(project);
        }
        return matchingProjects;
    }

    protected Widget getTable() {
        _table = new SortableTable();
        _table.setWidth("100%");
        _table.setHighlightSelect(true);
        _table.addSelectionListener(new RowSelectedListener());
        _table.addDoubleClickSelectionListener(new RowSelectedWithDoubleClickListener());
        _table.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(PROJECT_NAME_COLUMN, PROJECT_NAME_HEADING, SortableColumn.SORT_ASC)
        });

        _table.addColumn(new TextColumn("", false, false)); // hidden
        _table.addColumn(new TextColumn(PROJECT_NAME_HEADING));
        _table.addColumn(new TextColumn(PI_HEADING));
        _table.addColumn(new TextColumn(ORG_HEADING));
        //_table.addColumn(new TextColumn(HABITAT_HEADING));

        _pagingPanel = new PagingPanel(_table, "SelectProject", DEFAULT_NUM_ROWS);
        _pagingPanel.setNoDataMessage("No projects found.");

        VerticalPanel panel = new VerticalPanel();
        panel.add(_pagingPanel);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(HtmlUtils.getHtml("&bull;&nbsp;Click once to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint"));

        return panel;
    }

    public Map<String, Project> getProjects() {
        return _projects;
    }

    /**
     * When a table row is selected, determine the project and store it until Apply is hit
     */
    private class RowSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            _selectedProject = getSelectedProject();
            _applyButton.setEnabled(true);
        }

        public void onUnSelect(String value) {
            _selectedProject = null;
            _applyButton.setEnabled(false);
        }
    }

    private class RowSelectedWithDoubleClickListener implements DoubleClickSelectionListener {
        public void onSelect(String value) {
            _selectedProject = getSelectedProject();
            notifySelectionListener();
        }
    }

    protected void notifySelectionListener() {
        int selectedIndex = getRowForProject(_selectedProject, ((List) _pagingPanel.getPaginator().getData())) + 1;
        int totalRowsInTable = _pagingPanel.getPaginator().getTotalRowCount();
        _projectSelectionListener.onSelect(_selectedProject, selectedIndex, totalRowsInTable);
    }

    protected void notifyCancelListener() {
        if (_cancelListener != null)
            _cancelListener.onCancel(_pagingPanel.getPaginator().getTotalRowCount());
    }

    private List<TableRow> processProjects(Collection<Project> projects) {
        List tableRows = new ArrayList();
        if (_projects == null)
            return tableRows;

        for (Project project : projects) {
            TableRow tableRow = new TableRow();
            tableRow.setRowObject(project);
            tableRow.setValue(PROJECT_SYMBOL_COLUMN, new TableCell(project.getProjectSymbol())); // hidden
            tableRow.setValue(PROJECT_NAME_COLUMN, new TableCell(project.getProjectName(), createNameWidget(project)));
            tableRow.setValue(PI_COLUMN, new TableCell(project.getPrincipalInvestigators()));
            tableRow.setValue(ORG_COLUMN,
                    new TableCell(project.getOrganization(), new FulltextPopperUpperHTML(project.getOrganization(), ORG_SIZE)));
            //tableRow.setValue(HABITAT_COLUMN, null); //TODO: get Habitat

            tableRows.add(tableRow);
        }

        return tableRows;
    }

    private Widget createNameWidget(Project project) {
        HorizontalPanel panel = new HorizontalPanel();

        // Create the info popup panel; position just to the right of the info icon so there's no flashing problem in FF
        InfoActionLink infoLink = new InfoActionLink(new ProjectInfoPopup("Project Info", project, /*realizeNow*/ false));
        infoLink.setLauncher(new PopupAtRelativePixelLauncher(/*top adjustment*/0, /*left adjustment*/17, /*msDelay*/ 250));

        panel.add(infoLink);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        panel.add(HtmlUtils.getHtml(project.getProjectName(), "text", "nowrap"));

        return panel;
    }

    protected void populateTable(Collection<Project> projects) {
        _pagingPanel.clear();
        _pagingPanel.getPaginator().setData(processProjects(projects));
        _pagingPanel.getSortableTable().sort();
        _pagingPanel.first();
    }

    protected void populateSuggestOracle(Collection<Project> projects) {
        for (Project project : projects)
            _oraclePanel.addOracleSuggestion(project.getProjectName());
    }

    /**
     * Returns the next project, where "next" is the next project in the data table, as it is currently subsetted
     * (by the oracle) and sorted; only the paginator can know this set of projects, since it controls the paging
     * of the table.
     */
    public ProjectInTable getNextProjectInTable(Project currentProject) {
        // Get the TableRows in the all rows  of the current table (even other pages)
        List<TableRow> projectsInTable = ((List) _pagingPanel.getPaginator().getData());
        if (projectsInTable.size() == 0)
            return null; // error, throw an exception?

        // If the current project not in table, just return the first project
        int currentIndex = getRowForProject(currentProject, projectsInTable); // 0-based index
        if (currentIndex < 0) {
            _logger.debug("getNextProject: defaulting to first project");
            return new ProjectInTable((Project) projectsInTable.get(0).getRowObject(), 1, projectsInTable.size());
        }

        // Figure out the row of the next project and return the project
        int nextIndex = (currentIndex + 1) % projectsInTable.size();
        _logger.debug("getNextProject: returning project in row " + nextIndex);
        return new ProjectInTable((Project) projectsInTable.get(nextIndex).getRowObject(), nextIndex + 1, projectsInTable.size());
    }

    /**
     * Returns the prev project, where "prev" is the prev project in the data table, as it is currently subsetted
     * (by the oracle) and sorted; only the paginator can know this set of projects, since it controls the paging
     * of the table.
     */
    public ProjectInTable getPrevProjectInTable(Project currentProject) {
        // Get the TableRows in the all rows  of the current table (even other pages)
        List<TableRow> projectsInTable = ((List) _pagingPanel.getPaginator().getData());
        if (projectsInTable.size() == 0)
            return null; // error, throw an exception?

        // Figure out the row of the current project.  If not found, just go to first project
        int currentIndex = getRowForProject(currentProject, projectsInTable);
        if (currentIndex < 0) { // current project not found in table
            _logger.debug("getPrevProject: current project not found, defaulting to first project");
            return new ProjectInTable((Project) projectsInTable.get(0).getRowObject(), 1, projectsInTable.size());
        }

        // Figure out the row of the next project and return the project
        int prevIndex = (currentIndex - 1) % projectsInTable.size();
        if (prevIndex < 0) // wrapped, so return last project
            prevIndex = projectsInTable.size() - 1;
        _logger.debug("getPrevProject: returning project in row " + prevIndex);

        return new ProjectInTable((Project) projectsInTable.get(prevIndex).getRowObject(), prevIndex + 1, projectsInTable.size());
    }

    private int getRowForProject(Project currentProject, List<TableRow> projectsInTable) {
        int currentIndex = -1;
        int i = 0;

        for (TableRow row : projectsInTable) {
            if (currentProject.equals(row.getRowObject()))
                currentIndex = i;
            else
                i++;
        }
        _logger.debug("getNextProject: current project is row " + currentIndex);

        return currentIndex;
    }

    public ProjectSelectionCancelledListener getCancelListener() {
        return _cancelListener;
    }

    public SearchOraclePanel getOraclePanel() {
        return _oraclePanel;
    }

    public RoundedButton getApplyButton() {
        return _applyButton;
    }
}