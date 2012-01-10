
package org.janelia.it.jacs.shared.processors.recruitment;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 9, 2008
 * Time: 12:40:50 AM
 */
public class ProjectData implements Comparable, IsSerializable {

    private String projectName;
    private String projectDescription;

    public ProjectData() {
    }

    public ProjectData(String projectName, String projectDescription) {
        this.projectDescription = projectDescription;
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public int compareTo(Object o) {
        ProjectData pd2 = (ProjectData) o;
        int compared = this.projectDescription.compareTo(pd2.getProjectDescription());
        if (0 != compared) {
            return compared;
        }
        else {
            return this.projectName.compareTo(pd2.getProjectName());
        }
    }
}
