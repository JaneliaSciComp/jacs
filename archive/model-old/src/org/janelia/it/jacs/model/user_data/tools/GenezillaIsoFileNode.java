
package org.janelia.it.jacs.model.user_data.tools;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 28, 2010
 * Time: 1:54:05 PM
 */
public class GenezillaIsoFileNode extends FileNode implements Serializable, IsSerializable {

    public transient static final String TAG_ISO = "iso";

    // Fields
    private Integer sequenceCount;

    /**
     * default constructor
     */
    public GenezillaIsoFileNode() {
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
     * @param sequenceCount       - number of sequences
     */
    public GenezillaIsoFileNode(String owner, Task task, String name, String description, String visibility,
                                Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceCount = sequenceCount;
    }

    public String getSubDirectory() {
        return "IsoFiles";
    }

    public String getIsoFilePath() {
        return getFilePath(this.getObjectId() + ".iso");
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_ISO)) {
            return getIsoFilePath();
        }
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

}