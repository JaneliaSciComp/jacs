
package org.janelia.it.jacs.web.gwt.download.client.project;

import org.janelia.it.jacs.web.gwt.download.client.model.Project;

/**
 * @author Michael Press
 */
public interface ProjectSelectedListener {
    public void onSelect(Project project);

    public void onUnSelect(Project project);
}
