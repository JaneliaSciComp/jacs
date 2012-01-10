
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 19, 2006
 * Time: 3:22:08 PM
 */
public interface FileNodeDAO extends DAO {
    void saveOrUpdateFileNode(FileNode targetNode) throws DataAccessException, DaoException;

    FileNode getFileNodeById(Long fileNodeId) throws DataAccessException, DaoException;

    FileNode[] getResultFileNodesByTaskId(Long taskId) throws DataAccessException, DaoException;
}
