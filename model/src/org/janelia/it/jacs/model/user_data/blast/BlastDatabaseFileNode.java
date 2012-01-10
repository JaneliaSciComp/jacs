
package org.janelia.it.jacs.model.user_data.blast;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.DataSource;
import org.janelia.it.jacs.model.user_data.DownloadableFastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 13, 2006
 * Time: 10:32:16 AM
 */
public class BlastDatabaseFileNode extends DownloadableFastaFileNode {

    public transient static final String PEPTIDE = "peptide";
    public transient static final String NUCLEOTIDE = "nucleotide";
    public transient static final String PARTITION_PREFIX = "p";

    // Fields
    protected Integer partitionCount;
    protected String sequenceType; // either PEPTIDE or NUCLEOTIDE
    protected DataSource dataSource = DataSource.UNKNOWN;
    // This field tracks whether this node is for assembled or raw data
    protected Boolean isAssembledData;
    protected Integer sequenceCount = 0;
    protected String decypherDbId;

    public Integer getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Integer partitionCount) {
        this.partitionCount = partitionCount;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    /**
     * default constructor
     */
    public BlastDatabaseFileNode() {
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
     * @param sequenceType        - sequence type of the data
     */
    public BlastDatabaseFileNode(String owner, Task task, String name, String description, String visibility, String sequenceType,
                                 String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceType = sequenceType;
    }

    public long getNodeSize() {
        return getLength();
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean getIsAssembledData() {
        return isAssembledData;
    }

    public void setIsAssembledData(Boolean assembledData) {
        isAssembledData = assembledData;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    public String getDecypherDbId() {
        return decypherDbId;
    }

    public void setDecypherDbId(String decypherDbId)     {
        this.decypherDbId = decypherDbId;
    }

    @Override
    public String getSubDirectory() {
        return "BlastDatabases";
    }

}
