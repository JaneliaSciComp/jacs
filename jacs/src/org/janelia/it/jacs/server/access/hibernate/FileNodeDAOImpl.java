
package org.janelia.it.jacs.server.access.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.server.access.FileNodeDAO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 19, 2006
 * Time: 3:21:18 PM
 */
public class FileNodeDAOImpl extends DaoBaseImpl implements FileNodeDAO {

    // DAO's can only come from Spring's Hibernate
    private FileNodeDAOImpl() {
    }

    public void saveOrUpdateFileNode(FileNode targetNode) throws DataAccessException, DaoException {
        saveOrUpdateObject(targetNode, "FileNodeDAOImpl - saveOrUpdateFileNode");
    }

    public FileNode getFileNodeById(Long fileNodeId) throws DataAccessException, DaoException {
        try {
            return (FileNode) getHibernateTemplate().get(FileNode.class, fileNodeId);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FileNodeDAOImpl - getFileNodeById");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FileNodeDAOImpl - getFileNodeById");
        }
    }

    public FileNode[] getResultFileNodesByTaskId(Long taskId) throws DataAccessException, DaoException {
        try {
            Criteria criteria = getSession().createCriteria(FileNode.class).
                    createCriteria("task").add(Expression.eq("objectId", taskId));
            List fnl = criteria.list();
//            List fnl = getHibernateTemplate().findByCriteria(criteria);
            logger.info("Criteria=" + criteria.toString());
            return (FileNode[]) fnl.toArray(new FileNode[fnl.size()]);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "FileNodeDAOImpl - getResultFileNodesByTaskId");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "FileNodeDAOImpl - getResultFileNodesByTaskId");
        }
    }

}
