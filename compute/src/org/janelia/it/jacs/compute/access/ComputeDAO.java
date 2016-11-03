package org.janelia.it.jacs.compute.access;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates all DB access operations.  It wraps RuntimeExceptions with checked DaoException
 * to get container to throw it to the client and let client make the decision on whether or not
 * it wants to rollback the transaction
 *
 * @author Sean Murphy
 */
public class ComputeDAO extends AbstractBaseDAO {
    private static Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    private TaskDAO taskDao;

    public ComputeDAO(EntityManager entityManager, TaskDAO taskDao) {
        super(entityManager);
        this.taskDao = taskDao;
    }


    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state) throws DaoException {
        LOG.trace("bulkAddGridJobStatus(taskId=" + taskId + ", queue=" + queue + ", jobIds=" + jobIds.size() + ", state=" + state + ")");

        LOG.debug("bulkAddGridJobStatus - Setting " + jobIds.size() + " jobs to status " + state.name() + " for task " + taskId);
        for (String jobId : jobIds) {
            GridJobStatus s = new GridJobStatus(taskId, jobId, queue, state);
            checkAndRecordError(s);
            save(s);
        }
        LOG.debug("bulkAddGridJobStatus - completed");
    }

    /**
     * Method to record a task error, when appropriate
     * @param status - object which has the SGE execution script info
     * @throws DaoException thrown when there is a problem recording a task error
     */
    private void checkAndRecordError(GridJobStatus status) throws DaoException {
        if (null == status) {
            LOG.warn("Calling checkandRecordError but grid status object is null");
            return;
        }

        // If an exit code exists and is non-zero record an error.
        if (null!=status.getExitStatus() && 0!=status.getExitStatus()){
            taskDao.updateTaskStatus(status.getTaskID(), Event.ERROR_EVENT, "Error - GridJob exit status is "+status.getExitStatus());
        }
    }

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates) throws DaoException {
        LOG.trace("bulkUpdateGridJobStatus(taskId="+taskId+", jobStates.size="+jobStates.size()+")");

        LOG.debug("Starting db update of task("+taskId+") with "+jobStates.size()+" job items");
        for (String jobId : jobStates.keySet()) {
            updateJobStatus(taskId, jobId, jobStates.get(jobId));
        }
        LOG.debug("Done updating the job status for task "+taskId);
    }

    public void saveOrUpdateGridJobStatus(GridJobStatus gridJobStatus) throws DaoException {
        LOG.trace("saveOrUpdateGridJobStatus(gridJobStatus="+gridJobStatus+")");

        save(gridJobStatus);
    }

    public void cleanUpGridJobStatus(long taskId) throws DaoException {
        LOG.trace("cleanUpGridJobStatus(taskId="+taskId+")");

        String hql = "update accounting set status = ? where task_id = ? and status not in (?, ?) ";

        Query query = getCurrentSession().createSQLQuery();
        query.setString(0, GridJobStatus.JobState.ERROR.name());
        query.setLong(1, taskId);
        query.setString(2, GridJobStatus.JobState.FAILED.name());
        query.setString(3, GridJobStatus.JobState.DONE.name());


        query.executeUpdate();
    }

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateJobStatus(taskId="+taskId+", jobId="+jobId+", state="+state+")");    
        }
        
        GridJobStatus tmpStatus = getGridJobStatus(taskId, jobId);
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            saveOrUpdate(tmpStatus);
        }
        else {
            log.error("GridJobStatus for task_id:" + taskId + " and job_id:" + jobId + " NOT FOUND");
        }
    }

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateJobStatus(taskId="+taskId+", jobId="+jobId+", state="+state+", infoMap.size="+infoMap.size()+")");    
        }
        
        GridJobStatus tmpStatus = getGridJobStatus(taskId, jobId);
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            tmpStatus.updateFromMap(infoMap);
            checkAndRecordError(tmpStatus);
            saveOrUpdate(tmpStatus);
        }
        else {
            log.error("GridJobStatus for task_id:" + taskId + " and job_id:" + jobId + " NOT FOUND");
        }
    }

    public GridJobStatus getGridJobStatus(long taskId, String jobId) {
        if (log.isTraceEnabled()) {
            log.trace("getGridJobStatus(taskId="+taskId+", jobId="+jobId+")");    
        }
        
        try {
            //   String sqlQuery = "select * from accounting where task_id=" + taskId + ";";
            //   SQLQuery query = getCurrentSession().createSQLQuery(sqlQuery);
            Query query = getCurrentSession().createQuery("from GridJobStatus a where a.taskID = ? and a.jobID = ?");
            query.setLong(0, taskId);
            query.setString(1, jobId);
            List result = query.list();
            if (result != null) {
                switch (result.size()) {
                    case 0:
                        return null;
                    case 1:
                        return (GridJobStatus) result.get(0);
                    default:
                        log.error("getGridJobStatus found more then one entiry for task '" + taskId + "' and job '" + jobId + "'. Only first one will be used");
                        return (GridJobStatus) result.get(0);
                }
            }
        }
        catch (Exception e) {
            log.error("Unable to retrieve Jobs for task " + taskId + " due to exception " + e.toString());
        }
        return null;
    }

    public List<Long> getActiveTasks() {
        if (log.isTraceEnabled()) {
            log.trace("getActiveTasks()");    
        }
        
        LinkedList<Long> tasks = new LinkedList<Long>();
        String sql = "select distinct(task_id) from accounting where status in ('" + GridJobStatus.JobState.QUEUED.name() + "', '" + GridJobStatus.JobState.RUNNING.name() + "' )";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            log.debug("No active tasks found - SQL: '" + sql + "'");
            return tasks; // empty list
        }
        for (BigInteger returnValue : returnList) {
            tasks.add(returnValue.longValue());
        }
        return tasks;
    }

    public List<Long> getWaitingTasks() {
        if (log.isTraceEnabled()) {
            log.trace("getWaitingTasks()");    
        }
        
        //_logger.debug("Getting the list of waiting tasks in the SGE queue.");
        LinkedList<Long> tasks = new LinkedList<Long>();
        String sql = "select distinct(task_id) from accounting where status in ('" + GridJobStatus.JobState.QUEUED.name() + "')";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            log.debug("No waiting tasks found - SQL: '" + sql + "'");
            return tasks; // empty list
        }
        for (BigInteger returnValue : returnList) {
            tasks.add(returnValue.longValue());
        }
        //_logger.debug("Number of waiting tasks are : " + tasks.size());
        return tasks;
    }


    public List<GridJobStatus> getGridJobStatusesByTaskId(long taskId, String[] states) {
        if (log.isTraceEnabled()) {
            log.trace("getGridJobStatusesByTaskId(taskId="+taskId+", states.length="+states.length+")");    
        }
        
        try {
            //   String sqlQuery = "select * from accounting where task_id=" + taskId + ";";
            //   SQLQuery query = getCurrentSession().createSQLQuery(sqlQuery);

            Query query;
            StringBuilder hql = new StringBuilder();
            hql.append("from GridJobStatus a where a.taskID=").append(taskId);
            if (states != null && states.length > 0) {
                hql.append(" and a.status in (");
                for (String state : states) {
                    hql.append("?,");
                }
                hql.deleteCharAt(hql.length() - 1); // remove extra comma
                hql.append(")");
                query = getCurrentSession().createQuery(hql.toString());
                for (int i = 0; i < states.length; i++) {
                    query.setString(i, states[i]);
                }
            }
            else {
                query = getCurrentSession().createQuery(hql.toString());
            }
            return query.list();
        }
        catch (Exception e) {
            log.error("Unable to retrieve Jobs for task " + taskId + " due to exception ", e);
        }
        return null;
    }

    public List getQueuedJobs() {
        if (log.isTraceEnabled()) {
            log.trace("getQueuedJobs()");    
        }
        
        Query query = getCurrentSession().createQuery("from GridJobStatus a where a.status<>'DONE' order by a.taskID");
        return query.list();
    }

    public List<Node> getNodeByName(String nodeName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getNodeByName(nodeName="+nodeName+")");    
        }
        
        try {
            String hql = "select n from Node n where n.name='" + nodeName + "'";
            //if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql);
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getNodeByName");
        }
    }

    public List<Node> getNodeByPathOverride(String pathOverride) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getNodeByPathOverride(pathOverride="+pathOverride+")");    
        }
        
        try {
            String hql = "select n from Node n where n.pathOverride=?";
            Query query = getCurrentSession().createQuery(hql).setString(0, pathOverride);
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getNodeByPathOverride");
        }
    }
    
    /**
     * Method to expire system-owned recruitment and filter data tasks.  Expired system tasks will no longer show up
     * in the system lists, BUT user-saved data based on the old GBK file/Gi-number should still work.  This occurrance
     * should hopefully be rare.
     *
     * @param giNumber - number deleted or obsoleted
     */
    public void setSystemDataRelatedToGiNumberObsolete(String giNumber) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("setSystemDataRelatedToGiNumberObsolete(giNumber="+giNumber+")");    
        }
        
        try {
            StringBuilder sql = new StringBuilder("update task t set expiration_date=current_timestamp from task_parameter p where p.parameter_value='" +
                    giNumber + "' and p.parameter_name='giNumber' and t.task_id=p.task_id and t.task_owner='user:system'");
            if (log.isDebugEnabled()) log.debug("sql=" + sql);
            SQLQuery query = getCurrentSession().createSQLQuery(sql.toString());
            query.executeUpdate();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "setDataRelatedToGiNumberObsolete");
        }
    }

    public Integer getPercentCompleteForATask(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getPercentCompleteForATask(taskId="+taskId+")");    
        }

        List allJobs = getGridJobStatusesByTaskId(taskId, null);

        String[] states = {"DONE"};
        List completeJobs = getGridJobStatusesByTaskId(taskId, states);

        if (allJobs.size() != 0) {
            return Math.round((100 * completeJobs.size()) / allJobs.size());
        }
        else {
            return null;
        }

    }

    public List<? extends FileNode> getBlastDatabases(String nodeClassName) {
        if (log.isTraceEnabled()) {
            log.trace("getBlastDatabases(nodeClassName="+nodeClassName+")");    
        }
        
        String hql = "select clazz from Node clazz where subclass ='" + nodeClassName + "' and order by clazz.description";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<? extends FileNode> getBlastDatabasesOfAUser(String nodeClassName, String ownerKey) {
        if (log.isTraceEnabled()) {
            log.trace("getBlastDatabasesOfAUser(nodeClassName="+nodeClassName+", ownerKey="+ownerKey+")");    
        }
        
        String hql = "select clazz from Node clazz where subclass ='" + nodeClassName + "' and " +
                " clazz.owner='" + ownerKey + "' and (visibility = 'public' or visibility = 'private')" +
                " order by clazz.description";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<Task> getUserTasksByType(String simpleName, String ownerKey) {
        if (log.isTraceEnabled()) {
            log.trace("getUserTasksByType(simpleName="+simpleName+", ownerKey="+ownerKey+")");    
        }
        
        String hql = "select clazz from Task clazz where subclass='" + simpleName + "' and clazz.owner='" + ownerKey + "' order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public Task getRecruitmentFilterTaskByUserPipelineId(Long pipelineId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getRecruitmentFilterTaskByUserPipelineId(pipelineId="+pipelineId+")");    
        }
        
        try {
            SQLQuery query = getSession().createSQLQuery("select task_id from task where parent_task_id=" + pipelineId
                    + " and subclass='recruitmentViewerFilterDataTask'");
            List results = query.list();
            if (null == results || results.size() == 0) {
                return null;
            }

            // Technically, should have one hit - very old data may not have
            BigInteger tmpResult = (BigInteger) results.get(0);
            return sessionFactory.getCurrentSession().get(Task.class, tmpResult.longValue());
        }
        catch (Exception e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
    }


    public List<Task> getRecentUserParentTasksByOwner(String ownerKey) {
        if (log.isTraceEnabled()) {
            log.trace("getRecentUserParentTasksByOwner(ownerKey="+ownerKey+")");    
        }
        
    	Calendar minDate = Calendar.getInstance();
    	minDate.add(Calendar.DATE, -1);
        StringBuilder hql = new StringBuilder();
        hql.append("select distinct task from Task task ");
        hql.append("join task.events event "); 
        hql.append("where task.owner = :owner "); 
        hql.append("and task.taskDeleted=false ");
        hql.append("and task.parentTaskId is null ");
        hql.append("and event.timestamp >=  :minDate ");
        hql.append("order by task.objectId ");
        Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
        query.setString("owner", ownerKey);
        query.setTimestamp("minDate", minDate.getTime());
        return query.list();
    }

    public List<Task> getUserParentTasksByOwner(String ownerKey) {
        if (log.isTraceEnabled()) {
            log.trace("getUserParentTasksByOwner(ownerKey="+ownerKey+")");    
        }
        
        String hql = "select clazz from Task clazz where clazz.owner='" + ownerKey + "' and clazz.taskDeleted=false and clazz.parentTaskId is null order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<Task> getUserTasks(String ownerKey) {
        if (log.isTraceEnabled()) {
            log.trace("getUserTasks(ownerKey="+ownerKey+")");    
        }
        
        String hql = "select clazz from Task clazz where clazz.owner='" + ownerKey + "' and clazz.taskDeleted=false order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("setParentTaskId(parentTaskId="+parentTaskId+", childTaskId="+childTaskId+")");    
        }
        
        Query query = sessionFactory.getCurrentSession().createQuery("select clazz from Task clazz where clazz.objectId=" + childTaskId);
        List tmpList = query.list();
        if (null != tmpList && tmpList.size() == 1) {
            Task tmpTask = (Task) tmpList.iterator().next();
            tmpTask.setParentTaskId(parentTaskId);
            saveOrUpdate(tmpTask);
        }
    }

    public Long getSystemDatabaseIdByName(String databaseName) {
        if (log.isTraceEnabled()) {
            log.trace("getSystemDatabaseIdByName(databaseName="+databaseName+")");    
        }
        
        Query query = sessionFactory.getCurrentSession().createQuery("select clazz from Node clazz where clazz.name='" + databaseName + "' and clazz.owner='user:system'");
        List tmpList = query.list();
        List<Node> nodeList = new ArrayList<Node>();
        for (Object o : tmpList) {
            Node n = (Node) o;
            if (n.getVisibility().trim().toLowerCase().equals("public")) {
                nodeList.add(n);
            }
        }
        long mostRecentTimestamp = 0L;
        Node mostRecentNode = null;
        for (Node n : nodeList) {
            Date datestamp = TimebasedIdentifierGenerator.getTimestamp(n.getObjectId());
            long timestamp = datestamp.getTime();
            if (timestamp > mostRecentTimestamp) {
                mostRecentTimestamp = timestamp;
                mostRecentNode = n;
            }
        }
        if (mostRecentNode == null) {
            return null;
        }
        else {
            return mostRecentNode.getObjectId();
        }
    }

    public long getCumulativeCpuTime(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getCumulativeCpuTime(taskId="+taskId+")");    
        }
        
        //_logger.info("Getting cumulative cpu time for task id " + taskId);
        long cpuTime = getCpuTime(taskId);

        String sql = "select task_id from task where parent_task_id =" + taskId;
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            return cpuTime;
        }

        for (BigInteger childTaskId : returnList) {
            cpuTime = cpuTime + getCumulativeCpuTime(childTaskId.longValue());
        }

        log.info("Cumulative cpu time for task " + taskId + " is " + cpuTime);
        return cpuTime;
    }

    public long getCpuTime(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getCpuTime(taskId="+taskId+")");    
        }
        
        long cpuTime = 0;

        //_logger.info("Getting cpu time for task id " + taskId);
        String sql = "select sum(cpu_time) from accounting where task_id =" + taskId;
        Query query = getCurrentSession().createSQLQuery(sql);
        List result = query.list();
        if (result.size() > 0) {
            if (result.get(0) != null) {
                cpuTime = ((BigInteger) result.get(0)).longValue();
            }
        }
        //_logger.info("cpu time for task id " + taskId + " is " + cpuTime);
        return cpuTime;
    }
    
    public String getFastaEntry(String targetAcc) {
        if (log.isTraceEnabled()) {
            log.trace("getFastaEntry(targetAcc="+targetAcc+")");    
        }
        
        String sql = "select e.defline, s.sequence from sequence_entity e, bio_sequence s where e.accession='" + targetAcc + "' and s.sequence_id=e.sequence_id;";
        StringBuilder returnEntry = new StringBuilder();
        Query query = getCurrentSession().createSQLQuery(sql);
        List result = query.list();
        if (result.size() == 1) {
            Object[] results = (Object[]) result.get(0);
            String defline = (String) results[0];
            String sequence = (String) results[1];
            if (null == defline || null == sequence || "".equals(defline) || "".equals(sequence)) {
                System.out.println("Failed to find accession: " + targetAcc);
                return null;
            }
            else {
                returnEntry.append(">").append(defline).append("\n").append(sequence).append("\n");
            }
        }
        else {
            System.out.println("Failed to find accession: " + targetAcc);
            return null;
        }
        return returnEntry.toString();
    }

    public GenericServiceDefinitionNode getGenericServiceDefinitionByName(String serviceName) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("getGenericServiceDefinitionByName(serviceName="+serviceName+")");    
        }
        
        try {
            StringBuilder hql = new StringBuilder("select clazz from GenericServiceDefinitionNode clazz");
            hql.append(" where clazz.name='").append(serviceName).append("'");
            hql.append(" and clazz.visibility != '").append(Node.VISIBILITY_INACTIVE).append("'");
            if (log.isDebugEnabled()) log.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            if (query.list().size() > 0) {
                return (GenericServiceDefinitionNode) query.list().get(0);
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getGenericServiceDefinitionByName");
        }
    }

    
    public String createTempFile(String prefix, String content) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createTempFile(prefix="+prefix+", content="+content+")");    
        }
    	
    	try {
        	File file = File.createTempFile(prefix, null);
        	
        	FileWriter writer = new FileWriter(file);
        	writer.write(content);
        	writer.close();
        	
        	return file.getAbsolutePath();
    	}
    	catch (IOException e) {
    		throw new DaoException("Error creating temp file", e);
    	}
    }

    public UserToolEvent addEventToSession(UserToolEvent userToolEvent) throws DaoException {
//        if (log.isTraceEnabled()) {
//            log.trace("addEventToSession(userToolEvent="+userToolEvent+")");
//        }
        
        try {
            // If there is no session AND the event is the initial login then save to get the first GUID for the session
            if (null==userToolEvent.getSessionId() && UserToolEvent.TOOL_EVENT_LOGIN.equals(userToolEvent.getAction())) {
                getCurrentSession().saveOrUpdate(userToolEvent);
                userToolEvent.setSessionId(userToolEvent.getId());
            }
            getCurrentSession().saveOrUpdate(userToolEvent);
        }
        catch (HibernateException e) {
            throw new DaoException("Error recording tool event", e);
        }

        return userToolEvent;
    }
}