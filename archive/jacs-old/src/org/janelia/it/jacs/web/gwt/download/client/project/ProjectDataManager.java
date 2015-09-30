
package org.janelia.it.jacs.web.gwt.download.client.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * @author Michael Press
 */
public class ProjectDataManager implements IsSerializable, Serializable {
    private String _initialProjectSymbol;
    private Project _currentProject;
    private Map<String, Project> _projects;
    private List<DataRetrievedListener> _listeners;
    private int _currentProjectIndex = -1;
    private int _numProjectsInTable = -1;

    protected static final String LAST_PROJECT_PREF = "lastProject";
    protected static final String LAST_PROJECT_PREF_CATEGORY = "projects";

    private static DownloadMetaDataServiceAsync downloadService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    public ProjectDataManager() {
    }

    public Project getCurrentProject() {
        return _currentProject;
    }

    public void setCurrentProject(Project project) {
        _currentProject = project;
        Preferences.setSubjectPreference(new SubjectPreference(LAST_PROJECT_PREF, LAST_PROJECT_PREF_CATEGORY, project.getProjectSymbol()));
    }

    public void setCurrentProject(ProjectInTable projectInTable) {
        setCurrentProject(projectInTable.getProject());
        setProjectSubsetValues(projectInTable.getSelectedIndex(), projectInTable.getTotalProjectsInTable());
    }

    public String getInitialProjectSymbol() {
        return _initialProjectSymbol;
    }

    public void setInitialProjectSymbol(String initialProjectSymbol) {
        _initialProjectSymbol = initialProjectSymbol;
    }

    /**
     * Retrieves all projects from the server.  Projects are cached for subsequent calls.  Listeners are notified.
     */
    public void retrieveAllProjects() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                downloadService.getSymbolToProjectMapping(new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        notifyFailure(throwable);
                    }

                    public void onSuccess(Object result) {
                        _projects = (Map<String, Project>) result;

                        //TODO: handle if the project doesn't exist anymore
                        // Convert the initial project symbol into an actual project
                        if (_initialProjectSymbol != null && _projects != null)
                            setCurrentProject(_projects.get(_initialProjectSymbol));
                        notifySuccess();
                    }
                });
            }
        });
    }

    public void addDataRetrievedListener(DataRetrievedListener projectsRetrievedListener) {
        if (_listeners == null)
            _listeners = new ArrayList<DataRetrievedListener>();
        _listeners.add(projectsRetrievedListener);
    }

    private void notifySuccess() {
        for (DataRetrievedListener listener : _listeners)
            listener.onSuccess(_projects);
    }

    private void notifyFailure(Throwable throwable) {
        for (DataRetrievedListener listener : _listeners)
            listener.onFailure(throwable);
    }

    /**
     * Supports the concept of identifying the currently selected project as N of M projects in a table that
     * may show a subset of projects.
     *
     * @param currentProjectIndex current project index
     * @param numProjectsInTable  number of projects in the table
     */
    public void setProjectSubsetValues(int currentProjectIndex, int numProjectsInTable) {
        _currentProjectIndex = currentProjectIndex;
        _numProjectsInTable = numProjectsInTable;
    }

    /**
     * Supports the concept of identifying the currently selected project as N of M projects in a table that
     * may show a subset of projects.
     *
     * @return returns the current project index
     */
    public int getCurrentProjectIndexInTable() {
        return _currentProjectIndex;
    }

    /**
     * Supports the concept of identifying the currently selected project as N of M projects in a table that
     * may show a subset of projects.
     *
     * @return returns the number of projects in the table
     */
    public int getNumProjectsInTable() {
        return _numProjectsInTable;
    }

    public int getNumProjects() {
        return _projects.size();
    }
}
