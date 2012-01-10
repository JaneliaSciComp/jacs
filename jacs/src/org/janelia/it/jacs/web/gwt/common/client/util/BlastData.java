
package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Holds all the data associated with a Blast Job
 */
public class BlastData implements Serializable, IsSerializable {

    private Task _blastTask;
    private String _jobNum = "";
    private Task[] _taskArray = new Task[0];
    private String userReferenceFASTA = "";
    private String mostRecentlySelectedQuerySequenceType;
    private String _mostRecentlySelectedSubjectSequenceType;
    private String _mostRecentlySpecifiedQuerySequenceName;
    private String _taskIdFromParam;
    private String _datasetFromParam;

    /**
     * sibjectSequnceDataNodeMap contains the selected subject(s) for the BLAST
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     * key = name of the data node, value = db object id of the node
     */
    private HashMap<String, BlastableNodeVO> subjectSequenceDataNodeMap = new HashMap<String, BlastableNodeVO>();
    private HashMap<String, UserDataNodeVO> querySequenceDataNodeMap = new HashMap<String, UserDataNodeVO>();

    public Task getBlastTask() {
        return _blastTask;
    }

    public void setBlastTask(Task blastTask) {
        _blastTask = blastTask;
    }

    public void setJobNumber(String jobNum) {
        _jobNum = jobNum;
    }

    public String getJobNum() {
        return _jobNum;
    }

    public Task[] getTaskArray() {
        return _taskArray;
    }

    public void setTaskArray(Task[] taskArray) {
        this._taskArray = taskArray;
    }

    public HashMap<String, BlastableNodeVO> getSubjectSequenceDataNodeMap() {
        return subjectSequenceDataNodeMap;
    }

    public void setSubjectSequenceDataNodeMap(HashMap<String, BlastableNodeVO> subjectSequenceDataNodeMap) {
        this.subjectSequenceDataNodeMap = subjectSequenceDataNodeMap;
    }

    public HashMap<String, UserDataNodeVO> getQuerySequenceDataNodeMap() {
        return querySequenceDataNodeMap;
    }

    public void setQuerySequenceDataNodeMap(HashMap<String, UserDataNodeVO> querySequenceDataNodeMap) {
        this.querySequenceDataNodeMap = querySequenceDataNodeMap;
    }

    public String getUserReferenceFASTA() {
        return userReferenceFASTA;
    }

    public void setUserReferenceFASTA(String userReferenceFASTA) {
        this.userReferenceFASTA = userReferenceFASTA;
    }

    public String getSubjectSequenceNodesNamesString() {
        StringBuffer sbuf = new StringBuffer();
        for (Iterator iterator = subjectSequenceDataNodeMap.values().iterator(); iterator.hasNext();) {
            BlastableNodeVO blastableNode = (BlastableNodeVO) iterator.next();
            sbuf.append(blastableNode.getNodeName());
            if (iterator.hasNext()) {
                sbuf.append(",");
            }
        }
        return sbuf.toString();
    }

    /**
     * @return the list of IDs of the selected datasets -
     *         they may be IDs of aggregated nodes or IDs of blastDatabaseFileNodes
     */
    public List<String> getSubjectDatasetIdsList() {
        List<String> returnList = new ArrayList<String>();
        for (Object o : subjectSequenceDataNodeMap.values()) {
            BlastableNodeVO blastableNode = (BlastableNodeVO) o;
            returnList.add(blastableNode.getDatabaseObjectId());
        }
        return returnList;
    }

    public String getMostRecentlySelectedQuerySequenceType() {
        return mostRecentlySelectedQuerySequenceType;
    }

    public void setMostRecentlySelectedQuerySequenceType(String mostRecentlySelectedQuerySequenceType) {
        this.mostRecentlySelectedQuerySequenceType = mostRecentlySelectedQuerySequenceType;
    }

    public String getMostRecentlySelectedSubjectSequenceType() {
        return _mostRecentlySelectedSubjectSequenceType;
    }

    public void setMostRecentlySelectedSubjectSequenceType(String _mostRecentlySelectedSubjectSequenceType) {
        this._mostRecentlySelectedSubjectSequenceType = _mostRecentlySelectedSubjectSequenceType;
    }

    public String getMostRecentlySpecifiedQuerySequenceName() {
        return _mostRecentlySpecifiedQuerySequenceName;
    }

    public void setMostRecentlySpecifiedQuerySequenceName(String _mostRecentlySpecifiedQuerySequenceName) {
        this._mostRecentlySpecifiedQuerySequenceName = _mostRecentlySpecifiedQuerySequenceName;
    }

    public void setTaskIdFromParam(String id) {
        _taskIdFromParam = id;
    }

    public String getTaskIdFromParam() {
        return _taskIdFromParam;
    }

    public void setDatasetFromParam(String dataset) {
        _datasetFromParam = dataset;
    }

    public String getDatasetFromParam() {
        return _datasetFromParam;
    }
}