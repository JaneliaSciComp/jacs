
package org.janelia.it.jacs.model.user_data.pasa;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 12, 2010
 * Time: 10:22:30 AM
 */
public class PasaResultNode extends FileNode {

    public transient static final String TAG_CONFIG_FILE = "config";

    public transient static final String BASE_OUTPUT_FILENAME = "pasa";

    // Constructors

    /**
     * default constructor
     */
    public PasaResultNode() {
    }

    public String getSubDirectory() {
        return "PasaResult";
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
    public PasaResultNode(String owner, Task task, String name, String description, String visibility,
                          String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_CONFIG_FILE)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CONFIG_FILE);
        return null;
    }

}
