
package org.janelia.it.jacs.web.gwt.download.client.project;

import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * @author Michael Press
 */
public interface ProjectSelectedInTableListener {
    public void onSelect(Project project, int selectedProjectIndex, int numProjectsInTable);

    public void onUnSelect(Project project);
}