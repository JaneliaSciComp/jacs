
package org.janelia.it.jacs.web.gwt.download.client.project;

import org.janelia.it.jacs.web.gwt.download.client.model.Project;

import java.util.Map;

/**
 * @author Michael Press
 */
public interface ProjectRetrievalCallback {
    void onFailure(Throwable throwable);

    void onProjectsRetrieved(Map<String, Project> projects); // map of project symbol to project

    void onProjectRetrieved(Project project);
}
