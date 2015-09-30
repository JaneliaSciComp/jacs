
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.blast.CreateBlastDatabaseTask;
import org.janelia.it.jacs.model.tasks.blast.CreateRecruitmentBlastDatabaseTask;
import org.janelia.it.jacs.model.tasks.utility.FtpFileTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.Blastable;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.NodeDAOImpl;
import org.janelia.it.jacs.server.utils.SystemException;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 11:47:31 AM
 */
public class DataSetAPI {
    static Logger logger = Logger.getLogger(DataSetAPI.class.getName());

    NodeDAOImpl dataDao;
    TaskDAO taskDAO;
    ComputeBeanRemote computeBean;
    private ComputeBeanRemote pipelineBean;

    public void setDataDao(NodeDAOImpl dataDao) {
        this.dataDao = dataDao;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public void setComputeBean(ComputeBeanRemote computeBean) {
        this.computeBean = computeBean;
    }


    public void setPipelineBean(ComputeBeanRemote pipelineBean) {
        this.pipelineBean = pipelineBean;
    }

    public ComputeBeanRemote getPipelineBean() {
        return pipelineBean;
    }

    public List getUserSpecificData(String targetUser) throws SystemException {
        try {
            return dataDao.getUserSpecificData(targetUser);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    public Node saveOrUpdateNode(String targetUser, Node targetNode) throws SystemException {
        try {
            logger.info("Pre: Target Node id is " + targetNode.getObjectId());
            // Associate the user to the node
            targetNode.setOwner(targetUser);
            dataDao.saveOrUpdateNode(targetNode);
            logger.info("Post: Target Node id is " + targetNode.getObjectId());
            return dataDao.getNodeById(targetNode.getObjectId());
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public List<BlastHit> getPagedBlastHitsByTaskId(String taskId,
                                                                                                       int startIndex,
                                                                                                       int numRows,
                                                                                                       SortArgument[] sortArgs) {
        try {
            List<Object[]> results =
                    dataDao.getPagedBlastHitsByTaskId(new Long(taskId), startIndex, numRows, false, sortArgs);
            return DataSetAPIRemarshaller.createClientBlastHits(results);
        }
        catch (DaoException e) {
            logger.error("DaoException retrieving BLAST node:" + e.getMessage());
            return null;
        }
        catch (Exception e) {
            logger.error("Exception remarshalling BLAST result:" + e.getMessage());
            return null;
        }
    }


//    /**
//     * @param taskId task in question
//     * @return Map<Site, Integer>
//     */
//    public Map<Site, Integer> getSitesForBlastResult(String taskId) {
//        Map<BioMaterial, Integer> modelSites;
//        try {
//            logger.debug("DataSetAPI.getSitesForBlastResult()");
//            modelSites = dataDao.getSitesForBlastResultNode(new Long(taskId));
//            logger.debug("got sites from DAO");
//            return DataSetAPIRemarshaller.remapSites(modelSites);
//        }
//        catch (DaoException e) {
//            logger.error("DaoException retrieving BLAST node:" + e.getMessage());
//            return null;
//        }
//        catch (Exception e) {
//            logger.error("Exception remarshalling BLAST result:" + e.getMessage());
//            return null;
//        }
//    }
//
    public String replaceNodeName(String nodeId, String nodeName)
            throws SystemException {
        try {
            return dataDao.replaceNodeName(nodeId, nodeName);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    public void deleteNode(User sessionUser, String nodeId)
            throws SystemException {
        try {
            computeBean.deleteNode(sessionUser.getUserLogin(), Long.valueOf(nodeId), true);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public Integer getNumBlastableNodesForUser(String searchString, String sequenceType, String user)
            throws SystemException {
        try {
            return dataDao.getNumBlastableNodesForUser(searchString, sequenceType, user);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    /**
     * The method searches the FastaFileNode(s) for the <code>user</code>
     *
     * @param searchString string to filter the nodes by
     * @param sequenceType potentially filter the nodes by sequence type
     * @param startIndex   index to start from
     * @param numRows      num rows desired
     * @param sortArgs     SortArgument objects to determine ordering
     * @param user         user requested for
     * @return UserDataNodeVO array
     * @throws SystemException System unable to perform action
     */
    public UserDataNodeVO[] getPagedBlastableNodesForUser(String searchString,
                                                          String sequenceType,
                                                          int startIndex,
                                                          int numRows,
                                                          SortArgument[] sortArgs,
                                                          String user)
            throws SystemException {
        try {
            logger.debug(numRows + " requested");
            SortArgument[] actualSortArgs = null;
            if (sortArgs != null) {
                actualSortArgs = new SortArgument[sortArgs.length];
                for (int i = 0; i < sortArgs.length; i++) {
                    String sortField = sortArgs[i].getSortArgumentName();
                    if (sortField == null || sortField.length() == 0) {
                        continue;
                    }
                    if (sortField.equals(UserDataNodeVO.SORT_BY_NODE_ID)) {
                        sortField = "objectId";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_NAME)) {
                        sortField = "name";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_DESCRIPTION)) {
                        sortField = "description";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_TYPE)) {
                        sortField = "sequenceType";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_DATE_CREATED)) {
                        sortField = "objectId"; // we assume this is generated with TimebasedIdentifierGenerator
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_LENGTH)) {
                        sortField = "totalQuerySeqeuenceLength";
                    }
                    else {
                        // unknown or unsupported sort field name
                        continue;
                    }
                    actualSortArgs[i] = new SortArgument(sortField, sortArgs[i].getSortDirection());
                }
            }
            List<Node> nodeList = dataDao.getPagedBlastableNodesForUser(searchString, sequenceType, startIndex, numRows,
                    actualSortArgs, user);
            logger.debug("received " + nodeList.size() + " rows from dataDao.getPagedBlastableNodesForUser()");
            ArrayList<UserDataNodeVO> voList = new ArrayList<UserDataNodeVO>();
            DecimalFormat lengthFormat = new DecimalFormat("###,###");
            for (Object aNodeList : nodeList) {
                Node dn = (Node) aNodeList;
                String parentTaskStatus=getParentTaskStatusOfNode(dn);
                UserDataNodeVO uVo = new UserDataNodeVO(
                        dn.getObjectId() + "",
                        dn.getDescription(),
                        dn.getVisibility(),
                        dn.getDataType(),
                        ((Blastable) dn).getSequenceType(), // if this throws a ClassCastException we have a problem
                        dn.getName(),
                        dn.getOwner() == null ? "" : dn.getOwner(),
                        lengthFormat.format(dn.getLength()), // dn.length should be the totalQuerySequenceLength
                        TimebasedIdentifierGenerator.getTimestamp(dn.getObjectId()),
                        "preview not implemented", // preview not implemented
                        parentTaskStatus);
                voList.add(uVo);
            }
            UserDataNodeVO[] voArr = voList.toArray(new UserDataNodeVO[voList.size()]);
            logger.debug("returning voArr with " + voArr.length + " entires");
            return voArr;
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    public List<String> getBlastableNodeNamesForUser(String searchString, String sequenceType, String user)
            throws SystemException {
        try {
            return dataDao.getBlastableNodeNamesForUser(searchString, sequenceType, user);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

//    public BlastableNodeVO[] getReversePsiBlastDatasets() {
//        try {
//            return dataDao.getReversePsiBlastDatasets();
//        }
//        catch (DaoException daoe) {
//            logger.error(daoe);
//            throw new RuntimeException(daoe);
//        }
//    }
//
    public String submitJob(User sessionUser, Task newTask) throws SystemException {
        String jobId = "Not available";
        try {
            logger.warn("The new task is null:" + (null == newTask));
            logger.info("***********************************");
            logger.info("The user is: " + sessionUser.toString());
            logger.info("***********************************");
            newTask.setOwner(sessionUser.getUserLogin());
            // update the first event date to RIGHT NOW
            newTask.getFirstEvent().setTimestamp(new Date());
            taskDAO.saveOrUpdateTask(newTask);
            // update the job ID after saving it
            jobId = newTask.getObjectId().toString();
            logger.info("Calling submitComputeTask for task id=" + newTask.getObjectId());
            logger.info(newTask.getTaskName());
            String processName = "";
            // todo Tasks should know their own default processes
//            if (newTask instanceof AnalysisPipeline16sTask) {
//                processName = "AnalysisPipeline16S";
//            }
            if (newTask instanceof FtpFileTask) {
                processName = "NCBIFtpFile";
            }
//            else if (newTask instanceof ProkAnnotationDirectoryUpdateTask) {
//                processName = "ProkAnnotationDirectoryUpdate";
//            }
//            else if (newTask instanceof ProkaryoticAnnotationLoadGenomeDataTask) {
//                processName = "ProkAnnotationLoadGenomeData";
//            }
//            else if (newTask instanceof ProkaryoticAnnotationBulkLoadGenomeDataTask) {
//                processName = "ProkAnnotationBulkLoadGenomeData";
//            }
//            // Note: The ordering of the below check is important and must happen before ProkaryoticAnnotationTask below.
//            // If this becomes a problem, refine the check to be based on task name.
//            else if (newTask instanceof ProkaryoticAnnotationBulkTask) {
//                processName = "ProkAnnotationBulk";
//            }
//            else if (newTask instanceof ProkaryoticAnnotationServiceLoadGenomeDataTask) {
//                processName = "ProkAnnotationServiceLoadGenomeData";
//            }
//            else if (newTask instanceof ProkaryoticAnnotationTask) {
//                processName = "ProkAnnotationPipeline";
//            }
//            else if (newTask instanceof MetaGenoOrfCallerTask) {
//                processName = "MetaGenoORFCaller";
//            }
//            else if (newTask instanceof MetaGenoAnnotationTask) {
//                processName = "MetaGenoAnnotation";
//            }
            else if (newTask instanceof CreateBlastDatabaseTask) {
                if (newTask instanceof CreateRecruitmentBlastDatabaseTask) {
                    processName = "CreateRecruitmentBlastDB";
                }
                else {
                    processName = "CreateBlastDB";
                }
            }
//            else if (newTask instanceof ProfileComparisonTask) {
//                processName = "ProfileComparison";
//            }
//            else if (newTask instanceof BarcodeDesignerTask) {
//                processName = "DesignBarcode";
//            }
//            else if (newTask instanceof NeuronSeparatorPipelineTask) {
//                processName = "NeuronSeparatorPipeline";
//            }
//            else if (newTask instanceof GenomeProjectRecruitmentSamplingTask) {
//                processName = "GenomeProjectRecruitmentSampling";
//            }
//            else if (newTask instanceof UploadFastqDirectoryTask) {
//                processName = "UploadFastqDirectory";
//            }
//            else if (newTask instanceof UploadRnaSeqReferenceGenomeTask) {
//                processName = "UploadRnaSeqReferenceGenome";
//            }
//            else if (newTask instanceof RnaSeqPipelineTask) {
//                processName = "RnaSeqPipeline";
//            }
//            else if (newTask instanceof NeuronalAssayAnalysisTask) {
//                processName = "NeuronalAssayAnalysis";
//            }
//            else if (newTask instanceof BatchTicTask) {
//                processName = "TranscriptionImagingConsortiumBatch";
//            }
//            else if (newTask instanceof InspectTask){
//                processName = "Inspect";
//            }
            else if (newTask instanceof GenericTask){
                if ("sampleSync".equals(newTask.getTaskName())) {
                    processName = "SampleFileNodeSync";
                    String tmpOwner = newTask.getParameter("TargetUser");
                    newTask.setOwner(tmpOwner);
                    taskDAO.saveOrUpdateTask(newTask);
                }
            }
            logger.info("DataSetAPI submitJob using processName=" + processName);
            pipelineBean.submitJob(processName, newTask.getObjectId());
            jobId = newTask.getObjectId().toString();
        }
        catch (Throwable e) {
            logger.error("Error returned from Job execution:" + e.getMessage(), e);
            //e.printStackTrace(System.out);
            if (newTask != null) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.length() > 200) {
                    errorMessage = errorMessage.substring(0, 200);
                }
                Event errEvent = new Event("Failed to execute job" + (errorMessage == null ? "" : ": " + errorMessage),
                        new Date(),
                        Event.ERROR_EVENT);
                newTask.addEvent(errEvent);
                try {
                    taskDAO.saveOrUpdateTask(newTask);
                }
                catch (Throwable te) {
                    logger.error("Error updating task failure event:" + te.getMessage(), e);
                    throw new SystemException("Error updating task failure event.");
                }
            }
        }
        return jobId;
    }

    // This method keeps to the "rule" that only the computeserver has access to the filestore
    public List<String> getFiles(String tmpDirectory, boolean directoriesOnly) throws SystemException {
        try {
            return computeBean.getFiles(tmpDirectory, directoriesOnly);
        }
        catch (RemoteException e) {
            logger.error("Unable to obtain directory information:" + e.getMessage(), e);
            throw new SystemException("Unable to obtain directory information.");
        }
    }

    public HashSet<String> getProjectCodes() throws SystemException {
        try {
            return computeBean.getProjectCodes();
        }
        catch (Exception e) {
            logger.error("Unable to obtain project code information:" + e.getMessage(), e);
            throw new SystemException("Unable to obtain directory information.");
        }
    }

    public void validateFilePath(String filePath) throws SystemException {
        try {
            computeBean.validateFile(filePath);
        }
        catch (Exception e) {
            logger.error("Unable to obtain file information:" + e.getMessage(), e);
            throw new SystemException("Unable to obtain file information.");
        }
    }

    public Integer getNumNodesForUserByName(String nodeClassName, String user) throws SystemException {
        try {
            return dataDao.getNumNodesForUserByName(nodeClassName, user);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    public UserDataNodeVO[] getPagedNodesForUserByName(String nodeClassName,
                                                       int startIndex,
                                                       int numRows,
                                                       SortArgument[] sortArgs,
                                                       String user)
            throws SystemException {
        try {
            logger.debug(numRows + " requested");
            SortArgument[] actualSortArgs = null;
            if (sortArgs != null) {
                actualSortArgs = new SortArgument[sortArgs.length];
                for (int i = 0; i < sortArgs.length; i++) {
                    String sortField = sortArgs[i].getSortArgumentName();
                    if (sortField == null || sortField.length() == 0) {
                        continue;
                    }
                    if (sortField.equals(UserDataNodeVO.SORT_BY_NODE_ID)) {
                        sortField = "objectId";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_NAME)) {
                        sortField = "name";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_DESCRIPTION)) {
                        sortField = "description";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_TYPE)) {
                        sortField = "sequenceType";
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_DATE_CREATED)) {
                        sortField = "objectId"; // we assume this is generated with TimebasedIdentifierGenerator
                    }
                    else if (sortField.equals(UserDataNodeVO.SORT_BY_LENGTH)) {
                        sortField = "totalQuerySeqeuenceLength";
                    }
                    else {
                        // unknown or unsupported sort field name
                        continue;
                    }
                    actualSortArgs[i] = new SortArgument(sortField, sortArgs[i].getSortDirection());
                }
            }
            List<Node> nodeList = dataDao.getPagedNodesForUserByName(nodeClassName, startIndex, numRows,
                    actualSortArgs, user);
            logger.debug("received " + nodeList.size() + " rows from dataDao.getPagedNodesForUserByName()");
            ArrayList<UserDataNodeVO> voList = new ArrayList<UserDataNodeVO>();
            DecimalFormat lengthFormat = new DecimalFormat("###,###");
            for (Object aNodeList : nodeList) {
                Node dn = (Node) aNodeList;
                String parentTaskStatus=getParentTaskStatusOfNode(dn);
                UserDataNodeVO uVo = new UserDataNodeVO(
                        dn.getObjectId() + "",
                        dn.getDescription(),
                        dn.getVisibility(),
                        dn.getDataType(),
                        nodeClassName,
                        dn.getName(),
                        dn.getOwner() == null ? "" : dn.getOwner(),
                        lengthFormat.format(dn.getLength()), // dn.length should be the totalQuerySequenceLength
                        TimebasedIdentifierGenerator.getTimestamp(dn.getObjectId()),
                        "preview not implemented",  // preview not implemented
                        parentTaskStatus);
                voList.add(uVo);
            }
            UserDataNodeVO[] voArr = voList.toArray(new UserDataNodeVO[voList.size()]);
            logger.debug("returning voArr with " + voArr.length + " entires");
            return voArr;
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    String getParentTaskStatusOfNode(Node n) {
        Task t = n.getTask();
        String parentTaskStatus = null;
        if (t != null) {
            List<Event> events = t.getEvents();
            if (events != null && events.size() > 0) {
                Event lastEvent = events.get(events.size() - 1);
                parentTaskStatus = lastEvent.getEventType();
            }
        }
        return parentTaskStatus;
    }

    public List<String> getNodeNamesForUserByName(String nodeClassName, String userLogin) throws SystemException {
        try {
            return dataDao.getNodeNamesForUserByName(nodeClassName, userLogin);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    /**
     * I could probably create this task in the web page but oh well.
     * @param sessionUser person asking for the cleanup
     * @param username target cleanup account
     * @throws SystemException thrown when a problem runing the sync occurs
     */
    public void syncUserData(User sessionUser, String username) throws SystemException {
        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        Task task = new GenericTask(new HashSet<Node>(), username, new ArrayList<Event>(),
                taskParameters, "sampleSync", "Sample Sync");
        task.setJobName("MultiColor FlipOut Sample Fileshare Sync Task");
        task.setParameter("TargetUser",username);
        submitJob(sessionUser, task);
    }
}
