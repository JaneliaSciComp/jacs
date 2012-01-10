
package org.janelia.it.jacs.model.user_data.tools;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 28, 2010
 * Time: 2:12:09 PM
 */
public class CddSearchResultNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String BASE_OUTPUT_FILENAME = "cddsearch";

    // Valid files within CddSearchResultNode.
    public transient static final String TAG_CDDSEARCH_OUTPUT = "out";

    // Constructors

    /**
     * default constructor
     */
    public CddSearchResultNode() {
    }

    public String getSubDirectory() {
        return "CddSearchResult";
    }

    protected long hitCount;

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
    public CddSearchResultNode(String owner, Task task, String name, String description, String visibility,
                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_CDDSEARCH_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CDDSEARCH_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
