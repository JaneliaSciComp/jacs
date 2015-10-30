
package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 6, 2009
 * Time: 11:37:06 AM
 */
public class MetageneResultNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within HmmerPfamResultNode.
    public transient static final String TAG_RAW_OUTPUT = "metagene_raw";
    public transient static final String TAG_BTAB_OUTPUT = "metagene_btab";

    public transient static final String BASE_OUTPUT_FILENAME = "metagene";
    public transient static final String RAW_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_raw";
    public transient static final String BTAB_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_btab";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public MetageneResultNode() {
    }

    public String getSubDirectory() {
        return "MetageneResult";
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
    public MetageneResultNode(String owner, Task task, String name, String description, String visibility,
                              String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_RAW_OUTPUT)) return getFilePath(RAW_OUTPUT_FILENAME);
        if (tag.equals(TAG_BTAB_OUTPUT)) return getFilePath(BTAB_OUTPUT_FILENAME);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
