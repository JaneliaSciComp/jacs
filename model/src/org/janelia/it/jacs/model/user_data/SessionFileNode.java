
package org.janelia.it.jacs.model.user_data;

import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 21, 2009
 * Time: 2:45:40 PM
 */
public class SessionFileNode extends FileNode {

    public SessionFileNode() {
    }

    public SessionFileNode(String owner, Task task, String name, String description, String visibility,
                           String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    @Override
    public String getSubDirectory() {
        return "WorkSession";
    }

    @Override
    public String getFilePathByTag(String tag) {
        return null;
    }
}
