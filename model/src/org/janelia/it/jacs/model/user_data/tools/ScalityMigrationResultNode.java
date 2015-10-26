
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2010
 * Time: 4:00:55 PM
 */
public class ScalityMigrationResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Constructors

    /**
     * default constructor
     */
    public ScalityMigrationResultNode() {
    }

    public String getSubDirectory() {
        return "ScalityMigrationResult";
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
    public ScalityMigrationResultNode(String owner, Task task, String name, String description, String visibility,
                                      String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

}
