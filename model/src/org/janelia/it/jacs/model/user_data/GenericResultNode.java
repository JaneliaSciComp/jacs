
package org.janelia.it.jacs.model.user_data;

import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 22, 2010
 * Time: 9:26:32 AM
 */
public class GenericResultNode extends GenericFileNode {

    public GenericResultNode() {
        super();
    }

    public GenericResultNode(String owner, Task task, String name, String description, String visibility,
                             String relativeFilePath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeFilePath);
    }

    public String getSubDirectory() {
        return "GenericResults";
    }

}
