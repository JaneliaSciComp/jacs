package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */

public class TiledMicroscopeDAO extends ComputeBaseDAO {

    public TiledMicroscopeDAO(Logger logger) {
        super(logger);
    }

    public void createTiledMicroscopeEntityTypes() throws DaoException {
        _logger.debug("createTiledMicroscopeEntityTypes() - TiledMicroscopeDAO layer");

        try {

            _logger.debug("Creating attributes");
            createEntityAttribute(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            createEntityAttribute(EntityConstants.ATTRIBUTE_PROPERTY);

            _logger.debug("Creating Workspace entity");
            Set<String> workspaceAttributeSet = new HashSet<String>();
            workspaceAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
            createEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE, workspaceAttributeSet);

            _logger.debug("Creating Neuron entity");
            Set<String> neuronAttributeSet = new HashSet<String>();
            neuronAttributeSet.add(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            createEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, neuronAttributeSet);

            _logger.debug("Creating PropertySet entity");
            Set<String> propertiesAttributeSet = new HashSet<String>();
            propertiesAttributeSet.add(EntityConstants.ATTRIBUTE_PROPERTY);
            createEntityType(EntityConstants.TYPE_PROPERTY_SET, propertiesAttributeSet);

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

        _logger.debug("creteTiledMicroscopeEntityTypes() - done");
    }

}
