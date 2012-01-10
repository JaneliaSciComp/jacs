
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: May 4, 2010
 * Time: 2:07:27 PM
 */
public class PrositeScanResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within PrositeScanResultNode.
    public transient static final String TAG_PROSITESCAN_OUTPUT = "out";

    public transient static final String BASE_OUTPUT_FILENAME = "prositescan";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public PrositeScanResultNode() {
    }

    public String getSubDirectory() {
        return "PrositeScanResult";
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
    public PrositeScanResultNode(String owner, Task task, String name, String description, String visibility,
                                 String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_PROSITESCAN_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_PROSITESCAN_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}