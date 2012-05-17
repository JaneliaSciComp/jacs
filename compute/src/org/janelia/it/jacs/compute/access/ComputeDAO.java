
package org.janelia.it.jacs.compute.access;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.genomics.BioSequence;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.Library;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatasetNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * This class encapsulates all DB access operations.  It wraps RuntimeExceptions with checked DaoException
 * to get container to throw it to the client and let client make the decision on whether or not
 * it wants to rollback the transaction
 *
 * @author Sean Murphy
 */

public class ComputeDAO extends ComputeBaseDAO {
    private static int MAX_MESSAGE_SIZE = 10000;

    public ComputeDAO(Logger logger) {
        super(logger);
    }

    public BlastDatabaseFileNode getBlastDatabaseFileNodeById(Long fileNodeId) {
        return (BlastDatabaseFileNode) getCurrentSession().get(BlastDatabaseFileNode.class, fileNodeId);
    }

    /**
     * It expects either a blast file node ID or an aggregate ID and returns an aggregate object.
     * If the object represented by the ID is a file node the result is encapsulated in an
     * aggregated node.
     *
     * @param nodeId         a BlastDatabaseFileNode or a BlastDatasetNode ID
     * @param fetchFileNodes - if true it eagerly retrieves all file nodes
     * @return an aggregated node
     */
    public BlastDatasetNode getBlastDatasetNodeById(Long nodeId, boolean fetchFileNodes) {
        Session session = sessionFactory.getCurrentSession();
        BlastDatasetNode blastDatasetNode = null;
        try {
            Node node = (Node) session.get(Node.class, nodeId);
            if (node instanceof BlastDatasetNode) {
                blastDatasetNode = (BlastDatasetNode) node;
                if (fetchFileNodes) {
                    // force a read of the aggregated file nodes
                    blastDatasetNode.getBlastDatabaseFileNodes().size();
                }
            }
            else if (node instanceof BlastDatabaseFileNode) {
                BlastDatabaseFileNode blastFileNode = (BlastDatabaseFileNode) node;
                blastDatasetNode = new BlastDatasetNode(blastFileNode.getOwner(),
                        blastFileNode.getTask(),
                        blastFileNode.getName(),
                        blastFileNode.getDescription(),
                        blastFileNode.getVisibility(),
                        null);
                blastDatasetNode.setObjectId(blastFileNode.getObjectId());
                blastDatasetNode.setSequenceType(blastFileNode.getSequenceType());
                blastDatasetNode.addBlastDatabaseFileNode(blastFileNode);
            }
        }
        catch (HibernateException e) {
            _logger.error("Error in method getBlastDatasetNodeById:\n" + e.getMessage(), e);
        }
        return blastDatasetNode;
    }

    public Read getReadByBseEntityId(Long bseEntityId) {
        Read r = null;
        Query query = sessionFactory.getCurrentSession().getNamedQuery("findReadByBseEntityId");
        query.setParameter("entityId", bseEntityId);
        List list = query.list();
        if (list.size() > 0)
            r = (Read) list.get(0);
        return r;
    }

    public Read getReadByAccession(String accession) {
        Read r = null;
        Query query = sessionFactory.getCurrentSession().getNamedQuery("findReadByAccesion"); // Accesion is sic
        query.setParameter("accesion", accession); // accesion is sic
        List list = query.list();
        if (list.size() > 0)
            r = (Read) list.get(0);
        if (r != null) {
            r.getBioSequence().getSequence();
            _logger.info("Sequence text for read " + r.getAccession() + " is non-null");
            Library l = r.getLibrary();
            if (l != null) {
                _logger.info("Retrieved read with accession " + accession + " and library id " + l.getLibraryId());
                Set samples = l.getSamples();
                if (samples != null) {
                    for (Object sample : samples) {
                        Sample s = (Sample) sample;
                        _logger.info("Retrieving sample acc " + s.getSampleAcc());
                        if (s.getBioMaterials() != null) {
                            Iterator siteIter = s.getBioMaterials().iterator();
                            if (siteIter.hasNext()) {
                                BioMaterial site = (BioMaterial) siteIter.next();
                                _logger.info("Retrieved BioMaterial " + site.getMaterialId());
                            }
                        }
                        else {
                            _logger.info("BioMaterial info for sample " + s.getSampleAcc() + " was null or empty");
                        }
                    }
                }
                else {
                    _logger.info("Sample set is null");
                }
            }
            else {
                _logger.info("Retrieved read with accession " + accession + " but library was null");
            }
        }
        return r;
    }

    public BioSequence getBioSequenceByBseEntityId(Long bseEntityId) {
        throw new HibernateException("someone needs to re-add the definition of findSequenceByBseEntityId");
        //Query query = getSessionFactory().getCurrentSession().getNamedQuery("findSequenceByBseEntityId");
//        query.setParameter("entityId", bseEntityId);
//        List list = query.list();
//        if (list.size() > 0)
//            b = (BioSequence) list.get(0);
//        return b;
    }

    public BlastResultNode getBlastHitResultDataNodeById(Long dataNodeId) {
        Session session = sessionFactory.getCurrentSession();
        return (BlastResultNode) session.get(BlastResultNode.class, dataNodeId);
    }

    public BlastResultNode getBlastHitResultDataNodeByTaskId(Long taskId) {
        Task task = getTaskById(taskId);
        if (task == null) {
            return null;
        }
        Query query = getCurrentSession().getNamedQuery("findBlastResultNodeByTaskId"); // Accesion is sic
        query.setParameter("taskId", taskId); // accesion is sic
        List nodes = query.list();
        if (nodes == null || nodes.size() < 1)
            return null;

        return (BlastResultNode) nodes.iterator().next();
    }

    public Long getBlastHitCountByTaskId(Long taskId) throws DaoException {
        return (Long) getCurrentSession()
                .getNamedQuery("findBlastHitCountByTaskId")
                .setParameter("taskId", taskId)
                .uniqueResult();
    }

    public BlastResultFileNode getBlastResultFileNodeByTaskId(Long taskId) throws DaoException {
        return (BlastResultFileNode) getCurrentSession()
                .getNamedQuery("findBlastResultFileNodeByTaskId")
                .setParameter("taskId", taskId)
                .uniqueResult();
    }

//    public Map<String, String> getDeflinesByAccessionSet(Set<String> accessionSet) {
//        assert false : "This method binds collection to a scalar variable (see setParameter)";
//        Map<String, String> deflineMap = new HashMap<String, String>();
//        Query query = sessionFactory.getCurrentSession().getNamedQuery("findDeflinesByAccessionSet");
//        query.setParameter("accession", accessionSet);
//
//        for (Iterator it = query.iterate(); it.hasNext();) {
//            Object[] obj = (Object[]) it.next();
//            deflineMap.put((String) obj[0], (String) obj[1]);
//        }
//        return deflineMap;
//    }


//    public Map<String, BaseSequenceEntity> getEntityIdsByAccessionSet(Set<String> accessionSet) {
//        Map<String, BaseSequenceEntity> entityIdMap = new HashMap<String, BaseSequenceEntity>();
//        // prepopulate map
//        for (String accession : accessionSet) {
//            entityIdMap.put(accession, null);
//
//        }
//        List<String> arrList = new ArrayList<String>(accessionSet);
//        Collection patialList;
//        // Postgres driver chokes on more then 10000 elements at a time
//        int chunk = 10000;
//        for (int from = 0, to = 0; from < arrList.size(); from = to) {
//            to = (to + chunk > arrList.size()) ? arrList.size() : to + chunk;
//            patialList = arrList.subList(from, to);
//            Criteria criteria = getCurrentSession().createCriteria(BaseSequenceEntity.class);
//            criteria.add(Restrictions.in("accession", patialList));
//            List<BaseSequenceEntity> bseList = criteria.list();
//
//            for (BaseSequenceEntity bse : bseList) {
//                entityIdMap.put(bse.getAccession(), bse);
//            }
//        }
//
//        return entityIdMap;
//
//    }

    public List<String> getFirstAccesions(int limit) {
        Query q = sessionFactory.getCurrentSession().createQuery("select bse.accession from BaseSequenceEntity bse");
        q.setMaxResults(limit);
        return q.list();
    }

    //Example if needed: PreparedStatement ps = session.connection().prepareStatement(new String());
    public Map<Long, String> getAllTaskPvoStrings() throws Exception {
        Session session = getCurrentSession();
        HashMap<Long, String> map = new HashMap<Long, String>();
        try {
            Query query = session.getNamedQuery("taskParameterPvoStringQuery");
            SQLQuery sqlQuery = session.createSQLQuery(query.getQueryString()).setResultSetMapping("taskParameterPvoStringMapping");
            List results = sqlQuery.list();
            for (Object result1 : results) {
                Object[] result = (Object[]) result1;
                Long taskId = (Long) result[0];
                String pvoString = (String) result[1];
                map.put(taskId, pvoString);
            }
        }
        catch (HibernateException e) {
            _logger.error("Error in getAllTaskPvoStrings\n" + e.getMessage(), e);
        }
        return map;
    }

    /**
     * This method is used by the RecruitmentNodeManager and updates num hits in the task.
     *
     * @param recruitmentNodeId - node whose task needs the new num hits value
     * @param numRecruited      - number of recruited (filtered)
     */
    public void setRVHitsForNode(Long recruitmentNodeId, String numRecruited) {
        try {
            RecruitmentResultFileNode tmpNode = (RecruitmentResultFileNode) getCurrentSession().load(RecruitmentResultFileNode.class, recruitmentNodeId);
            RecruitmentViewerFilterDataTask tmpTask = (RecruitmentViewerFilterDataTask) tmpNode.getTask();
            if (null != tmpTask && tmpTask.getNumHits() != Long.valueOf(numRecruited)) {
                _logger.debug("Saving change of " + tmpTask.getQuery() + " to " + numRecruited);
                tmpTask.setNumHits(new Long(numRecruited));
                getCurrentSession().saveOrUpdate(tmpTask);
            }
        }
        catch (HibernateException e) {
            _logger.error("Error in saveOrUpdateNode\n" + e.getMessage(), e);
        }
        catch (ParameterException e) {
            _logger.error("Error in saveOrUpdateNode\n" + e.getMessage(), e);
        }
    }

    public List<Object[]> getReadsByAccessions(HashSet<String> accSet) throws DaoException {
        try {
            StringBuffer sql = new StringBuffer("select se.defline, bs.sequence from sequence_entity se, " +
                    "bio_sequence bs where se.sequence_id=bs.sequence_id and se.accession in (");
            for (String s : accSet) {
                sql.append("'").append(s).append("',");
            }
            sql.deleteCharAt(sql.length() - 1);
            sql.append(")");

            if (_logger.isDebugEnabled()) _logger.debug("accSet length=" + accSet.size()/*+"\nhql=" + hql*/);
            SQLQuery query = getCurrentSession().createSQLQuery(sql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getReadsByAccessions");
        }
    }

    public List<Node> getNodesByClassAndUser(String className, String username) throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select clazz from " + className + " clazz");
            if (null != username) {
                hql.append("  where clazz.owner='").append(username).append("'");
            }
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getNodesByClassAndUser");
        }
    }

    public RecruitmentResultFileNode getSystemRecruitmentResultNodeByRecruitmentFileNodeId(String giNumber) throws DaoException {
        try {
            String queryString = "select node_id from node where task_id=(select tp.task_id from task_parameter tp, task t where tp.parameter_name='giNumber' and tp.parameter_value='" + giNumber + "' and t.task_id=tp.task_id and t.subclass='recruitmentViewerFilterDataTask' )";
            SQLQuery query = getCurrentSession().createSQLQuery(queryString);
            List returnList = query.list();
            if (null != returnList && returnList.size() >= 1) {
                BigInteger bigint = (BigInteger) returnList.iterator().next();
                return (RecruitmentResultFileNode) getNodeById(bigint.longValue());
            }
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getSystemRecruitmentResultNodeByRecruitmentFileNodeId");
        }
        return null;
    }

    public List getSampleInfo() throws DaoException {
        try {
            String queryString = "select b.sample_acc, b.sample_title, b.sample_name, ss.project, ss.project_name, min(l.min_insert_size ) as min_insert_size, max(l.max_insert_size) as max_insert_size from bio_sample b, sample_site ss, library l where ss.sample_name = b.sample_name and b.sample_acc = l.sample_acc group by b.sample_acc, b.sample_title, b.sample_name, ss.project, ss.project_name order by b.sample_acc";
            SQLQuery query = getCurrentSession().createSQLQuery(queryString);
            List returnList = query.list();
            if (null != returnList && returnList.size() >= 1) {
                return returnList;
            }
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getSampleInfo");
        }
        return null;
    }


    public List getHeaderDataForFRV(ArrayList readAccList) throws DaoException {
        try {
            StringBuffer sql = new StringBuffer("select rmp.accession, rmp.mate_acc, bs.sample_name " +
                    "from read_mate_pair rmp, bio_sample bs " +
                    "where rmp.accession in (:readAccList) and rmp.sample_acc=bs.sample_acc");
            if (_logger.isDebugEnabled()) _logger.debug("readAccList length=" + readAccList.size() + "\nsql=" + sql);
            Query query = getCurrentSession().createSQLQuery(sql.toString());
            query.setParameterList("readAccList", readAccList);
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getHeaderDataForFVR");
        }
    }

    public String getRecruitmentFilterDataTaskForUserByGenbankId(String genbankFileName, String userLogin) {
        StringBuffer sql = new StringBuffer("select task_id from task_parameter where task_id in (select task_id from task where task_owner='" + userLogin + "' and subclass='recruitmentViewerFilterDataTask') and parameter_name='genbankFileName' and parameter_value='" + genbankFileName + "'");
//        if (_logger.isDebugEnabled()) _logger.debug("Looking for ("+userLogin+","+genbankFileName+")\nsql=" + sql);
        Query query = getCurrentSession().createSQLQuery(sql.toString());
        List returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            return null;
        }
        if (1 < returnList.size()) {
            _logger.warn("getRecruitmentFilterDataTaskForUserByGenbankId returned " + returnList.size() + " results for this user.  Expecting ONLY one! Returning the first entry...");
        }
        BigInteger returnValue = (BigInteger) returnList.get(0);
        return returnValue.toString();
    }

    public void addEventToTask(Long taskId, Event event) throws DaoException {
        // Is the stack trace is thrown into the event description, trunkcate it.  Data should never be put in the event
        // description.
        if (event.getDescription().length()>MAX_MESSAGE_SIZE) {
            event.setDescription(event.getDescription().substring(0, MAX_MESSAGE_SIZE));
        }
        Task tmpTask = getTaskById(taskId);
        tmpTask.addEvent(event);
        saveOrUpdate(tmpTask);
    }

    public void setTaskParameter(Long taskId, String parameterKey, String parameterValue) throws DaoException {
        Task tmpTask = getTaskById(taskId);
        tmpTask.setParameter(parameterKey, parameterValue);
        saveOrUpdate(tmpTask);
    }

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state) throws DaoException {
        for (String jobId : jobIds) {
            GridJobStatus s = new GridJobStatus(taskId, jobId, queue, state);
            checkAndRecordError(s);
            saveOrUpdate(s);
        }
    }

    /**
     * Method to record a task error, when appropriate
     * @param status - object which has the SGE execution script info
     * @throws DaoException thrown when there is a problem recording a task error
     */
    private void checkAndRecordError(GridJobStatus status) throws DaoException {
        // If an exit code exists and is non-zero record an error.
        if (null!=status.getExitStatus() && 0!=status.getExitStatus()){
            addEventToTask(status.getTaskID(), new Event(Event.ERROR_EVENT, new Date(), Event.ERROR_EVENT));
        }
    }

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates) throws DaoException {
        for (String jobId : jobStates.keySet()) {
            updateJobStatus(taskId, jobId, jobStates.get(jobId));
        }
    }

    public void saveOrUpdateGridJobStatus(GridJobStatus gridJobStatus) throws DaoException {
        getCurrentSession().saveOrUpdate(gridJobStatus);
    }

    public void cleanUpGridJobStatus(long taskId) throws DaoException {
        Query query = getCurrentSession().createSQLQuery("update accounting set status = ? where task_id = ? and status not in (?, ?) ");
        query.setString(0, GridJobStatus.JobState.ERROR.name());
        query.setLong(1, taskId);
        query.setString(2, GridJobStatus.JobState.FAILED.name());
        query.setString(3, GridJobStatus.JobState.DONE.name());


        query.executeUpdate();
    }

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state) throws DaoException {
        GridJobStatus tmpStatus = getGridJobStatus(taskId, jobId);
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            saveOrUpdate(tmpStatus);
        }
        else {
            _logger.error("GridJobStatus for task_id:" + taskId + " and job_id:" + jobId + " NOT FOUND");
        }
    }

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) throws DaoException {
        GridJobStatus tmpStatus = getGridJobStatus(taskId, jobId);
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            tmpStatus.updateFromMap(infoMap);
            checkAndRecordError(tmpStatus);
            saveOrUpdate(tmpStatus);
        }
        else {
            _logger.error("GridJobStatus for task_id:" + taskId + " and job_id:" + jobId + " NOT FOUND");
        }
    }

    public GridJobStatus getGridJobStatus(long taskId, String jobId) {
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
                        _logger.error("getGridJobStatus found more then one entiry for task '" + taskId + "' and job '" + jobId + "'. Only first one will be used");
                        return (GridJobStatus) result.get(0);
                }
            }
        }
        catch (Exception e) {
            _logger.error("Unable to retrieve Jobs for task " + taskId + " due to exception " + e.toString());
        }
        return null;
    }

    public List<Long> getActiveTasks() {
        LinkedList<Long> tasks = new LinkedList<Long>();
        String sql = "select distinct(task_id) from accounting where status in ('" + GridJobStatus.JobState.QUEUED.name() + "', '" + GridJobStatus.JobState.RUNNING.name() + "' )";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            _logger.debug("No active tasks found - SQL: '" + sql + "'");
            return tasks; // empty list
        }
        for (BigInteger returnValue : returnList) {
            tasks.add(returnValue.longValue());
        }
        return tasks;
    }

    public List<Long> getWaitingTasks() {
        //_logger.debug("Getting the list of waiting tasks in the SGE queue.");
        LinkedList<Long> tasks = new LinkedList<Long>();
        String sql = "select distinct(task_id) from accounting where status in ('" + GridJobStatus.JobState.QUEUED.name() + "')";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            _logger.debug("No waiting tasks found - SQL: '" + sql + "'");
            return tasks; // empty list
        }
        for (BigInteger returnValue : returnList) {
            tasks.add(returnValue.longValue());
        }
        //_logger.debug("Number of waiting tasks are : " + tasks.size());
        return tasks;
    }


    public List<GridJobStatus> getGridJobStatusesByTaskId(long taskId, String[] states) {
        try {
            //   String sqlQuery = "select * from accounting where task_id=" + taskId + ";";
            //   SQLQuery query = getCurrentSession().createSQLQuery(sqlQuery);

            Query query;
            StringBuffer hql = new StringBuffer();
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
            _logger.error("Unable to retrieve Jobs for task " + taskId + " due to exception ", e);
        }
        return null;
    }

    public List getQueuedJobs() {
        Query query = getCurrentSession().createQuery("from GridJobStatus a where a.status<>'DONE' order by a.taskID");
        return query.list();
    }

    public List<Node> getNodeByName(String nodeName) throws DaoException {
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
        try {
            StringBuffer sql = new StringBuffer("update task t set expiration_date=current_timestamp from task_parameter p where p.parameter_value='" +
                    giNumber + "' and p.parameter_name='giNumber' and t.task_id=p.task_id and t.task_owner='system'");
            if (_logger.isDebugEnabled()) _logger.debug("sql=" + sql);
            SQLQuery query = getCurrentSession().createSQLQuery(sql.toString());
            query.executeUpdate();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "setDataRelatedToGiNumberObsolete");
        }
    }

    public Integer getPercentCompleteForATask(long taskId) {

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
        String hql = "select clazz from Node clazz where subclass ='" + nodeClassName + "' and order by clazz.description";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<? extends FileNode> getBlastDatabasesOfAUser(String nodeClassName, String username) {
        String hql = "select clazz from Node clazz where subclass ='" + nodeClassName + "' and " +
                " clazz.owner='" + username + "' and (visibility = 'public' or visibility = 'private')" +
                " order by clazz.description";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<Task> getUserTasksByType(String simpleName, String userName) {
        String hql = "select clazz from Task clazz where subclass='" + simpleName + "' and clazz.owner='" + userName + "' order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public Task getRecruitmentFilterTaskByUserPipelineId(Long pipelineId) throws DaoException {
        try {
            SQLQuery query = getSession().createSQLQuery("select task_id from task where parent_task_id=" + pipelineId
                    + " and subclass='recruitmentViewerFilterDataTask'");
            List results = query.list();
            if (null == results || results.size() == 0) {
                return null;
            }

            // Technically, should have one hit - very old data may not have
            BigInteger tmpResult = (BigInteger) results.get(0);
            return (Task) sessionFactory.getCurrentSession().get(Task.class, tmpResult.longValue());
        }
        catch (Exception e) {
            throw handleException(e, "TaskDAOImpl - getTaskById");
        }
    }


    public List<Task> getUserParentTasksByOwner(String userLogin) {
        String hql = "select clazz from Task clazz where clazz.owner='" + userLogin + "' and clazz.taskDeleted=false and clazz.parentTaskId is null order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public List<Task> getUserTasks(String userLogin) {
        String hql = "select clazz from Task clazz where clazz.owner='" + userLogin + "' and clazz.taskDeleted=false order by clazz.objectId";
        Query query = sessionFactory.getCurrentSession().createQuery(hql);
        return query.list();
    }

    public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException {
        Query query = sessionFactory.getCurrentSession().createQuery("select clazz from Task clazz where clazz.objectId=" + childTaskId);
        List tmpList = query.list();
        if (null != tmpList && tmpList.size() == 1) {
            Task tmpTask = (Task) tmpList.iterator().next();
            tmpTask.setParentTaskId(parentTaskId);
            saveOrUpdate(tmpTask);
        }
    }

    public List<User> getAllUsers() {
        Query query = sessionFactory.getCurrentSession().createQuery("select clazz from User clazz");
        return query.list();
    }

    public Long getSystemDatabaseIdByName(String databaseName) {
        Query query = sessionFactory.getCurrentSession().createQuery("select clazz from Node clazz where clazz.name='" + databaseName + "' and clazz.owner='system'");
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
        //_logger.info("Getting cumulative cpu time for task id " + taskId);
        long cpuTime = getCpuTime(taskId);

        String sql = "select task_id from Task where parent_task_id =" + taskId;
        Query query = getCurrentSession().createSQLQuery(sql);
        List<BigInteger> returnList = query.list();
        if (null == returnList || returnList.size() <= 0) {
            return cpuTime;
        }

        for (BigInteger childTaskId : returnList) {
            cpuTime = cpuTime + getCumulativeCpuTime(childTaskId.longValue());
        }

        _logger.info("Cumulative cpu time for task " + taskId + " is " + cpuTime);
        return cpuTime;
    }

    public long getCpuTime(long taskId) {
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
        String sql = "select e.defline, s.sequence from sequence_entity e, bio_sequence s where e.accession='" + targetAcc + "' and s.sequence_id=e.sequence_id;";
        StringBuffer returnEntry = new StringBuffer();
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
        try {
            StringBuffer hql = new StringBuffer("select clazz from GenericServiceDefinitionNode clazz");
            hql.append(" where clazz.name='").append(serviceName).append("'");
            hql.append(" and clazz.visibility != '").append(Node.VISIBILITY_INACTIVE).append("'");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
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


    /**
     * Method used to add new users to the system
     * @param userLogin - login of the user in the system
     * @return a formatted user object, or null if there was a problem
     * @throws DaoException thrown if there was a problem adding the user to the database
     */
    public User createUser(String userLogin) throws DaoException {
        User tmpUser;
        try {
            tmpUser = getUserByName(userLogin);
            if (null!=tmpUser) {
                _logger.warn("Cannot create user "+userLogin+" as they already exist!");
                return tmpUser;
            }
            else {
                tmpUser = new User();
                tmpUser.setUserLogin(userLogin);
                saveOrUpdate(tmpUser);
                return tmpUser;
            }
        }
        catch (DaoException e) {
            throw handleException(e, "createUser");
        }
    }
    
    public String createTempFile(String prefix, String content) throws DaoException {
    	
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
}