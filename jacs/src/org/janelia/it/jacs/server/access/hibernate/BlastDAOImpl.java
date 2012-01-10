
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.server.access.BlastDAO;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigInteger;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lfoster
 * Date: Nov 20, 2006
 * Time: 2:15:11 PM
 */
public class BlastDAOImpl extends DaoBaseImpl implements BlastDAO {
    private static Logger _log = Logger.getLogger(BlastDAOImpl.class);

//    private static final String FILTERABLE_STRING = "geographic_location";

    public Map<String, String> getNodeIdVsSiteLocation(String project) throws DaoException {
        try {
            // Query to find all site _accessions_   vs node ids.
            StringBuffer sqlQuery = new StringBuffer();
            sqlQuery.append("select distinct material_id, node_id from camera.bio_material_blast_node");
            SQLQuery query = getSession().createSQLQuery(sqlQuery.toString());
            _log.info(query.toString());
            List<Object[]> materialIdVsNodeArr = (List<Object[]>) query.list();
            _log.info("Query resulted in " + materialIdVsNodeArr.size() + " hits");

            Map<String, List<String>> materialIdVsNodeIdList = new HashMap<String, List<String>>();
            for (Object[] nextArr : materialIdVsNodeArr) {
                // int8 shows in pgAdminIII.  Converts to BigInteger...
                BigInteger materialIdBI = (BigInteger) nextArr[0];
                String materialIdString = materialIdBI.toString();
                BigInteger nodeIdBI = (BigInteger) nextArr[1];
                String nodeIdString = nodeIdBI.toString();
                List<String> nodeIdList = materialIdVsNodeIdList.get(materialIdString);
                if (nodeIdList == null) {
                    nodeIdList = new ArrayList<String>();
                    materialIdVsNodeIdList.put(materialIdString, nodeIdList);
                }
                nodeIdList.add(nodeIdString);
            }

            // Next relate all the site _accessions_  to their site locations.
            sqlQuery = new StringBuffer();
            sqlQuery.append("select distinct bm.material_id as site_id, cs.region as geographic_location");
            sqlQuery.append(" from camera.bio_material bm inner join camera.collection_site cs on cs.site_id=bm.collection_site_id");
            sqlQuery.append(" where bm.project_symbol='").append(project).append("'");
            query = getSession().createSQLQuery(sqlQuery.toString());
            _log.info(query.toString());
            List<Object[]> siteAccessionVsLocationArr = (List<Object[]>) query.list();
            _log.info("Query resulted in " + siteAccessionVsLocationArr.size() + " hits");

            Map<String, String> siteAccessionVsLocation = new HashMap<String, String>();
            for (Object[] nextArr : siteAccessionVsLocationArr) {
                BigInteger materialBM = (BigInteger) nextArr[0];
                siteAccessionVsLocation.put(materialBM.toString(), (String) nextArr[1]);
            }

            // Finally, produce one map linking the site location to the blast ID.
            Map<String, String> returnMap = new HashMap<String, String>();
            for (String materialId : materialIdVsNodeIdList.keySet()) {
                List<String> nodeIdList = materialIdVsNodeIdList.get(materialId);
                String siteLocation = siteAccessionVsLocation.get(materialId);
                for (String nodeId : nodeIdList) {
                    returnMap.put(nodeId, siteLocation);
                }
            }

            return (returnMap);

        }
        catch (DataAccessResourceFailureException e) {
            _log.error(e);
            throw handleException(e, "BlastDAOImpl - getSiteLocations");
        }
        catch (IllegalStateException e) {
            _log.error(e);
            throw handleException(e, "BlastDAOImpl - getSiteLocations");
        }
        catch (HibernateException e) {
            _log.error(e);
            throw convertHibernateAccessException(e);
        }
    }

    public List<String> getSiteLocations(String project) throws DaoException {
        try {
            StringBuffer sqlQuery = new StringBuffer();
            sqlQuery.append("select distinct cs.region as geographic_location");
            sqlQuery.append(" from camera.bio_material bm inner join camera.collection_site cs on cs.site_id=bm.collection_site_id");
            sqlQuery.append(" where bm.project_symbol='").append(project).append("'");
            sqlQuery.append(" order by cs.region");
            SQLQuery query = getSession().createSQLQuery(sqlQuery.toString());
            _log.info(query.toString());

            // NOTE: if multiple columns, the result would have been a list of Object arrays.
            List results = (List<String>) query.list();
            _log.info("Query resulted in " + results.size() + " hits");
            return (results);
        }
        catch (DataAccessResourceFailureException e) {
            _log.error(e);
            throw handleException(e, "BlastDAOImpl - getSiteLocations");
        }
        catch (IllegalStateException e) {
            _log.error(e);
            throw handleException(e, "BlastDAOImpl - getSiteLocations");
        }
        catch (HibernateException e) {
            _log.error(e);
            throw convertHibernateAccessException(e);
        }
    }

    public BlastTaskVO getPrepopulatedBlastTask(String taskIdString) throws DaoException {
        _log.debug("BlastDAOImpl getPrepopulatedBlastTask() taskId=" + taskIdString);
        Long taskId = new Long(taskIdString.trim());
        try {
            // First get the task
            DetachedCriteria taskCriteria = DetachedCriteria.forClass(BlastTask.class);
            taskCriteria.add(Restrictions.eq("objectId", taskId));
            List taskList = getHibernateTemplate().findByCriteria(taskCriteria);
            if (taskList == null || taskList.size() == 0)
                throw new DaoException("Could not find BlastTask with taskId=" + taskId);
            BlastTask blastTask = (BlastTask) taskList.get(0);
            BlastTask newBlastTask = blastTask.getClass().newInstance();

            // Copy values
            for (String s1 : blastTask.getParameterKeySet()) {
                String value = blastTask.getParameter(s1);
                newBlastTask.setParameter(s1, value);
            }
            newBlastTask.setJobName(blastTask.getJobName());
            BlastTaskVO blastTaskVO = new BlastTaskVO(newBlastTask);

            // Figure out sequence types according to blast type
            if (blastTask instanceof BlastNTask ||
                    blastTask instanceof MegablastTask ||
                    blastTask instanceof TBlastXTask) {
                blastTaskVO.setQueryType(SequenceType.NUCLEOTIDE);
                blastTaskVO.setSubjectType(SequenceType.NUCLEOTIDE);
            }
            else if (blastTask instanceof BlastXTask) {
                blastTaskVO.setQueryType(SequenceType.NUCLEOTIDE);
                blastTaskVO.setSubjectType(SequenceType.PEPTIDE);
            }
            else if (blastTask instanceof TBlastNTask) {
                blastTaskVO.setQueryType(SequenceType.PEPTIDE);
                blastTaskVO.setSubjectType(SequenceType.NUCLEOTIDE);
            }
            else if (blastTask instanceof BlastPTask) {
                blastTaskVO.setQueryType(SequenceType.PEPTIDE);
                blastTaskVO.setSubjectType(SequenceType.PEPTIDE);
            }
            else {
                throw new Exception("Do not recognize for taskId=" + blastTask.getObjectId() + " blastTask type=" + blastTask.getClass().getName());
            }

            // Get the query node and add to map
            DetachedCriteria queryNodeCriteria = DetachedCriteria.forClass(Node.class);
            String queryNodeIdString = blastTask.getParameter(BlastTask.PARAM_query);
            Long queryNodeId = new Long(queryNodeIdString.trim());
            queryNodeCriteria.add(Restrictions.eq("objectId", queryNodeId));
            List queryNodes = getHibernateTemplate().findByCriteria(queryNodeCriteria);
            if (queryNodes == null || queryNodes.size() == 0)
                throw new Exception("Could not find queryNode with objectId=" + queryNodeId);
            Node queryNode = (Node) queryNodes.get(0);
            UserDataNodeVO queryNodeVO = new UserDataNodeVO();
            if (queryNode instanceof FastaFileNode) {
                FastaFileNode queryFileNode = (FastaFileNode) queryNode;
                queryNodeVO.setDatabaseObjectId(queryFileNode.getObjectId() + "");
                queryNodeVO.setDateCreated(TimebasedIdentifierGenerator.getTimestamp(queryFileNode.getObjectId()));
                queryNodeVO.setDataType(queryFileNode.getDataType());
                queryNodeVO.setDescription(queryFileNode.getDescription());
                queryNodeVO.setLength(queryFileNode.getLength() + "");
                queryNodeVO.setNodeName(queryFileNode.getName());
                queryNodeVO.setSequenceCount(queryFileNode.getSequenceCount());
                blastTaskVO.setQueryNodeVO(queryNodeVO);
            }
            else {
                throw new Exception("Could not recognize class of queryNode for id=" + queryNode.getObjectId() + " which is=" + queryNode.getClass().getName());
            }

            // Get and add all subject blastable data nodes to map
            DetachedCriteria subjectNodeCriteria = DetachedCriteria.forClass(Node.class);
            String subjectNodeString = blastTask.getParameter(BlastTask.PARAM_subjectDatabases);
            String[] subjectStrings = subjectNodeString.split(",");
            ArrayList<Long> subjectList = new ArrayList<Long>();
            for (String s : subjectStrings) {
                subjectList.add(new Long(s.trim()));
            }
            subjectNodeCriteria.add(Restrictions.in("objectId", subjectList));
            List subjectNodes = getHibernateTemplate().findByCriteria(subjectNodeCriteria);
            Set<BlastableNodeVO> subjectNodeVOs = new HashSet<BlastableNodeVO>();
            if (subjectNodes == null || subjectNodes.size() == 0)
                throw new Exception("Could not find subject nodes from string list=" + subjectNodeString);
            for (Object o : subjectNodes) {
                if (o instanceof BlastDatabaseFileNode) {
                    BlastDatabaseFileNode subjectNode = (BlastDatabaseFileNode) o;
                    BlastableNodeVO subjectNodeVO = new BlastableNodeVO();
                    subjectNodeVO.setDatabaseObjectId(subjectNode.getObjectId() + "");
                    subjectNodeVO.setDescription(subjectNode.getDescription());
                    subjectNodeVO.setLength(subjectNode.getLength() + "");
                    subjectNodeVO.setNodeName(subjectNode.getName());
                    subjectNodeVO.setSequenceCount(subjectNode.getSequenceCount());
                    subjectNodeVO.setSequenceType(subjectNode.getSequenceType());
                    subjectNodeVOs.add(subjectNodeVO);
                }
                else {
                    throw new Exception("Do not recognize for nodeId=" + ((Node) o).getObjectId() + " type of subject node=" + o.getClass().getName());
                }
            }
            blastTaskVO.setSubjectNodeVOs(subjectNodeVOs);

            _log.debug("Succesfully finished getPrepopulatedBlastTask() returning blastTask object from BlastDAOImpl");
            return blastTaskVO;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e.getMessage());
        }
    }
}
