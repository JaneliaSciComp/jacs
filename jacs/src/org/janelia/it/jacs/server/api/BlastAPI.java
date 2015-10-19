
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;
import org.janelia.it.jacs.server.access.NodeDAO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.UserDAOImpl;
import org.janelia.it.jacs.server.utils.SystemException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 22, 2006
 * Time: 5:06:29 PM
 */
public class BlastAPI {

    static Logger logger = Logger.getLogger(BlastAPI.class.getName());
    TaskDAO taskDAO;
    NodeDAO nodeDAO;
    UserDAO userDAO;
    ComputeBeanRemote computeBean;

    public void setComputeBean(ComputeBeanRemote computeBean) {
        this.computeBean = computeBean;
    }

    public BlastAPI() {
    }

    public void setUserDAO(UserDAOImpl userDAO) {
        this.userDAO = userDAO;
    }

    public TaskDAO getTaskDAO() {
        return taskDAO;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public NodeDAO getNodeDAO() {
        return nodeDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        this.nodeDAO = nodeDAO;
    }

    public BlastTask[] getBlastPrograms(String querySequenceType, String subjectSequenceType) {
        logger.debug("Returning the program list based on queryType=" + querySequenceType + " subjectType=" + subjectSequenceType);
        ArrayList<BlastTask> tmpPrograms = new ArrayList<BlastTask>();
        if (querySequenceType.equals(SequenceType.NUCLEOTIDE)) {
            if (subjectSequenceType.equals(SequenceType.NUCLEOTIDE)) {
                tmpPrograms.add(new BlastNTask());
                tmpPrograms.add(new MegablastTask());
                tmpPrograms.add(new TBlastXTask());
            }
            else if (subjectSequenceType.equals(SequenceType.PEPTIDE)) {
                tmpPrograms.add(new BlastXTask());
            }
        }
        else if (querySequenceType.equals(SequenceType.PEPTIDE)) {
            if (subjectSequenceType.equals(SequenceType.NUCLEOTIDE)) {
                tmpPrograms.add(new TBlastNTask());
            }
            else if (subjectSequenceType.equals(SequenceType.PEPTIDE)) {
                tmpPrograms.add(new BlastPTask());
            }
        }
        BlastTask[] blastPrograms = new BlastTask[tmpPrograms.size()];
        tmpPrograms.toArray(blastPrograms);

        // Returning the array for GWT usage
        logger.debug("getBlastPrograms returning array of size=" + blastPrograms.length);
        return blastPrograms;
    }

    public Task getBlastTaskById(Long taskId) throws SystemException {
        // go to the DAO and grab the object required
        try {
            return taskDAO.getTaskById(taskId);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

    /**
     * This method checks to see if the task corresponding to taskId has completed.
     * Currently, only used by BlastAPITest. Can probably be done another way.
     *
     * @param taskId - task id to check status on
     * @return boolean if the job is done
     * @throws SystemException - problem checking the job status
     */
    public boolean isTaskDone(Long taskId) throws SystemException {
        try {
            return taskDAO.isDone(taskId);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public String runBlast(User requestingUser, Task newBlastTask) throws SystemException {
        String blastJobId = "Not available";
        try {
            logger.warn("The new blast task is null:" + (null == newBlastTask));
            logger.info("***********************************");
            logger.info("Query Node Id = " + newBlastTask.getParameterVO(BlastTask.PARAM_query));
            logger.info("Subject Node Ids Are = " + newBlastTask.getParameterVO(BlastTask.PARAM_subjectDatabases));
            logger.info("The user is: " + requestingUser.toString());
            logger.info("***********************************");
            newBlastTask.setOwner(requestingUser.getUserLogin());
            // update the first event date to RIGHT NOW
            newBlastTask.getFirstEvent().setTimestamp(new Date());
            taskDAO.saveOrUpdateTask(newBlastTask);
            // update the job ID after saving it
            blastJobId = newBlastTask.getObjectId().toString();
            logger.info("Calling submitComputeTask for task id=" + newBlastTask.getObjectId());
            logger.info(newBlastTask.getTaskName());
            TextParameterVO refSeqParam = (TextParameterVO) newBlastTask.getParameterVO(BlastTask.PARAM_query);
            MultiSelectVO subjSeqParam = (MultiSelectVO) newBlastTask.getParameterVO(BlastTask.PARAM_subjectDatabases);
            if (logger.isInfoEnabled()) {
                try {
                    logger.info("Reference Seq: " + refSeqParam.getTextValue());
                    if (subjSeqParam.getPotentialChoices() != null) {
                        for (Object o : subjSeqParam.getPotentialChoices()) {
                            logger.info("Subj. Seq. Object=" + o.toString());
                        }
                    }
                    for (Object o : newBlastTask.getEvents()) {
                        logger.info("Events=" + o.toString());
                    }
                    logger.info("Task id before submission:" + newBlastTask.getObjectId().toString());
                }
                catch (Exception e) {
                    logger.error("Unable to log the blast information: " + e.getMessage());
                }
            }
            String blastProcessName = SystemConfigurationProperties.getString("BlastServer.ProcessName");
            // Check the location of the blast db.  May be remote.
            try {
                List<String> dbs = subjSeqParam.getActualUserChoices();
//                if (newBlastTask instanceof ReversePsiBlastTask) {
//                    blastProcessName = "ReversePsiBlast";
//                }
//                else if (newBlastTask instanceof PsiBlastTask) {
//                    blastProcessName = "PsiBlast";
//                }
                if (null != dbs && null != dbs.get(0)) {
                    BlastDatabaseFileNode tmpNode = (BlastDatabaseFileNode) computeBean.getNodeById(Long.valueOf(dbs.get(0)));
                    // Is replicated means the blast db can be executed offsite
                    if (null != tmpNode && tmpNode.getIsReplicated()) {
                        blastProcessName = SystemConfigurationProperties.getString("BlastServer.RemoteProcessName");
                    }
                }
            }
            catch (Exception e) {
                logger.error("There was an error determining where to run the blast job.");
                throw e;
            }

            logger.info("BlastAPI runBlast using blastProcessName=" + blastProcessName);
            computeBean.submitJob(blastProcessName, newBlastTask.getObjectId());
            blastJobId = newBlastTask.getObjectId().toString();
        }
        catch (Throwable e) {
            logger.error("Error returned from Job execution:" + e.getMessage(), e);
            //e.printStackTrace(System.out);  
            if (newBlastTask != null) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.length() > 200) {
                    errorMessage = errorMessage.substring(0, 200);
                }
                Event errEvent = new Event("Failed to execute job" + (errorMessage == null ? "" : ": " + errorMessage),
                        new Date(),
                        Event.ERROR_EVENT);
                newBlastTask.addEvent(errEvent);
                try {
                    taskDAO.saveOrUpdateTask(newBlastTask);
                }
                catch (Throwable te) {
                    logger.error("Error updating task failure event:" + te.getMessage(), e);
                    throw new SystemException("Error updating task failure event.");
                }
            }
        }

        return blastJobId;
    }

    public BlastableNodeVO[] getPublicDatabaseList(String sequenceType, String requestingUser) throws SystemException {
        try {
            return nodeDAO.getBlastableDatabaseList(sequenceType, requestingUser);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public BlastableNodeVO getPublicDatabaseByNodeId(String requestingUser, String nodeId) throws SystemException {
        try {
            return nodeDAO.getBlastableDatabaseByNodeId(requestingUser, nodeId);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }


}
