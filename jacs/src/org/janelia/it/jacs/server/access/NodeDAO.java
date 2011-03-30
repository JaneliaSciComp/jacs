/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

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

    Map<BioMaterial, Integer> getSitesForBlastResultNode(Long taskId)
            throws DataAccessException, DaoException;

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

    BlastableNodeVO[] getReversePsiBlastDatasets() throws DaoException;

    List<Event> getAllEventsRelatedToData(Node targetNode, String commonParameterKey, String commonParameterValue,
                                          String taskSubclassLikeString);
}
