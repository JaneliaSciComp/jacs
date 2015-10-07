
package org.janelia.it.jacs.model.user_data.sift;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: zguan
 * Date: Jul 21, 2010
 * Time: 10:31:24 AM
 */
public class SiftProteinSubstitutionResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within SiftProteinSubstitutionResultNode.
    public transient static final String TAG_SIFTPROTEINSUBSTITUTION_OUTPUT = "out";

    public transient static final String BASE_OUTPUT_FILENAME = "siftProteinSubstitution";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public SiftProteinSubstitutionResultNode() {
    }

    public String getSubDirectory() {
        return "SiftProteinSubstitutionResult";
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
    public SiftProteinSubstitutionResultNode(String owner, Task task, String name, String description, String visibility,
                                             String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_SIFTPROTEINSUBSTITUTION_OUTPUT))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_SIFTPROTEINSUBSTITUTION_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
