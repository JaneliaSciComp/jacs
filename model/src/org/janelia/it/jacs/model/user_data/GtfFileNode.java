
package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2010
 * Time: 4:51:05 PM
 */
public class GtfFileNode extends FileNode implements java.io.Serializable, IsSerializable {

    static Logger logger = Logger.getLogger(FastaFileNode.class.getName());

    public transient static final String TAG_GTF = "gtf";

    // Fields
    private Integer sequenceCount;

    /**
     * default constructor
     */
    public GtfFileNode() {
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
     * @param sequenceCount       - number of sequences with features
     */
    public GtfFileNode(String owner, Task task, String name, String description, String visibility,
                       Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceCount = sequenceCount;
    }

    public String getSubDirectory() {
        return "GtfFiles";
    }

    public String getGtfFilePath() {
        return getFilePath("geneTransferFeatures_" + this.getObjectId() + ".gtf");
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_GTF)) {
            return getGtfFilePath();
        }
        logger.error("GtfFileNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

}
