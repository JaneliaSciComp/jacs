package org.janelia.it.jacs.compute.access;

import java.util.List;

import javax.persistence.EntityManager;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatasetNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates Node access DB operations.
 *
 * @author Sean Murphy
 */
public class NodeDAO extends AbstractBaseDAO {
    private static Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    private TaskDAO taskDao;

    public NodeDAO(EntityManager entityManager, TaskDAO taskDao) {
        super(entityManager);
        this.taskDao = taskDao;
    }

    public BlastDatabaseFileNode getBlastDatabaseFileNodeById(Long fileNodeId) {
        LOG.trace("getBlastDatabaseFileNodeById(fileNodeId={})", fileNodeId);
        return findByNumericId(fileNodeId, BlastDatabaseFileNode.class);
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
        LOG.trace("getBlastDatasetNodeById(nodeId={}, fetchFileNodes={})", nodeId, fetchFileNodes);

        BlastDatasetNode blastDatasetNode = null;
        Node node = findByNumericId(nodeId, Node.class);
        if (node instanceof BlastDatasetNode) {
            blastDatasetNode = (BlastDatasetNode) node;
            if (fetchFileNodes) {
                // force a read of the aggregated file nodes
                blastDatasetNode.getBlastDatabaseFileNodes().size();
            }
        } else if (node instanceof BlastDatabaseFileNode) {
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
        return blastDatasetNode;
    }

    public Read getReadByBseEntityId(Long bseEntityId) {
        LOG.trace("getReadByBseEntityId(bseEntityId={})", bseEntityId);
        return findFirstUsingNamedQuery("findReadByBseEntityId", ImmutableMap.<String, Object>of("entityId", bseEntityId), Read.class);
    }

    public BlastResultNode getBlastHitResultDataNodeByTaskId(Long taskId) {
        LOG.trace("getBlastHitResultDataNodeByTaskId(taskId="+taskId+")");
        return findFirstUsingNamedQuery("findBlastResultNodeByTaskId", ImmutableMap.<String, Object>of("taskId", taskId), BlastResultNode.class);
    }

    public Long getBlastHitCountByTaskId(Long taskId) throws DaoException {
        LOG.trace("getBlastHitCountByTaskId(taskId={})", taskId);
        return getSingleResultUsingNamedQuery("findBlastHitCountByTaskId", ImmutableMap.<String, Object>of("taskId", taskId), Long.class);
    }

    public BlastResultFileNode getBlastResultFileNodeByTaskId(Long taskId) throws DaoException {
        LOG.trace("getBlastResultFileNodeByTaskId(taskId={})", taskId);
        return findFirstUsingNamedQuery("findBlastResultFileNodeByTaskId", ImmutableMap.<String, Object>of("taskId", taskId), BlastResultFileNode.class);
    }

    public List<Node> getNodeByName(String nodeName) throws DaoException {
        LOG.trace("getNodeByName(nodeName={})", nodeName);

        String hql = "select n from Node n where n.name=:nodeName";
        return findByQueryParams(hql, ImmutableMap.<String, Object>of("nodeName", nodeName), Node.class);
    }

    public List<Node> getNodeByPathOverride(String pathOverride) throws DaoException {
        LOG.trace("getNodeByPathOverride(pathOverride={})", pathOverride);

        String hql = "select n from Node n where n.pathOverride = :path";
        return findByQueryParams(hql, ImmutableMap.<String, Object>of("path", pathOverride), Node.class);
    }

}