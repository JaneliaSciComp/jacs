
package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 16, 2007
 * Time: 12:32:39 PM
 */

// note: this class may be more appropriate in the jacs/server package
public class UserDataNodeVO implements IsSerializable, Serializable {

    public static final String SORT_BY_NODE_ID = "nodeid";
    public static final String SORT_BY_DESCRIPTION = "description";
    public static final String SORT_BY_NAME = "name";
    public static final String SORT_BY_TYPE = "type";
    public static final String SORT_BY_DATE_CREATED = "created";
    public static final String SORT_BY_LENGTH = "length";

    private String databaseObjectId;
    private String description;
    private String visibility;
    private String dataType;
    private String sequenceType;
    private String nodeName;
    private String owner;
    private String strLength;
    private String sequencePreview;
    private Date dateCreated;
    private int numOfSeqences;
    private long seqLength; // sane as strLength, but in the numeric form
    private boolean isProcessed;
    private String parentTaskStatus;

    public UserDataNodeVO() {
    }


    public String getParentTaskStatus() {
        return parentTaskStatus;
    }

    public void setParentTaskStatus(String parentTaskStatus) {
        this.parentTaskStatus = parentTaskStatus;
    }

    public UserDataNodeVO(String databaseObjectId,
                          String description,
                          String visibility,
                          String dataType,
                          String sequenceType,
                          String nodeName,
                          String owner,
                          String strLength,
                          Date dateCreated,
                          String sequencePreview,
                          String parentTaskStatus) {
        this.databaseObjectId = databaseObjectId;
        this.description = description;
        this.visibility = visibility;
        this.dataType = dataType;
        this.sequenceType = sequenceType;
        this.nodeName = nodeName;
        this.owner = owner;
        this.strLength = strLength;
        this.dateCreated = dateCreated;
        this.sequencePreview = sequencePreview;
        this.parentTaskStatus = parentTaskStatus;      
    }


    public String getDatabaseObjectId() {
        return databaseObjectId;
    }

    public void setDatabaseObjectId(String databaseObjectId) {
        this.databaseObjectId = databaseObjectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLength() {
        return strLength;
    }

    public void setLength(String length) {
        this.strLength = length;
    }

    public int getSequenceCount() {
        return numOfSeqences;
    }

    public void setSequenceCount(int cnt) {
        this.numOfSeqences = cnt;
    }

    public void setSequenceCount(Integer cnt) {
        try {
            this.numOfSeqences = cnt;
        }
        catch (Throwable t) {
            this.numOfSeqences = 0;
        }
    }

    public long getSequenceLength() {
        return seqLength;
    }

    public void setSequenceLength(long len) {
        this.seqLength = len;
    }

    public void setSequenceLength(Long len) {
        try {
            this.seqLength = len;
        }
        catch (Throwable t) {
            this.seqLength = 0;
        }
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSequencePreview() {
        return sequencePreview;
    }

    public void setSequencePreview(String sequencePreview) {
        this.sequencePreview = sequencePreview;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public String toString() {
        return "UserDataNodeVO{" +
                "databaseObjectId='" + databaseObjectId + '\'' +
                ", description='" + description + '\'' +
                ", visibility='" + visibility + '\'' +
                ", dataType='" + dataType + '\'' +
                ", sequenceType='" + sequenceType + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", owner='" + owner + '\'' +
                ", length='" + strLength + '\'' +
                ", dateCreated='" + dateCreated + '\'' +
                ", sequencePreview='" + sequencePreview + '\'' +
                ", parentTaskStatus='" + parentTaskStatus + '\'' +
                ", isProcessed='" + isProcessed + '\'' +
                '}';
    }

}
