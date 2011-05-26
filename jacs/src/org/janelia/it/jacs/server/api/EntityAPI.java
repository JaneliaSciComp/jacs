package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.EntityDAOImpl;
import org.janelia.it.jacs.server.utils.SystemException;

import java.util.List;

public class EntityAPI {
    static Logger logger = Logger.getLogger(EntityAPI.class.getName());
    EntityDAOImpl entityDAO;

    public void setEntityDAO(EntityDAOImpl userDAO) {
        this.entityDAO = userDAO;
    }

    /**
     * The method searches the EntityType(s)
     *
     * @param searchString
     * @param startIndex
     * @param numRows
     * @param sortArgs
     * @return
     * @throws org.janelia.it.jacs.server.utils.SystemException
     */
    public List<EntityType> getPagedEntityTypes(String searchString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws SystemException {
        try {
            logger.debug(numRows + " requested");
            int dbStartIndex = startIndex;
            int dbNumRows = numRows;
            SortArgument[] actualSortArgs = null;
            if (sortArgs != null) {
                actualSortArgs = new SortArgument[sortArgs.length];
                for (int i = 0; i < sortArgs.length; i++) {
                    String sortField = sortArgs[i].getSortArgumentName();
                    // todo Remove this hard-wiring;  Strings from the front-end were ALL SCREWED UP (full URLs streamed in)
//                    sortField = UserDataVO.SORT_BY_USER_LOGIN;
//                    if (sortField == null || sortField.length() == 0) {
//                        continue;
//                    }
//                    if (sortField.equals(UserDataVO.SORT_BY_USER_LOGIN)) {
//                        sortField = UserDataVO.SORT_BY_USER_LOGIN;
//                    }
//                    else if (sortField.equals(UserDataVO.SORT_BY_USER_ID)) {
//                        sortField = UserDataVO.SORT_BY_USER_ID;
//                    }
//                    else if (sortField.equals(UserDataVO.SORT_BY_FULLNAME)) {
//                        sortField = UserDataVO.SORT_BY_FULLNAME;
//                    }
//                    else if (sortField.equals(UserDataVO.SORT_BY_EMAIL)) {
//                        sortField = UserDataVO.SORT_BY_EMAIL;
//                    }
//                    else {
//                        // unknown or unsupported sort field name
//                        continue;
//                    }
                    actualSortArgs[i] = new SortArgument(sortField, sortArgs[i].getSortDirection());
                }
            }
            List<EntityType> entityTypeList = entityDAO.getPagedEntityTypes(searchString, dbStartIndex, dbNumRows, actualSortArgs);
            logger.debug("received " + entityTypeList.size() + " rows from entityDAO.getPagedUsers");
            logger.debug("returning list with " + entityTypeList.size() + " entires");
            return entityTypeList;
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }


    public void markEntityTypesForDeletion(String targetEntityType) throws SystemException {
        throw new SystemException("Currently unable to delete Entity Type " + targetEntityType);
    }

    public Integer getNumEntityTypes(String searchString) throws SystemException {
        try {
            return entityDAO.getNumEntityTypes(searchString);
        }
        catch (DaoException e) {
            throw new SystemException(e);
        }
    }

}
