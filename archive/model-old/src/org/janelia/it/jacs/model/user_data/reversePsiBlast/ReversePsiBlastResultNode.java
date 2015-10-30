
package org.janelia.it.jacs.model.user_data.reversePsiBlast;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 18, 2008
 * Time: 4:34:16 PM
 */
public class ReversePsiBlastResultNode extends FileNode implements IsSerializable, Serializable {
    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within ReversePsiBlastResultNode.
    public transient static final String TAG_TEXT_OUTPUT = "text";
    public transient static final String TEXT_OUTPUT_FILENAME = "rpsBlast.out";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public ReversePsiBlastResultNode() {
    }

    public String getSubDirectory() {
        return "ReversePsiBlastResult";
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
    public ReversePsiBlastResultNode(String owner, Task task, String name, String description, String visibility,
                                     String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_TEXT_OUTPUT)) return getFilePath(TEXT_OUTPUT_FILENAME);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
