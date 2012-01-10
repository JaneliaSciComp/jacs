
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 15, 2010
 * Time: 1:33:51 PM
 */
public class GenericServiceDefinitionNode extends FileNode {

    public enum serviceTags {
        initialization, execution, finalization, readme
    }

    public GenericServiceDefinitionNode() {
        super();
    }

    public GenericServiceDefinitionNode(String owner, Task task, String name, String description, String visibility,
                                        String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }


    @Override
    public String getSubDirectory() {
        return "GenericServices";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFilePathByTag(String tag) {
        switch (serviceTags.valueOf(tag.toLowerCase())) {
            case initialization:
                return getDirectoryPath().concat("/initialization.sh");
            case execution:
                return getDirectoryPath().concat("/execution.sh");
            case finalization:
                return getDirectoryPath().concat("/finalization.sh");
            case readme:
                return getDirectoryPath().concat("/readme.txt");
            default:
                return null;
        }
    }
}
