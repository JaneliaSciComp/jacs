
package org.janelia.it.jacs.model.user_data.blast;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 13, 2006
 * Time: 10:32:16 AM
 */
public class BlastDatasetNode extends Node implements java.io.Serializable, IsSerializable {

    // Fields
    private Set<BlastDatabaseFileNode> blastDatabaseFileNodes = new HashSet<BlastDatabaseFileNode>();
    // for now we are only dealing with homgeneous datasets
    // if we have to deal w/ heterogenous data we'll have to bring them to
    // a common denominator
    private String sequenceType; // either PEPTIDE or NUCLEOTIDE

    /**
     * default constructor
     */
    public BlastDatasetNode() {
    }

    /**
     * constructor
     */
    public BlastDatasetNode(String owner,
                            Task task,
                            String name,
                            String description,
                            String visibility,
                            String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_AGGREGATE_DATA_TYPE, relativeSessionPath);
    }

    /**
     * constructor
     */
    public BlastDatasetNode(String owner,
                            Task task,
                            String name,
                            String description,
                            String visibility,
                            String sequenceType,
                            String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_AGGREGATE_DATA_TYPE, relativeSessionPath);
        this.sequenceType = sequenceType;
    }

    public Set<BlastDatabaseFileNode> getBlastDatabaseFileNodes() {
        return blastDatabaseFileNodes;
    }

    public void setBlastDatabaseFileNodes(Set<BlastDatabaseFileNode> blastDatabaseFileNodes) {
        this.blastDatabaseFileNodes = blastDatabaseFileNodes;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public void addBlastDatabaseFileNode(BlastDatabaseFileNode blastDatabaseFileNode) {
        this.blastDatabaseFileNodes.add(blastDatabaseFileNode);
    }

    /**
     * @return the sum of all member's sequence length
     */
    public Long getLength() {
        Long totalLength = 0l;
        for (Object blastDatabaseFileNode1 : blastDatabaseFileNodes) {
            BlastDatabaseFileNode blastDatabaseFileNode = (BlastDatabaseFileNode) blastDatabaseFileNode1;
            totalLength = totalLength + blastDatabaseFileNode.getLength();
        }
        return totalLength;
    }

    /**
     * @return the sum of all member's partitions
     */
    public Integer getPartitionCount() {
        Integer totalPartitionCount = 0;
        for (Object blastDatabaseFileNode1 : blastDatabaseFileNodes) {
            BlastDatabaseFileNode blastDatabaseFileNode = (BlastDatabaseFileNode) blastDatabaseFileNode1;
            totalPartitionCount = totalPartitionCount + blastDatabaseFileNode.getPartitionCount();
        }
        return totalPartitionCount;
    }

}
