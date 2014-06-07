
package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.Blastable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 14, 2006
 * Time: 1:13:37 PM
 */
public class FragmentFileNode extends FileNode implements Serializable, IsSerializable, Blastable {

    static Logger logger = Logger.getLogger(FragmentFileNode.class.getName());

    public transient static final String TAG_FRAGMENT = "frg";

    public transient static final String PEPTIDE = "peptide";
    public transient static final String NUCLEOTIDE = "nucleotide";

    // Fields
    private String sequenceType; // either PEPTIDE or NUCLEOTIDE
    private Integer sequenceCount;

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    /**
     * default constructor
     */
    public FragmentFileNode() {
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
     * @param type                - sequence type for the node
     * @param sequenceCount       - the number of sequences in the file
     */
    public FragmentFileNode(String owner, Task task, String name, String description, String visibility,
                            String type, Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceType = type;
        this.sequenceCount = sequenceCount;
    }

    public String getSubDirectory() {
        return "";
    }

    public String getFragmentFilePath() {
        return getFilePath(sequenceType + "." + TAG_FRAGMENT);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_FRAGMENT)) {
            return getFilePath(sequenceType + "." + TAG_FRAGMENT);
        }
        logger.error("FragmentFileNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

}