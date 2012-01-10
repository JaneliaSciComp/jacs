
package org.janelia.it.jacs.web.gwt.download.client.project;

import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * Stores information about a project in the context of a paged table.  The selectedIndex is a 1-based index
 * suitable for display (as in "Project 1 of N")
 *
 * @author Michael Press
 */
public class ProjectInTable {
    private Project _project;
    private int _selectedIndex;
    private int _totalProjectsInTable;

    public ProjectInTable(Project project, int selectedIndex, int totalProjectsInTable) {
        _project = project;
        _selectedIndex = selectedIndex;
        _totalProjectsInTable = totalProjectsInTable;
    }

    public Project getProject() {
        return _project;
    }

    public int getSelectedIndex() {
        return _selectedIndex;
    }

    public int getTotalProjectsInTable() {
        return _totalProjectsInTable;
    }
}
