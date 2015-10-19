
package org.janelia.it.jacs.web.gwt.download.client.project;

import java.util.List;

/**
 * @author Michael Press
 */
public interface ProjectsSelectedListener {
    public void onSelect(List<String> projectNames);
}