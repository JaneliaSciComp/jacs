
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Dec 6, 2006
 * Time: 2:58:48 PM
 */
public interface NodeDAO extends DAO {
    List getUserSpecificData(String targetUser)
            throws DataAccessException, DaoException;

    BlastableNodeVO[] getBlastableDatabaseList(String sequenceType, String targetUser)
            throws DataAccessException, DaoException;

    BlastableNodeVO getBlastableDatabaseByNodeId(String targetUser, String nodeId)
            throws DataAccessException, DaoException;

    List getNodeNames(List nodeIds)
            throws DataAccessException, DaoException;

    List<Node> getNodesByIds(List<Long> nodeIds)
            throws DataAccessException, DaoException;

    Node getNodeById(Long Id)
            throws DataAccessException, DaoException;

    Node getNodeByName(String name)
            throws DataAccessException, DaoException;

    BlastResultNode getBlastResultNodeByTaskId(Long taskId)
            throws DataAccessException, DaoException;

    BlastResultNode getBlastResultNodeByNodeId(Long NodeId)
            throws DataAccessException, DaoException;

    List<Node> getTaskOutputNodes(Long taskId, Class clazz)
            throws DaoException;

    int getNumBlastableSubjectNodes(String[] userLogins)
            throws DaoException;

    List<Node> getPagedBlastableSubjectNodes(String[] userLogins,
                                             String sequenceType,
                                             int startIndex,
                                             int numRows,
                                             SortArgument[] sortArgs)
            throws DaoException;

    FastaFileNode getFastaFileNode(Long nodeId)
            throws DataAccessException, DaoException;

//    Map<BioMaterial, Integer> getSitesForBlastResultNode(Long taskId)
//            throws DataAccessException, DaoException;
//
    String replaceNodeName(String nodeId, String nodeName)
            throws DataAccessException, DaoException;

    void markNodeForDeletion(String nodeId)
            throws DataAccessException, DaoException;

    public int getNumBlastableNodesForUser(String searchString, String sequenceType, String user)
            throws DataAccessException, DaoException;

    List<Node> getPagedBlastableNodesForUser(String searchString, String sequenceType, int startIndex, int numRows,
                                             SortArgument[] sortArgs, String user)
            throws DataAccessException, DaoException;

    List<Node> getPagedBlastableSubjectNodes(String[] userLogins, int startIndex, int numRows)
            throws DaoException;

    List<Object[]> getPagedBlastHitsByTaskId(Long taskId,
                                             int startIndex,
                                             int numRows,
                                             boolean includeHSPRanking,
                                             SortArgument[] sortArgs)
            throws DataAccessException, DaoException;

    Long getNumBlastHitsForNode(Node node)
            throws DataAccessException, DaoException;

//    BlastableNodeVO[] getReversePsiBlastDatasets() throws DaoException;

    List<Event> getAllEventsRelatedToData(Node targetNode, String commonParameterKey, String commonParameterValue,
                                          String taskSubclassLikeString);
}
