
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 30, 2010
 * Time: 12:00:00 PM
 */
public class TrfResultNode extends FileNode {

    // Valid files within TrfResultNode.
    public transient static final String TAG_TRF_STDOUT = "out";
    public transient static final String TAG_TRF_DAT = "dat";
    public transient static final String TAG_TRF_HTML = "html";

    public transient static final String BASE_OUTPUT_FILENAME = "trf";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public TrfResultNode() {
    }

    public String getSubDirectory() {
        return "TrfResult";
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
    public TrfResultNode(String owner, Task task, String name, String description, String visibility,
                         String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_TRF_STDOUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_TRF_STDOUT);
        if (tag.equals(TAG_TRF_DAT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_TRF_DAT);
        if (tag.equals(TAG_TRF_HTML)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_TRF_HTML);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}