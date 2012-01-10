
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 15, 2010
 * Time: 9:52:45 AM
 */
public class TargetpResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within TargetpResultNode.
    public transient static final String TAG_TARGETP_OUTPUT = "out";

    public transient static final String BASE_OUTPUT_FILENAME = "targetp";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public TargetpResultNode() {
    }

    public String getSubDirectory() {
        return "TargetpResult";
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
    public TargetpResultNode(String owner, Task task, String name, String description, String visibility,
                             String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_TARGETP_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_TARGETP_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}