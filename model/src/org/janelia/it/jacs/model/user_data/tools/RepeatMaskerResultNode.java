
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 14, 2010
 * Time: 1:07:27 PM
 */
public class RepeatMaskerResultNode extends FileNode {

    // Valid files within RepeatMaskerResultNode.
    public transient static final String TAG_REPEATMASKER_OUT = "out";
    public transient static final String TAG_REPEATMASKER_ALIGN = "align";
    public transient static final String TAG_REPEATMASKER_MASKED = "masked";
    public transient static final String TAG_REPEATMASKER_CAT = "cat";
    public transient static final String TAG_REPEATMASKER_TBL = "tbl";
    public transient static final String TAG_REPEATMASKER_STDOUT = "stdout";
    public transient static final String TAG_REPEATMASKER_STDERR = "stderr";

    public transient static final String BASE_OUTPUT_FILENAME = "repeatMasker";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public RepeatMaskerResultNode() {
    }

    public String getSubDirectory() {
        return "RepeatMaskerResult";
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
    public RepeatMaskerResultNode(String owner, Task task, String name, String description, String visibility,
                                  String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_REPEATMASKER_OUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_OUT);
        if (tag.equals(TAG_REPEATMASKER_ALIGN)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_ALIGN);
        if (tag.equals(TAG_REPEATMASKER_MASKED))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_MASKED);
        if (tag.equals(TAG_REPEATMASKER_CAT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_CAT);
        if (tag.equals(TAG_REPEATMASKER_TBL)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_TBL);
        if (tag.equals(TAG_REPEATMASKER_STDERR))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_STDERR);
        if (tag.equals(TAG_REPEATMASKER_STDOUT))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_REPEATMASKER_STDOUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}