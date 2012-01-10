
package org.janelia.it.jacs.model.user_data.pangenome;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

public class InparanoidResultFileNode extends FileNode {

    /**
     * default constructor
     */
    public InparanoidResultFileNode() {
    }

    public String getSubDirectory() {
        return "InparanoidResults";
    }

    /**
     * constructor
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     */
    public InparanoidResultFileNode(String owner, Task task, String name, String description, String visibility,
                                    String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
//        if (tag.equals(TAG_CONFIG_FILE)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_CONFIG_FILE);
        return null;
    }

}