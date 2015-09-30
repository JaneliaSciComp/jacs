
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
//import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
//import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
//import org.janelia.it.jacs.model.tasks.rnaSeq.RnaSeqPipelineTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.utils.BlastJobInfoGenerator;
import org.janelia.it.jacs.server.access.hibernate.utils.BlastJobInfoGeneratorFactory;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.shared.tasks.RnaSeqJobInfo;
import org.janelia.it.jacs.shared.tasks.SearchJobInfo;
//import org.janelia.it.jacs.web.gwt.common.server.RecruitmentTaskToInfoTranslator;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigInteger;
import java.util.*;

/**
 * Task: Lfoster
 * Date: Aug 10, 2006
 * Time: 1:37:31 PM
 * Implementation of data task data access object, using hibernate criteria for query.
 */
public class TaskDAOImpl extends DaoBaseImpl implements TaskDAO {
    private static Logger _logger = Logger.getLogger(TaskDAOImpl.class);

    // DAO's can only come from Spring's Hibernate
    private TaskDAOImpl() {
    }

    public Task getTaskById(Long taskId)
            throws DataAccessException, DaoException {
        try {
            return (Task) getHibernateTemplate().get(Task.class, taskId);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
    }

    public Task getTaskWithMessages(Long taskId) throws DataAccessException, DaoException {
        return (Task) findByNamedQueryAndNamedParam("findTaskWithMessages", "taskId", taskId, true);  // separate line for debugging
    }

    public void purgeTask(Long taskId)
            throws DataAccessException, DaoException {
        try {
            Task toPurge = (Task) getHibernateTemplate().get(Task.class, taskId);
            if (toPurge != null) {
                _logger.debug("Purge task entry: " + toPurge.getObjectId());
                getHibernateTemplate().delete(toPurge);
            }
        }
        catch (HibernateException e) {
            _logger.error("Delete task error", e);
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            _logger.error("Delete task - data access error", e);
            throw handleException(e, "TaskDAOImpl - purgeTask");
        }
        catch (IllegalStateException e) {
            _logger.error("Delete task - illegal state", e);
            throw handleException(e, "TaskDAOImpl - purgeTask");
        }
        catch (Exception e) {
            _logger.error("Delete task - unexpected error", e);
            throw new DaoException(e, "Delete task");
        }
    }

    public List<BlastJobInfo> getPagedBlastJobsForUserLogin(String classname,
                                                            String userLogin,
                                                            String likeString,
                                                            int startIndex,
                                                            int numRows,
                                                            SortArgument[] sortArgs) throws DaoException {

        try {
            BlastJobInfoGenerator jobInfoGenerator = BlastJobInfoGeneratorFactory.getInstance(this, classname);
            List<Task> tmpResults = getPagedTasksForUserLoginByClass(userLogin, likeString, classname, startIndex,
                    numRows, sortArgs, true);
            return jobInfoGenerator.getBlastJobInfo(tmpResults);
        }
        catch (Exception e) {
            _logger.error("Error in getPagedTasksForUserLoginByClass", e);
            DaoException daoExc = new DaoException("TaskDAOImpl.getPagedTasksForUserLoginByClass");
            daoExc.initCause(e);
            throw daoExc;
        }
    }

//    public List<RnaSeqJobInfo> getPagedRnaSeqJobsForUserLogin(String classname,
//                                                              String userLogin,
//                                                              String likeString,
//                                                              int startIndex,
//                                                              int numRows,
//                                                              SortArgument[] sortArgs) throws DaoException {
//        try {
//            List<RnaSeqJobInfo> rnaSeqJobInfoList = new ArrayList<RnaSeqJobInfo>();
//            List<Task> tmpResults = getPagedTasksForUserLoginByClass(userLogin, likeString, classname, startIndex,
//                    numRows, sortArgs, true);
//            for (Task task : tmpResults) {
//                RnaSeqJobInfo jobInfo = new RnaSeqJobInfo();
//                RnaSeqPipelineTask rnaSeqPipelineTask = (RnaSeqPipelineTask) task;
//                List<Event> eventList = rnaSeqPipelineTask.getEvents();
//                FileNode resultFileNode = getFileNodeForTask(rnaSeqPipelineTask);
//                jobInfo.setJobId(task.getObjectId().toString());
//                jobInfo.setJobname(task.getJobName());
//                if (resultFileNode != null) {
//                    jobInfo.setJobResultsDirectoryPath(resultFileNode.getDirectoryPath());
//                }
//                Map<String, String> paramMap = new HashMap<String, String>();
//                for (String key : task.getParameterKeySet()) {
//                    String parameter = task.getParameter(key);
//                    paramMap.put(key, parameter);
//                }
//                jobInfo.setParamMap(paramMap);
//                jobInfo.setUsername(task.getOwner());
//                if (eventList.size() > 0) {
//                    Date submittedDate = eventList.get(0).getTimestamp();
//                    jobInfo.setSubmitted(submittedDate);
//                    jobInfo.setStatus(eventList.get(eventList.size() - 1).getEventType());
//                }
//                rnaSeqJobInfoList.add(jobInfo);
//            }
//            return rnaSeqJobInfoList;
//        }
//        catch (Exception e) {
//            _logger.error("Error in getPagedTasksForUserLoginByClass", e);
//            DaoException daoExc = new DaoException("TaskDAOImpl.getPagedTasksForUserLoginByClass");
//            daoExc.initCause(e);
//            throw daoExc;
//        }
//    }
//
    public List<SearchJobInfo> getPagedSearchInfoForUserLogin(String userLogin,
                                                              int startIndex,
                                                              int numRows,
                                                              SortArgument[] sortArgs)
            throws DaoException {
        List<SearchJobInfo> infoList = new ArrayList<SearchJobInfo>();
        try {
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals(JobInfo.SORT_BY_JOB_ID)) {
                        dataSortField = "taskId";
                    }
                    else if (dataSortField.equals(JobInfo.SORT_BY_JOB_NAME)) {
                        dataSortField = "jobName";
                    }
                    else if (dataSortField.equals(JobInfo.SORT_BY_STATUS)) {
                        dataSortField = "{last_event.eventType}";
                    }
                    else if (dataSortField.equals(JobInfo.SORT_BY_SUBMITTED)) {
                        dataSortField = "{first_event.timestamp}";
                    }
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_NUM_HITS)) {
                        dataSortField = "nhits";
                    }
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_QUERY_SEQ)) {
                        dataSortField = "query";
                    }
                    else {
                        // unknown or unsupported sort field -> therefore set it to null
                        dataSortField = null;
                    }
                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" desc");
                        }
                    }
                } // end for all sortArgs
            }
            String orderByClause;
            if (orderByFieldsBuffer.length() == 0) {
                orderByClause = "order by task.task_id desc ";
            }
            else {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            SQLQuery sqlQuery;
            {
                Query query = getSession().getNamedQuery("searchInfoSqlQuery");
                String sql = query.getQueryString() + " " + orderByClause;
                sqlQuery = getSession().createSQLQuery(sql).setResultSetMapping("searchJobInfoMapping");
                sqlQuery.setString("userLogin", userLogin);
                _logger.debug("sql=" + sqlQuery.getQueryString());
            }
            if (startIndex >= 0) {
                sqlQuery.setFirstResult(startIndex);
            }
            if (numRows > 0) {
                sqlQuery.setMaxResults(numRows);
            }
            List<Object[]> results = sqlQuery.list();
            if (results == null) {
                _logger.debug("results are null");
            }
            else {
                _logger.debug("result list size=" + results.size());
            }
            for (Object[] result : results) {
                Long jobId = (Long) result[0];
                String jobName = (String) result[1];
                String query = (String) result[2];
                String topics = (String) result[3];
                Event firstEvent = (Event) result[4];
                Event lastEvent = (Event) result[5];

                SearchJobInfo searchInfo = new SearchJobInfo();
                searchInfo.setJobId(jobId.toString());
                searchInfo.setJobname(jobName);
                searchInfo.setQueryName(query);
                searchInfo.setCategories(topics.split(","));
                searchInfo.setUsername(userLogin);
                // Event-based attributes
                searchInfo.setSubmitted(new Date(firstEvent.getTimestamp().getTime()));
                searchInfo.setStatusDescription(lastEvent.getDescription());
                searchInfo.setStatus(lastEvent.getEventType());
                infoList.add(searchInfo);
            }
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "TaskDAOImpl - findSearchJobsForUserLoginUsingSQL");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "TaskDAOImpl - findSearchJobsForUserLoginUsingSQL");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (Exception e) {
            _logger.error("Unexpected exception", e);
            throw handleException(e, "TaskDAOImpl - unexpected exception - findSearchJobsForUserLoginUsingSQL");
        }
        _logger.debug("returning search infoList size=" + infoList.size());
        return infoList;
    }

    public JobInfo getJobStatusByJobId(Long taskId) throws Exception {
        Task task = (Task) getSession().get(Task.class, taskId);
        if (task != null) {
            if (task instanceof BlastTask) {
                BlastJobInfoGenerator jobInfoGenerator =
                        BlastJobInfoGeneratorFactory.getInstance(this, "BlastTask");
                return jobInfoGenerator.getBlastJobInfo(task);
            }
//            else if (task instanceof ReversePsiBlastTask) {
//                BlastJobInfoGenerator jobInfoGenerator =
//                        BlastJobInfoGeneratorFactory.getInstance(this, "ReversePsiBlastTask");
//                return jobInfoGenerator.getBlastJobInfo(task);
//            }
//            else if (task instanceof RecruitmentViewerFilterDataTask) {
//                return RecruitmentTaskToInfoTranslator.getInfoForRecruitmentResultTask(
//                        (RecruitmentViewerFilterDataTask) task);
//            }
            else /* ExportTask */ {
                return createJobInfo(task);
            }
        }
        return null;
    }

    private JobInfo createJobInfo(Task task) throws DaoException {
        logger.info("Starting createJobInfo for taskId=" + task.getObjectId());

        JobInfo info = new JobInfo();
        // Get the default information
        info.setJobId(String.valueOf(task.getObjectId()));
        info.setUsername(task.getOwner());
        if (task.getMessages() == null) {
            info.setNumTaskMessages(0);
        }
        else {
            info.setNumTaskMessages(task.getMessages().size());
        }
        // Event-based attributes
        logger.info("Getting event info");
        if (task.getFirstEvent() != null)
            info.setSubmitted(new Date(task.getFirstEvent().getTimestamp().getTime()));
        Event lastEvent = task.getLastEvent();
        if (lastEvent != null) {
            info.setStatusDescription(lastEvent.getDescription());
            info.setStatus(lastEvent.getEventType());
        }

        // Parameters
        HashMap<String, String> paramMap = new HashMap<String, String>();
        for (String key : task.getParameterKeySet())
            paramMap.put(key, task.getParameter(key));
        info.setParamMap(paramMap);
        info.setJobname(task.getTaskName());
        return info;
    }

    public Task saveOrUpdateTask(Task targetTask)
            throws DataAccessException, DaoException {
        boolean success = validateTask(targetTask);
        if (!success) {
            throw new IllegalArgumentException("This object is not constructed properly for submission to the db.");
        }
        saveOrUpdateObject(targetTask, "TaskDAOImpl - saveOrUpdateTask");
        return targetTask;
    }

    /**
     * This method checks to see if the task corresponding to taskId has completed
     * It's needed in DAO because Task's events are lazy initialized
     *
     * @param taskId taskId to look up the status for
     * @return boolean if task is completed
     * @throws DataAccessException
     * @throws DaoException
     */
    public boolean isDone(Long taskId)
            throws DataAccessException, DaoException {
        // Task must exist in database
        Task task = (Task) getHibernateTemplate().load(Task.class, taskId);
        return task.isDone();
    }

    public List<Task> findTasksByIdRange(Long startId, Long endId)
            throws DataAccessException, DaoException {
        String[] p = new String[2];
        p[0] = "startId";
        p[1] = "endId";
        Long[] v = new Long[2];
        v[0] = startId;
        v[1] = endId;
        return (List<Task>) getHibernateTemplate().findByNamedQueryAndNamedParam("findTasksByIdRange", p, v);
    }

    public List<Task> findOrderedTasksByIdRange(Long startId, Long endId)
            throws DataAccessException, DaoException {
        String[] p = new String[2];
        p[0] = "startId";
        p[1] = "endId";
        Long[] v = new Long[2];
        v[0] = startId;
        v[1] = endId;
        return (List<Task>) getHibernateTemplate().findByNamedQueryAndNamedParam("findOrderedTasksByIdRange", p, v);
    }

    /**
     * @param cl      - specific class of task to be returned
     * @param startId - beginning of IDs
     * @param endId   - end of IDs
     * @return - list of objects which type was specified by cl
     * @throws DataAccessException
     * @throws DaoException        This method returns all tasks - deleted and not deleted
     */
    public <T extends Task> List<T> findSpecificTasksByIdRange(Class<T> cl, Long startId, Long endId, boolean includeSystem)
            throws DataAccessException, DaoException {

        Object[] v = new Object[2];
        v[0] = startId;
        v[1] = endId;
        String query = "select task from " + cl.getSimpleName() + " task " +
                " where task.id > ? and task.id < ? ";
        if (!includeSystem) {
            query += " and task.owner != 'system' ";
        }

        query += " order by task.id desc";

        return (List<T>) getHibernateTemplate().find(query, v);
    }

    /**
     * This method has been worked on by many people and probably attempts to do wayyyyyy too much.
     * If there are multiple bugs related to it, the method should probably be broken down into specific db calls
     * to eliminate all the confusion.
     *
     * @param userLogin           - owner of the tasks we're looking for
     * @param likeString          - string to match the query name we're interested in (FRV only?)
     * @param targetTaskClassName - class of tasks to look for
     * @param startIndex          pagination start
     * @param numRows             number of records we want
     * @param sortArgs            array of sort arguments (complicated "advanced sort")
     * @return list of tasks which match the criteria
     * @throws DataAccessException problem accessing the data
     * @throws DaoException        problem with the query for the data
     */
    public List<Task> getPagedTasksForUserLoginByClass(String userLogin,
                                                       String likeString,
                                                       String targetTaskClassName,
                                                       int startIndex,
                                                       int numRows,
                                                       SortArgument[] sortArgs,
                                                       boolean parentTasksOnly)
            throws DataAccessException, DaoException {
        List<Task> taskList;
        try {
            boolean isMultiSelect = false;
            String tasksHQL;
            String selectHQL = "select t ";
            String fromHQL = "from " + targetTaskClassName + " t ";
            String whereHQL = "where t.taskDeleted=false and t.expirationDate=null ";
            String likeHQL = "";

            // If user is not null only grab theirs
            if (null != userLogin && !"".equals(userLogin)) {
                whereHQL += "and t.owner='" + userLogin + "' ";
            }

            // Filter if we're only looking for pipeline (parent) runs.  For example, should we ignore child blast runs.
            if (parentTasksOnly) {
                whereHQL += "and t.parentTaskId=null ";
            }

            // do a case-insensitive starts-with "like" search on the query name if a value was specified
            if (likeString != null && likeString.length() > 0) {
                fromHQL += "left join t.taskParameterSet query with query.name = 'query' ";
                likeHQL = "and lower(query.value) like lower('" + likeString + "') ";
            }
            String orderByHQL = "";

            // Figure out the sorting
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    // in creating the sort field we will also have to take a look at the targetTaskClassName
                    // so that we would not use fields that are not defined in that class
                    // Basic Task sorting
                    else if (dataSortField.equals(JobInfo.SORT_BY_JOB_NAME)) {
                        // if this is not a blast job and the sort arg is by job name don't use it
                        dataSortField = "t.jobName";
                    }
                    else if (dataSortField.equals(JobInfo.SORT_BY_JOB_ID) || dataSortField.equals(JobInfo.SORT_BY_SUBMITTED)) {
                        dataSortField = "t.objectId";
                    }
                    // Blast Job Sorting
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_PROGRAM)) {
                        dataSortField = "t.taskName";
                    }
                    // NOTE: This sort works but often gets MASKED by the front-end; thus, it can appear to be sorted incorrectly.
                    else if (dataSortField.equals(JobInfo.SORT_BY_STATUS)) {
                        fromHQL += "join t.events lastEvent ";
                        whereHQL += "and lastEvent.eventIndex = (select max(eventIndex) from t.events) ";
                        dataSortField = "lastEvent.eventType";
                    }
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_QUERY_SEQ) ||
                            dataSortField.equals(RecruitableJobInfo.QUERY_SORT)) {
                        // It could be that anything which hits this area already has non-null a likeString but I'm not
                        // taking any chances.
                        if (!fromHQL.contains("left join t.taskParameterSet query with query.name = 'query' ")) {
                            fromHQL += "left join t.taskParameterSet query with query.name = 'query' ";
                        }
                        dataSortField = "query.value";
                    }
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_SUBJECT_DB)) {
                        selectHQL = "select t, blastDB.name ";
                        isMultiSelect = true;
                        fromHQL += ", BlastDatabaseFileNode blastDB ";
                        fromHQL += "left join t.taskParameterSet subject with subject.name = 'subject databases' ";
                        whereHQL += "and cast(blastDB.objectId as string)=subject.value ";
                        dataSortField = "blastDB.name";
                    }
                    // Recruitment Job Sorting
                    else if (dataSortField.equals(RecruitableJobInfo.HITS_SORT)) {
                        fromHQL += "left join t.taskParameterSet numHits with numHits.name = 'numHits' ";
                        dataSortField = "cast(numHits.value as integer)";
                    }
                    else if (dataSortField.equals(RecruitableJobInfo.SORT_BY_GENOME_LENGTH) ||
                            dataSortField.equals(RecruitableJobInfo.LENGTH_SORT)) {
                        fromHQL += "left join t.taskParameterSet refEnd with refEnd.name = 'refEnd' ";
                        dataSortField = "cast(refEnd.value as float)";
                    }
                    else if (dataSortField.equals(BlastJobInfo.SORT_BY_NUM_HITS)) {
                        selectHQL = "select t, resultNode ";
                        isMultiSelect = true;
                        fromHQL += ", BlastResultFileNode resultNode ";
                        whereHQL += "and resultNode is not null and resultNode.task = t ";
                        dataSortField = "resultNode.blastHitCount";
                    }

                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" desc");
                        }
                    }
                } // end for all sortArgs
            }
            if (orderByFieldsBuffer.length() > 0) {
                orderByHQL = "order by " + orderByFieldsBuffer.toString();
            }

            // Build the query
            tasksHQL = selectHQL + fromHQL + whereHQL + likeHQL + orderByHQL;

            // Now run the final query
            _logger.debug("Tasks HQL: " + tasksHQL);
            Query tasksQuery = getSession().createQuery(tasksHQL);
            if (startIndex > 0) {
                tasksQuery.setFirstResult(startIndex);
            }
            if (numRows > 0) {
                tasksQuery.setMaxResults(numRows);
            }
            List results = tasksQuery.list();
            taskList = new ArrayList<Task>();
            for (Object result : results) {
                Task t;
                if (isMultiSelect) {
                    Object[] resEntry = (Object[]) result;
                    t = (Task) resEntry[0];
                }
                else {
                    t = (Task) result;
                }
                taskList.add(t);
            }
        }
        catch (Throwable e) {
            _logger.error("Error in getPagedTasksForUserLoginByClass", e);
            DaoException daoExc = new DaoException("TaskDAOImpl.getPagedTasksForUserLoginByClass");
            daoExc.initCause(e);
            throw daoExc;
        }
        return taskList;
    }

    public Integer getNumTasksForUserLoginByClass(String userLogin,
                                                  String likeString,
                                                  String taskClassName,
                                                  boolean parentTasksOnly)
            throws DataAccessException, DaoException {
        _logger.debug("getNumTasksForUserLoginByClass start : user=" + userLogin + " class=" + taskClassName);
        try {
            String selectHQL = "select count(t.objectId) ";
            String fromHQL = "from " + taskClassName + " t ";
            String whereHQL = "where t.taskDeleted=false and t.expirationDate=null ";
            String likeHQL = "";

            // If user is not null only grab theirs
            if (null != userLogin && !"".equals(userLogin)) {
                whereHQL += "and t.owner='" + userLogin + "' ";
            }

            // Filter if we're only looking for pipeline (parent) runs.  For example, should we ignore child blast runs.
            if (parentTasksOnly) {
                whereHQL += "and t.parentTaskId=null ";
            }

            // do a case-insensitive starts-with "like" search on the query name if a value was specified
            if (likeString != null && likeString.length() > 0) {
                fromHQL += "left join t.taskParameterSet query with query.name = 'query' ";
                likeHQL = "and lower(query.value) like lower('" + likeString + "') ";
            }

            // Build the query
            String tasksHQL = selectHQL + fromHQL + whereHQL + likeHQL;

            _logger.debug("Tasks HQL: " + tasksHQL);
            Query tasksQuery = getSession().createQuery(tasksHQL);
            List results = tasksQuery.list();
            // Only takes the task returned
            if (results != null && results.size() > 0) {
                int numberOfResults = ((Long) results.get(0)).intValue();
                logger.debug("getNumTasksForUserLoginByClass - returning " + numberOfResults);
                return numberOfResults;
            }
            else {
                if (results == null) {
                    logger.debug("getNumTasksForUserLoginByClass - results returned null");
                }
                else {
                    logger.debug("getNumTasksForUserLoginByClass - returning zero");
                }
                return null;
            }
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "TaskDAOImpl - getNumTasksForUserLoginByClass");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "TaskDAOImpl - getNumDataTasksForUserByClass");
        }
    }

    /**
     * Method to prime the search oracles for the FRV query panels
     *
     * @param userLogin person who owns the tasks in question
     * @return a list of the ordered query names
     * @throws DataAccessException - problem accessing the data
     * @throws DaoException        - problem with the query for data
     */
    public List<String> getTaskQueryNamesForUser(String userLogin)
            throws DataAccessException, DaoException {
        List<String> names;
        try {
            String tasksHQL = "select query.value from RecruitmentViewerFilterDataTask t " +
                    "left join t.taskParameterSet query with query.name = 'query' " +
                    "where t.owner='" + userLogin + "' and t.taskDeleted=false and t.expirationDate=null order by query.value";
            _logger.debug("Tasks HQL: " + tasksHQL);
            Query tasksQuery = getSession().createQuery(tasksHQL);
            List results = tasksQuery.list();
            names = new ArrayList<String>();
            for (Object result : results) {
                names.add((String) result);
            }
        }
        catch (Throwable e) {
            _logger.error("Error in getTaskQueryNamesForUser", e);
            DaoException daoExc = new DaoException("TaskDAOImpl.getTaskQueryNamesForUser");
            daoExc.initCause(e);
            throw daoExc;
        }
        return names;
    }

    public void setTaskExpirationAndName(Long taskId, Date expirationDate, String jobName) throws DaoException {
        Task tmpTask = getTaskById(taskId);
        tmpTask.setExpirationDate(expirationDate);
        tmpTask.setJobName(jobName);
        saveOrUpdateTask(tmpTask);
    }

    public Task getRecruitmentFilterTaskByUserPipelineId(Long pipelineId) throws DaoException {
        try {
            SQLQuery query = getSession().createSQLQuery("select task_id from task where parent_task_id=" + pipelineId
                    + " and subclass='recruitmentViewerFilterDataTask'");
            List results = query.list();
            // Should have one hit
            BigInteger tmpResult = (BigInteger) results.get(0);
            return (Task) getHibernateTemplate().get(Task.class, tmpResult.longValue());
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
    }

    public List<String> getTaskQueryNamesByClassAndUser(String userLogin, String className) throws DaoException {
        List<String> names;
        try {
            String tasksHQL = "select query.value from " + className + " t " +
                    "left join t.taskParameterSet query with query.name = 'query' " +
                    "where t.owner='" + userLogin + "' and t.taskDeleted=false and t.expirationDate=null order by query.value";
            _logger.debug("Tasks HQL: " + tasksHQL);
            Query tasksQuery = getSession().createQuery(tasksHQL);
            List results = tasksQuery.list();
            names = new ArrayList<String>();
            for (Object result : results) {
                names.add((String) result);
            }
        }
        catch (Throwable e) {
            _logger.error("Error in getTaskNamesByClassAndUser", e);
            DaoException daoExc = new DaoException("TaskDAOImpl.getTaskNamesByClassAndUser");
            daoExc.initCause(e);
            throw daoExc;
        }
        return names;
    }

    /**
     * Retrieves and caches the node ids and their name
     * Right now it keeps appending to the _nodesCache cache so that mapping may grow
     * too much
     *
     * @param subjIds    subject node id list
     * @param nodesCache cache of nodes
     */
    public void getNodeNames(List<String> subjIds, Map<String, String> nodesCache) {
        // create the query for retrieving the missing subject descriptions
        HashSet<Long> missingNodes = new HashSet<Long>();
        for (String subjId : subjIds) {
            if (nodesCache.get(subjId) == null) {
                try {
                    missingNodes.add(Long.valueOf(subjId));
                }
                catch (NumberFormatException nfe) {
                    _logger.warn("Can't determine node name for subject node id [" + subjId + "] - skipping this entry");
                }

            }
        }
        if (missingNodes.size() > 0) {
            String hql = "select n.objectId,n.name " +
                    "from Node n " +
                    "where n.objectId in (:nIds)";
            Query query = getSession().createQuery(hql);
            query.setParameterList("nIds", missingNodes);
            List results = query.list();
            for (Object result : results) {
                Object[] resultEntry = (Object[]) result;
                nodesCache.put(resultEntry[0].toString(), (String) resultEntry[1]);
            }
        }
    }

    public List<BaseSequenceEntity> getBlastResultNodeBaseSequenceEntities(BlastResultNode resultNode) {
        Query query = getSession().createFilter(resultNode.getBlastHitResultSet(), "select this.subjectEntity");
        logger.info("Calling query to get bse list");
        return query.setMaxResults(1).list();
    }

    private boolean validateTask(Task targetTask) {
        if (null == targetTask) {
            return false;
        }
        if (null == targetTask.getOwner() ||
                null == targetTask.getTaskName() || null == targetTask.getParameterKeySet() ||
                null == targetTask.getInputNodes() || null == targetTask.getEvents()) {
            _logger.error("Task problems, cannot persist: " + targetTask.toString());
            return false;
        }
        return true;
    }

    private FileNode getFileNodeForTask(Task task) {
        DetachedCriteria nodeCriteria = DetachedCriteria.forClass(FileNode.class);
        nodeCriteria.add(Expression.eq("task", task));
        List nodes = getHibernateTemplate().findByCriteria(nodeCriteria);

        if (nodes == null || nodes.size() < 1) return null;

        FileNode node = (FileNode) nodes.iterator().next();
        return node;
    }

}
