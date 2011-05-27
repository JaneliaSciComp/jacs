package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.List;

public interface EntityDAO extends DAO {
    EntityType createEntityType(Long id, Long sequence, String name, String style, String description, String iconurl) throws DaoException;
    EntityType getEntityTypeByName(String targetEntityType) throws DaoException;

    List<EntityType> findAllEntityTypes() throws DataAccessException, DaoException;

    void saveOrUpdateEntityType(EntityType targetEntityType) throws DataAccessException, DaoException;
    long countAllEntityTypes() throws DataAccessException, DaoException;
}
