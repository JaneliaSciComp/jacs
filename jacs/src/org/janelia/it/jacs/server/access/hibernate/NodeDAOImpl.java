
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.collection.GWTEntityCleaner;
import org.hibernate.criterion.*;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.genomics.SequenceType;
//import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.DataSource;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatasetNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
//import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.server.access.NodeDAO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Lfoster
 * Date: Aug 10, 2006
 * Time: 1:37:31 PM
 * Implementation of data node data access object, using hibernate criteria for query.
 */
public class NodeDAOImpl extends DaoBaseImpl implements NodeDAO {
    private static Logger _logger = Logger.getLogger(NodeDAOImpl.class);

    // DAO's can only come from Spring's Hibernate
    private NodeDAOImpl() {
    }

    public List<Node> getUserSpecificData(String targetUser) throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Node.class);
            Criterion userNode = Expression.eq("owner", targetUser);
            criteria.add(userNode);
            Criterion deprecatedCriterion = Expression.ne("visibility", Node.VISIBILITY_PRIVATE_DEPRECATED);
            criteria.add(deprecatedCriterion);
            Criterion inactiveCriterion = Expression.ne("visibility", Node.VISIBILITY_INACTIVE);
            criteria.add(inactiveCriterion);
            return getHibernateTemplate().findByCriteria(criteria);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getUserSpecificData");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getUserSpecificData");
        }
    }

    public BlastResultNode getBlastResultNodeByTaskId(Long taskId) throws DataAccessException, DaoException {
        try {
            long start = System.currentTimeMillis();
            _logger.debug("NodeDAOImpl");
            DetachedCriteria taskCriteria = DetachedCriteria.forClass(Task.class);
            Criterion taskCriterion = Expression.eq("objectId", taskId);
            taskCriteria.add(taskCriterion);
            List tasks = getHibernateTemplate().findByCriteria(taskCriteria);

            if (tasks == null || tasks.size() < 1) return null;

            Task task = (Task) tasks.iterator().next();
            _logger.debug("NodeDAOImpl found task " + task.getObjectId());

            DetachedCriteria nodeCriteria = DetachedCriteria.forClass(BlastResultNode.class);
            nodeCriteria.add(Expression.eq("task", task));
            List nodes = getHibernateTemplate().findByCriteria(nodeCriteria);

            if (nodes == null || nodes.size() < 1) return null;

            BlastResultNode node = (BlastResultNode) nodes.iterator().next();
            _logger.debug("NodeDAOImpl found node " + node.getObjectId());

            // try NOT pre-populating the set of 
//            Set hits = node.getBlastHitResultSet();
//
//            _logger.debug("NodeDAOImpl found " + ((hits == null) ? 0 : hits.size()) + "hits");
//            _logger.debug("NodeDAOImpl loaded " + node.getBlastHitResultSet().size());

            long end = System.currentTimeMillis();
            if (logger.isInfoEnabled()) logger.info("Time to do getBlastResultNodeByTaskId: " + (end - start));
            return node;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByTaskId");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByTaskId");
        }
    }

    public List<Node> getTaskOutputNodes(Long taskId, Class clazz) throws DaoException {
        try {
            _logger.debug("NodeDAOImpl.getTaskOutputNodes " + taskId);
            if (taskId == null || taskId == 0) {
                return null;
            }
            String hql = "select outNode " +
                    "from Task t " +
                    "inner join t.outputNodes outNode " +
                    "where t.objectId = :taskId " +
                    (clazz != null ? "and outNode.class = " + clazz.getCanonicalName() : "");
            _logger.debug("hql=" + hql);
            Query query = getSession().createQuery(hql);
            query.setParameter("taskId", taskId);
            List<Node> results = query.list();
            _logger.debug("Selected " + results.size() + " task output node(s)");
            return results;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getTaskOutputNodes");
        }
        catch (Exception e) {
            throw handleException(e, "NodeDAOImpl - getTaskOutputNodes");
        }
    }

    /**
     * @return List<Object[]> where Object[<BlastHit>][<String queryDefline>][<String subjectDefline>]
     */
    public List<Object[]> getPagedBlastHitsByTaskId(Long taskId,
                                                    int startIndex,
                                                    int numRows,
                                                    boolean includeHSPRanking,
                                                    SortArgument[] sortArgs)
    //SortArgument[] sortArgs,
    //Long userId)
            throws DataAccessException, DaoException {
        try {
            _logger.debug("NodeDAOImpl.getPagedBlastHitsByTaskId: " + taskId);
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            String rankRestriction = null;
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals("rank")) {
                        dataSortField = "hit.rank";
                        if (startIndex >= 0 && numRows > 0) {
                            // only create a rank restriction if the startIndex and numRows are valid
                            rankRestriction = "hit.rank between " + startIndex + " and " + (startIndex + numRows - 1) + " ";
                        }
                    }
                    else if (dataSortField.equals("bitScore")) {
                        dataSortField = "hit.bitScore";
                    }
                    else if (dataSortField.equals("lengthAlignment")) {
                        dataSortField = "hit.lengthAlignment";
                    }
                    else if (dataSortField.equals("subjectAcc")) {
                        dataSortField = "hit.subjectAcc";
                    }
                    else if (dataSortField.equals("queryDef")) {
                        dataSortField = "queryDef";
                    }
                    else if (dataSortField.equals("subjectDef")) {
                        dataSortField = "subjectDef";
                    }
                    else if (dataSortField.equals("sampleName")) {
                        dataSortField = "sample.sampleName";
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
                orderByClause = "order by hit.bitScore desc ";
            }
            else {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hspFields = null;
            if (includeHSPRanking) {
                hspFields = "(select count(h2.rank) " +
                        "    from node.blastHitResultSet h2 " +
                        "    where h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc " +
                        "      and h2.rank < hit.rank) as hspRank, " +
                        "   (select count(*) " +
                        "    from node.blastHitResultSet h2 " +
                        "    where h2.queryAcc = hit.queryAcc " +
                        "      and h2.subjectAcc = hit.subjectAcc) as nhsps ";
            }
            // Retrieve the hits from the node, plus the query and subject deflines
            String hql = "select hit, " +
                    "       queryDef, " +
                    "       subjectDef " +
                    (hspFields != null ? "," + hspFields : "") +
                    "from BlastResultNode node " +
                    "inner join node.blastHitResultSet hit " +
                    "left join hit.queryEntity queryEntity " +
                    "left join hit.subjectEntity subjectEntity " +
                    "left join subjectEntity.sample sample " +
                    "inner join node.deflineMap queryDef " +
                    "inner join node.deflineMap subjectDef " +
                    "where node.task.objectId = :taskId " +
                    "  and index(subjectDef) = hit.subjectAcc " +
                    "  and index(queryDef) = hit.queryAcc " +
                    //"  and node.user = :userId " +
                    ((rankRestriction != null) ? "and " + rankRestriction : "") +
                    orderByClause;
            _logger.debug("hql=" + hql);
            // Get the appropriate range of Node's hits, sorted by the specified field
            Query query = getSession().createQuery(hql);
            query.setLong("taskId", taskId);
            //query.setLong("userId",userId);
            if (rankRestriction == null) { // need to restrict result set if not done by rank clause
                if (startIndex > 0) {
                    query.setFirstResult(startIndex);
                }
                if (numRows > 0) {
                    query.setMaxResults(numRows);
                }
            }
            List<Object[]> results = query.list();
            _logger.debug("NodeDAOImpl found " + ((results == null) ? 0 : results.size()) + " hits");
            return results;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByTaskId");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByTaskId");
        }
    }

    public BlastResultNode getBlastResultNodeByNodeId(Long nodeId)
            throws DataAccessException, DaoException {
        try {
            return (BlastResultNode) getHibernateTemplate().get(BlastResultNode.class, nodeId);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByNodeId");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getBlastResultNodeByNodeId");
        }
    }

    /**
     * Only seems to be used for offline data validation
     */
    public List<Node> getPagedBlastableSubjectNodes(String[] userLogins, int startIndex, int numRows) throws DaoException {
        DetachedCriteria criteria = DetachedCriteria.forClass(Node.class, "blastableSubject");
        // add user Restrictions
        if (userLogins != null && userLogins.length > 0) {
            // include visibility restrictions
            criteria.add(Restrictions.or(
                    Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC),
                    Restrictions.in("owner", userLogins)));
        }
        else {
            // only select public nodes
            criteria.add(Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC));
        }
        // add node type restrictions
        criteria.add(Restrictions.or(
                Restrictions.eq("blastableSubject.class", BlastDatasetNode.class),
                Restrictions.eq("blastableSubject.class", BlastDatabaseFileNode.class)
        ));
        // add ordering
        criteria.addOrder(Order.asc("name"));
        List<Node> blastSubjectNodes = getHibernateTemplate().findByCriteria(criteria, startIndex, numRows);
        return blastSubjectNodes;
    }

//    public Map<BioMaterial, Integer> getSitesForBlastResultNode(Long taskId)
//            throws DataAccessException, DaoException {
//        try {
//            String sql =
//                    "select\n" +
//                            "  ss.material_acc as site_acc,\n" +
//                            "  ss.latitude,\n" +
//                            "  ss.longitude,\n" +
//                            "  ss.location as sample_location,\n" +
//                            "  count(distinct bh.subject_acc) as number_of_sequences\n" +
//                            "from \n" +
//                            "   flyportal.node n\n" +
//                            "   inner join flyportal.blast_hit bh on bh.result_node_id=n.node_id\n" +
//                            "   inner join flyportal.sequence_entity subj on subj.accession=bh.subject_acc\n" +
//                            "   inner join flyportal.sample_site ss on ss.sample_acc=subj.sample_acc\n" +
//                            "where n.task_id=" + taskId.toString() + "\n" +
//                            "group by ss.material_acc, ss.longitude, ss.latitude, ss.location";
//            Query query = getSession().createSQLQuery(sql);
//            _logger.debug(query);
//
//            List list = query.list();
//            if (list == null || list.size() == 0) return null;
//
//            // Convert the List<Object[]> to Map<Site, Integer>
//            Map<BioMaterial, Integer> sites = new HashMap<BioMaterial, Integer>();
//            for (Object listItem : list) {
//                Object[] items = (Object[]) listItem;
//                BioMaterial site = new BioMaterial((String) items[0], (String) items[1], (String) items[2], (String) items[3]);
//                sites.put(site, new Integer(((BigInteger) items[4]).intValue()));  // convert BigInt to Int
//            }
//
//            _logger.debug("getSitesForBlastResultNode() retrieved " + sites.size() + " sites");
//            return sites;
//        }
//        catch (DataAccessResourceFailureException e) {
//            throw handleException(e, "NodeDAOImpl - getSitesForBlastResultNode");
//        }
//        catch (IllegalStateException e) {
//            throw handleException(e, "NodeDAOImpl - getSitesForBlastResultNode");
//        }
//        catch (Throwable t) {
//            throw handleThrowable(t, "NodeDAOImpl - getSitesForBlastResultNode");
//        }
//    }
//
    public void saveOrUpdateNode(Node targetNode) throws DataAccessException, DaoException {
        saveOrUpdateObject(targetNode, "NodeDAOImpl - saveOrUpdateNode");
    }

    public FastaFileNode getFastaFileNode(Long nodeId) throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(FastaFileNode.class);
            criteria.add(Expression.eq("objectId", nodeId));
            List fileNode = getHibernateTemplate().findByCriteria(criteria);
            if (null != fileNode && 1 == fileNode.size()) {
                return (FastaFileNode) fileNode.iterator().next();
            }
            return null;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getFastaFileNode");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getFastaFileNode");
        }
    }

    public BlastableNodeVO getBlastableDatabaseByNodeId(String targetUser, String nodeId) throws DataAccessException, DaoException {
        List<Node> blastableSubjectNodeList;
        try {
            blastableSubjectNodeList = getPagedBlastableSubjectNodes(new String[]{targetUser},
                    SequenceType.NOT_SPECIFIED, 0, -1, new SortArgument[]{new SortArgument("ord")}, nodeId);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
        }

        if (blastableSubjectNodeList != null)
            return createBlastableNodeVO(blastableSubjectNodeList.get(0));
        else
            return null;
    }

    public BlastableNodeVO[] getBlastableDatabaseList(String sequenceType, String targetUser) throws DataAccessException, DaoException {
        List<Node> blastableSubjectNodeList;
        try {
            blastableSubjectNodeList = getPagedBlastableSubjectNodes(new String[]{targetUser}, sequenceType,
                    0, -1, new SortArgument[]{new SortArgument("ord")});
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
        }
        BlastableNodeVO[] result;
        if (blastableSubjectNodeList == null) {
            result = new BlastableNodeVO[0];
        }
        else {
            result = new BlastableNodeVO[blastableSubjectNodeList.size()];
            int resIndex = 0;
            for (Node subjectNode : blastableSubjectNodeList)
                result[resIndex++] = createBlastableNodeVO(subjectNode);
        }
        return result;
    }


//    public BlastableNodeVO[] getReversePsiBlastDatasets() throws DaoException {
//        List<ReversePsiBlastDatabaseNode> results;
//        try {
//            StringBuffer hqlQuery = new StringBuffer();
//            hqlQuery.append("select clazz from ReversePsiBlastDatabaseNode clazz order by clazz.name");
//            Query query = getSession().createQuery(hqlQuery.toString());
//            _logger.info(query.toString());
//
//            // NOTE: if multiple columns, the result would have been a list of Object arrays.
//            results = query.list();
//            _logger.info("Query resulted in " + results.size() + " hits");
//        }
//        catch (HibernateException e) {
//            throw convertHibernateAccessException(e);
//        }
//        catch (DataAccessResourceFailureException e) {
//            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
//        }
//        catch (IllegalStateException e) {
//            throw handleException(e, "NodeDAOImpl - getBlastableDatabaseList");
//        }
//        BlastableNodeVO[] result;
//        if (results == null) {
//            result = new BlastableNodeVO[0];
//        }
//        else {
//            result = new BlastableNodeVO[results.size()];
//            int resIndex = 0;
//            for (Node subjectNode : results)
//                result[resIndex++] = createBlastableNodeVO(subjectNode);
//        }
//        return result;
//    }
//
//
    /**
     * @param targetNode             - place where the data lives
     * @param commonParameterKey     - task parameterKey to match
     * @param commonParameterValue   - task parameterValues to match
     * @param taskSubclassLikeString - task types to search across for events
     * @return list of ordered events
     */
    public List<Event> getAllEventsRelatedToData(Node targetNode, String commonParameterKey, String commonParameterValue,
                                                 String taskSubclassLikeString) {
        String hql = "select event from Event event where event.task in " +
                "(select t from TaskParameter tp, Task t " +
                " where tp.task=t " +
                " and tp.name= :commonKey and tp.value like :commonValue and t.taskName " +
                " like :taskLikeString ) order by event.timestamp desc ";
        Query query = getSession().createQuery(hql);
        query.setParameter("commonKey", commonParameterKey);
        query.setParameter("commonValue", "%" + commonParameterValue + "%");
        query.setParameter("taskLikeString", "%" + taskSubclassLikeString + "%");

        List<Event> returnList = (List<Event>) query.list();
        for (Event event : returnList) {
            GWTEntityCleaner.evictAndClean(event, getSession());
        }
        logger.debug("Returned a list of " + returnList.size() + " events.");
        return returnList;
    }

    private BlastableNodeVO createBlastableNodeVO(Node subjectNode) {
        String sequenceType = null;
        String nodeOwner = null;
        String numSequences = "";
        DataSource dataSource=null;
        boolean isAssembled = Boolean.FALSE;
        if (subjectNode instanceof BlastDatabaseFileNode) {
            BlastDatabaseFileNode bn = (BlastDatabaseFileNode) subjectNode;
            sequenceType = bn.getSequenceType();
            Integer count = bn.getSequenceCount();
            numSequences = count == null ? "" : count.toString();
            dataSource = bn.getDataSource();
            if (bn.getIsAssembledData() != null) {
                isAssembled = bn.getIsAssembledData();
            }
        }
        else if (subjectNode instanceof BlastDatasetNode) {
            sequenceType = ((BlastDatasetNode) subjectNode).getSequenceType();
        }
//        else if (subjectNode instanceof ReversePsiBlastDatabaseNode) {
//            Long count = subjectNode.getLength();
//            numSequences = null == count ? "" : count.toString();
//        }
        if (subjectNode.getOwner() != null) {
            nodeOwner = subjectNode.getOwner();
        }
        return new BlastableNodeVO(subjectNode.getObjectId().toString(),
                subjectNode.getName(),
                subjectNode.getDescription(),
                subjectNode.getDataType(),
                sequenceType,
                nodeOwner,
                subjectNode.getLength() == null ? "" : String.valueOf(subjectNode.getLength()),
                numSequences,
                subjectNode.getVisibility(),
                dataSource,
                isAssembled,
                subjectNode.getOrd());
    }

    public int getNumBlastableSubjectNodes(String[] userLogins) throws DaoException {
        DetachedCriteria criteria = DetachedCriteria.forClass(BlastDatabaseFileNode.class, "blastableSubject");
        // add user Restrictions
        if (userLogins != null && userLogins.length > 0) {
            // include visibility restrictions
            criteria.add(Restrictions.or(
                    Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC),
                    Restrictions.in("owner", userLogins)));
        }
        else {
            // only select public nodes
            criteria.add(Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC));
        }
        // add node type restrictions
/*
        criteria.add(Restrictions.or(
            Restrictions.eq("blastableSubject.class",BlastDatasetNode.class),
            Restrictions.eq("blastableSubject.class", BlastDatabaseFileNode.class)
        ));
*/
        criteria.setProjection(Projections.count("blastableSubject.objectId"));
        List<Integer> queryResult = getHibernateTemplate().findByCriteria(criteria);
        int result = -1;
        if (queryResult != null || queryResult.size() > 0) {
            result = queryResult.get(0);
        }
        return result;
    }

    public List<Node> getPagedBlastableSubjectNodes(String[] userLogins, String sequenceType, int startIndex, int numRows, SortArgument[] sortArgs)
            throws DaoException {
        return getPagedBlastableSubjectNodes(userLogins, sequenceType, startIndex, numRows, sortArgs, null);
    }

    public List<Node> getPagedBlastableSubjectNodes(String[] userLogins, String sequenceType, int startIndex, int numRows,
                                                    SortArgument[] sortArgs, String id)
            throws DaoException {
        DetachedCriteria criteria = DetachedCriteria.forClass(BlastDatabaseFileNode.class, "blastableSubject");
        // add user Restrictions
        if (userLogins != null && userLogins.length > 0) {
            // include visibility restrictions
            criteria.add(Restrictions.or(
                    Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC),
                    Restrictions.in("owner", userLogins)));
        }
        else {
            // only select public nodes
            criteria.add(Restrictions.eq("visibility", Node.VISIBILITY_PUBLIC));
        }
        if (!SequenceType.NOT_SPECIFIED.equalsIgnoreCase(sequenceType)) {
            Criterion seqTypeCriterion = Expression.eq("sequenceType", sequenceType);
            criteria.add(seqTypeCriterion);
        }
        if (id != null)
            criteria.add(Restrictions.eq("objectId", new Long(id)));

        // add node type restrictions
/*
        criteria.add(Restrictions.or(
            Restrictions.eq("blastableSubject.class",BlastDatasetNode.class),
            Restrictions.eq("blastableSubject.class",BlastDatabaseFileNode.class)
        ));
*/
        // add ordering
        if (sortArgs == null) {
            criteria.addOrder(Order.asc("ord"));
        }
        else {
            for (SortArgument sortArg : sortArgs) {
                if (sortArg.isAsc()) {
                    criteria.addOrder(Order.asc(sortArg.getSortArgumentName()));
                }
                else if (sortArg.isDesc()) {
                    criteria.addOrder(Order.desc(sortArg.getSortArgumentName()));
                }
            }
        }
        List<Node> blastSubjectNodes = getHibernateTemplate().findByCriteria(criteria, startIndex, numRows);
        return blastSubjectNodes;
    }

    public List getNodeNames(List nodeIds) throws DataAccessException, DaoException {
        List nodeNames;
        try {
            // Add the string id's to the list as Longs
            ArrayList<Long> tmpNodes = new ArrayList<Long>();
            for (Object n : nodeIds) {
                if (n instanceof String) {
                    tmpNodes.add(new Long((String) n));
                }
                else if (n instanceof Long) {
                    tmpNodes.add((Long) n);
                }
                else {
                    String message = "List nodeId content must be type String or Long";
                    throw new DaoException(new Exception(message), message);
                }
            }
            DetachedCriteria criteria = DetachedCriteria.forClass(Node.class);
            criteria.add(Restrictions.in("objectId", tmpNodes));
            nodeNames = getHibernateTemplate().findByCriteria(criteria);
            if (null == nodeNames) {
                nodeNames = new ArrayList();
                nodeNames.add("Not available");
            }
        }
        catch (DataAccessException e) {
            _logger.error("Error obtaining node names. " + e.getMessage(), e);
            nodeNames = new ArrayList();
            nodeNames.add("Not available");
        }
        return nodeNames;
    }

    public Node getNodeById(Long nodeId) throws DataAccessException, DaoException {
        try {
            HibernateTemplate ht = getHibernateTemplate();
            return (Node) ht.getSessionFactory().getCurrentSession().get(Node.class, nodeId);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getNodeById");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getNodeById");
        }
    }

    public List<Node> getNodesByIds(List<Long> nodeIds) throws DataAccessException, DaoException {
        List<Node> nodes = null;
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Node.class);
            criteria.add(Restrictions.in("objectId", nodeIds));
            nodes = getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessException e) {
            _logger.error("Error obtaining nodes. " + e.getMessage(), e);
        }
        return nodes;
    }

    public Node getNodeByName(String name) throws DataAccessException, DaoException {
        Query query = getSession().getNamedQuery("findNodeByName");
        query.setParameter("name", name);
        return (Node) query.uniqueResult();
    }

    public String replaceNodeName(String nodeId, String nodeName)
            throws DataAccessException, DaoException {
        Node node = (Node) getHibernateTemplate().load(Node.class, new Long(nodeId.trim()));
        String currentName = node.getName();
        if (currentName.startsWith("upload_")) {
            // Then we cannot change the name because this format is required by the upload controller,
            // but we can change the description instead.
            node.setDescription(nodeName);
        }
        else {
            node.setName(nodeName);
        }
        getHibernateTemplate().saveOrUpdate(node);
        String newName = node.getName();
        if (newName.startsWith("upload_")) {
            // We want to return the description in this case
            return node.getDescription();
        }
        else {
            return node.getName();
        }
    }

    // Once deletion policy is decided, the use of Node.VISIBILITY_PRIVATE_DEPRECATED can be reconsidered
    public void markNodeForDeletion(String nodeId)
            throws DataAccessException, DaoException {
        Node node = (Node) getHibernateTemplate().load(Node.class, new Long(nodeId.trim()));
        node.setVisibility(Node.VISIBILITY_PRIVATE_DEPRECATED);
        getHibernateTemplate().saveOrUpdate(node);
    }

    public int getNumBlastableNodesForUser(String searchString, String sequenceType, String user)
            throws DataAccessException, DaoException {
        try {
            _logger.debug("executing getNumBlastableNodesForUser where user=" + user);

            // Results
            int totalCount = 0;
            DetachedCriteria criteria = createBlastableNodesForUserQuery(searchString, sequenceType, user);
            criteria.setProjection(Projections.rowCount());
            List<Integer> list = (List<Integer>) getHibernateTemplate().findByCriteria(criteria);
            if (list != null && list.size() > 0) {
                totalCount = list.get(0);
                _logger.debug("BlastableNodes count=" + totalCount);
            }
            else {
                _logger.debug("Found no blastableNodes");
            }
            return totalCount;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getNumBlastableNodesForUser");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getNumBlastableNodesForUser");
        }
    }

    private DetachedCriteria createBlastableNodesForUserQuery(String searchString, String sequenceType, String user) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Node.class, "blastableNode");
        Criterion userNode = Expression.eq("owner", user);
        criteria.add(userNode);
        criteria.add(Restrictions.eq("blastableNode.class", "FastaFileNode"));
        Criterion deprecatedCriterion = Expression.ne("visibility", Node.VISIBILITY_PRIVATE_DEPRECATED);
        Criterion inactiveCriterion = Expression.ne("visibility", Node.VISIBILITY_INACTIVE);
        criteria.add(deprecatedCriterion);
        criteria.add(inactiveCriterion);
        // Some mechanisms want to only display lists of specific sequence type
        if (!SequenceType.NOT_SPECIFIED.equalsIgnoreCase(sequenceType)) {
            Criterion seqTypeCriterion = Expression.eq("sequenceType", sequenceType);
            criteria.add(seqTypeCriterion);
        }
        if (searchString != null && searchString.length() > 0)
            criteria.add(Expression.like("name", searchString).ignoreCase());

        return criteria;
    }

    /**
     * The method searches the FastaFileNode(s) for the <code>user</code>
     */
    public List<Node> getPagedBlastableNodesForUser(String searchString, String sequenceType, int startIndex, int numRows, SortArgument[] sortArgs,
                                                    String user) throws DataAccessException, DaoException {
        try {
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals(UserDataNodeVO.SORT_BY_NODE_ID) ||
                            dataSortField.equals(UserDataNodeVO.SORT_BY_DATE_CREATED) ||
                            dataSortField.equals("objectId")) {
                        dataSortField = "objectId";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_NAME) ||
                            dataSortField.equals("name")) {
                        dataSortField = "name";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_DESCRIPTION) ||
                            dataSortField.equals("description")) {
                        dataSortField = "description";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_TYPE) ||
                            dataSortField.equals("sequenceType")) {
                        dataSortField = "sequenceType";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_LENGTH) ||
                            dataSortField.equals("totalQuerySeqeuenceLength")) {
                        // Note: This is a hack until Hibernate will start supporting
                        // column aliases properly!
                        dataSortField = "col_1_0_";
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
                }
            }
            String seqTypeFilter = "";
            if (!SequenceType.NOT_SPECIFIED.equalsIgnoreCase(sequenceType)) {
                seqTypeFilter = "and bn.sequenceType='" + sequenceType + "' ";
            }

            String orderByClause = "";
            if (orderByFieldsBuffer.length() > 0) {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hql =
                    "select bn from Node bn " +
                            "where bn.owner = :owner " +
                            "and bn.class in (FastaFileNode) " +
                            "and bn.visibility != :visibility and bn.visibility != :inactive " + seqTypeFilter + " ";
            if (searchString != null && searchString.length() > 0)
                hql += "and lower(bn.name) like lower(:searchString) ";
            hql += orderByClause;

            Query query = getSession().createQuery(hql);
            query.setParameter("owner", user);
            query.setParameter("visibility", Node.VISIBILITY_PRIVATE_DEPRECATED);
            query.setParameter("inactive", Node.VISIBILITY_INACTIVE);
            if (searchString != null && searchString.length() > 0)
                query.setParameter("searchString", searchString);
            if (numRows > 0) {
                query.setFirstResult(startIndex);
                query.setMaxResults(numRows);
            }
            _logger.debug("BlastableNodes HQL: " + hql);
            List<Node> list = (List<Node>)query.list();
            List<Node> blastableNodeList = new ArrayList<Node>();
            for (Node resEntry : list) {
                Long querySeqLength = resEntry.getLength();
                if (querySeqLength == null) {
                    querySeqLength = (long) -1;
                }
                resEntry.setLength(querySeqLength);
                blastableNodeList.add(resEntry);
            }
            return blastableNodeList;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getPagedBlastableNodesForUser");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getPagedBlastableNodesForUser");
        }
    }

    public Long getNumBlastHitsForNode(Node node)
            throws DataAccessException, DaoException {
        Long result = null;
        if (node instanceof BlastResultNode) {
            String sql = "select count(h.blast_hit_id)\n" +
                    "  from flyportal.blast_hit h where h.result_node_id=" + node.getObjectId();
            Query query = getSession().createSQLQuery(sql);
            List list = query.list();
            if (list.get(0) != null) {
                result = new Long(list.get(0).toString());
            }
        }
        else if (node instanceof BlastResultFileNode) {
            BlastResultFileNode brfn = (BlastResultFileNode) node;
            result = brfn.getBlastHitCount();
        }
        return result;
    }

    public List<String> getBlastableNodeNamesForUser(String searchString, String sequenceType, String user)
            throws DataAccessException, DaoException {
        try {
            _logger.debug("executing getBlastableNodeNamesForUser where user=" + user);

            DetachedCriteria criteria = createBlastableNodesForUserQuery(searchString, sequenceType, user);
            criteria.setProjection(Property.forName("name")); // restrict the query to pull just the name

            List<String> list = (List<String>) getHibernateTemplate().findByCriteria(criteria);
            _logger.debug("Found " + ((list == null || list.size() == 0) ? 0 : list.size()) + " node names");

            return list;
        }
        catch (Exception e) {
            logger.error("Error in getBlastableNodeNamesForUser:" + e.getMessage(), e);
            throw handleException(e, "getBlastableNodeNamesForUser");
        }
    }

    public Integer getNumNodesForUserByName(String nodeClassName, String user) throws DataAccessException, DaoException {
        try {
            _logger.debug("executing getNumBlastableNodesForUser where user=" + user);

            // Results
            int totalCount = 0;
            DetachedCriteria criteria = createNodesForUserByNameQuery(nodeClassName, user);
            criteria.setProjection(Projections.rowCount());
            List<Integer> list = (List<Integer>) getHibernateTemplate().findByCriteria(criteria);
            if (list != null && list.size() > 0) {
                totalCount = list.get(0);
                _logger.debug("Nodes count=" + totalCount);
            }
            else {
                _logger.debug("Found no Nodes");
            }
            return totalCount;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
        catch (Exception e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
    }

    private DetachedCriteria createNodesForUserByNameQuery(String nodeClassName, String user) throws Exception {
        Class nodeClass = Class.forName(nodeClassName);
        DetachedCriteria criteria = DetachedCriteria.forClass(nodeClass);
        Criterion userNode = Expression.eq("owner", user);
        criteria.add(userNode);
        Criterion deprecatedCriterion = Expression.ne("visibility", Node.VISIBILITY_PRIVATE_DEPRECATED);
        Criterion inactiveCriterion = Expression.ne("visibility", Node.VISIBILITY_INACTIVE);
        criteria.add(deprecatedCriterion);
        criteria.add(inactiveCriterion);
        return criteria;
    }

    public List<Node> getPagedNodesForUserByName(String nodeClassName,
                                                 int startIndex,
                                                 int numRows,
                                                 SortArgument[] sortArgs,
                                                 String user) throws DataAccessException, DaoException {
        try {
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals(UserDataNodeVO.SORT_BY_NODE_ID) ||
                            dataSortField.equals(UserDataNodeVO.SORT_BY_DATE_CREATED) ||
                            dataSortField.equals("objectId")) {
                        dataSortField = "objectId";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_NAME) ||
                            dataSortField.equals("name")) {
                        dataSortField = "name";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_DESCRIPTION) ||
                            dataSortField.equals("description")) {
                        dataSortField = "description";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_TYPE) ||
                            dataSortField.equals("sequenceType")) {
                        dataSortField = "sequenceType";
                    }
                    else if (dataSortField.equals(UserDataNodeVO.SORT_BY_LENGTH) ||
                            dataSortField.equals("totalQuerySeqeuenceLength")) {
                        // Note: This is a hack until Hibernate will start supporting
                        // column aliases properly!
                        dataSortField = "col_1_0_";
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
                }
            }
            String orderByClause = "";
            if (orderByFieldsBuffer.length() > 0) {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hql =
                    "select n from Node n " +
                            "where n.owner = :owner " +
                            "and n.class in (" + nodeClassName + ") " +
                            "and n.visibility != :visibility and n.visibility != :inactive ";
            hql += orderByClause;

            Query query = getSession().createQuery(hql);
            query.setParameter("owner", user);
            query.setParameter("visibility", Node.VISIBILITY_PRIVATE_DEPRECATED);
            query.setParameter("inactive", Node.VISIBILITY_INACTIVE);
            if (numRows > 0) {
                query.setFirstResult(startIndex);
                query.setMaxResults(numRows);
            }
            _logger.debug("Paged Nodes for User by Name HQL: " + hql);
            List<Node> list = query.list();
            List<Node> nodeList = new ArrayList<Node>();
            for (Node resEntry : list) {
                Long querySeqLength = resEntry.getLength();
                if (querySeqLength == null) {
                    resEntry.setLength(-1l);
                }
                nodeList.add(resEntry);
            }
            return nodeList;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getPagedNodesForUserByName");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getPagedNodesForUserByName");
        }
    }


    public List<String> getNodeNamesForUserByName(String nodeClassName, String userLogin) throws DaoException {
        try {
            _logger.debug("executing getNodeNamesForUserByName where user=" + userLogin+ " and class is "+nodeClassName);

            // Results
            DetachedCriteria criteria = createNodesForUserByNameQuery(nodeClassName, userLogin);
            List<Node> list = (List<Node>) getHibernateTemplate().findByCriteria(criteria);
            ArrayList<String> returnList = new ArrayList<String>();
            for (Node node : list) {
                returnList.add(node.getName());
            }
            return returnList;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
        catch (Exception e) {
            throw handleException(e, "NodeDAOImpl - getNumNodesForUserByName");
        }
    }
}
