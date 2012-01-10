
package org.janelia.it.jacs.model.user_data.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 2, 2010
 * Time: 12:22:56 PM
 */
public class TophatResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within TophatResultNode.
    public transient static final String TAG_SAM_OUTPUT = "sam";
    public transient static final String TAG_NOHITS_OUTPUT = "nohits";
    public transient static final String TAG_WIG_OUTPUT = "wig";
    public transient static final String TAG_BED_OUTPUT = "bed";

    public transient static final String BASE_OUTPUT_FILENAME = "tophat";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public TophatResultNode() {
    }

    public String getSubDirectory() {
        return "TophatResult";
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
    public TophatResultNode(String owner, Task task, String name, String description, String visibility,
                            String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_SAM_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_SAM_OUTPUT);
        if (tag.equals(TAG_NOHITS_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_NOHITS_OUTPUT);
        if (tag.equals(TAG_WIG_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_WIG_OUTPUT);
        if (tag.equals(TAG_BED_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_BED_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
