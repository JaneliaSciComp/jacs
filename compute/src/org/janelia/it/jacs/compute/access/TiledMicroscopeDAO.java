package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmPreferences;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

import java.util.Date;
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

    AnnotationDAO annotationDAO;

    public TiledMicroscopeDAO(Logger logger) {
        super(logger);
        annotationDAO=new AnnotationDAO(logger);
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

        _logger.debug("createTiledMicroscopeEntityTypes() - done");
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long brainSampleId, String name, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = EJBFactory.getLocalEntityBean().getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Tiled Microscope Workspace must be created with valid 3D Tile Microscope Sample Id");
            }
            Entity workspace=new Entity();
            workspace.setCreationDate(new Date());
            workspace.setName(name);
            User user = EJBFactory.getLocalComputeBean().getUserByNameOrKey(ownerKey);
            if (user==null) {
                throw new Exception("Owner Key="+ownerKey+" is not valid");
            }
            workspace.setOwnerKey(ownerKey);
            EntityType tiledMicroscopeWorkspaceType=annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            workspace.setEntityType(tiledMicroscopeWorkspaceType);
            workspace=EJBFactory.getLocalEntityBean().saveOrUpdateEntity(workspace);
            TmPreferences preferences=createTiledMicroscopePreferences(workspace.getId());
            TmWorkspace tmWorkspace=new TmWorkspace(workspace);
            tmWorkspace.setPreferences(preferences);
            return tmWorkspace;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws DaoException {
        try {
            Entity workspace = EJBFactory.getLocalEntityBean().getEntityById(workspaceId);
            if (!workspace.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity neuron=new Entity();
            neuron.setCreationDate(new Date());
            neuron.setName(name);
            neuron.setOwnerKey(workspace.getOwnerKey());
            EntityType neuronType = annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            neuron.setEntityType(neuronType);
            neuron=EJBFactory.getLocalEntityBean().saveOrUpdateEntity(neuron);
            TmNeuron tmNeuron=new TmNeuron(neuron);
            return tmNeuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected TmPreferences createTiledMicroscopePreferences(Long workspaceId) throws DaoException {
        try {
            Entity workspace = EJBFactory.getLocalEntityBean().getEntityById(workspaceId);
            if (!workspace.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity preferences=new Entity();
            preferences.setCreationDate(new Date());
            preferences.setOwnerKey(workspace.getOwnerKey());
            EntityType propertyType = annotationDAO.getEntityTypeByName(EntityConstants.TYPE_PROPERTY_SET);
            preferences.setEntityType(propertyType);
            preferences=EJBFactory.getLocalEntityBean().saveOrUpdateEntity(preferences);
            workspace.addChildEntity(preferences, EntityConstants.ATTRIBUTE_ENTITY);
            EJBFactory.getLocalEntityBean().saveOrUpdateEntity(workspace);
            TmPreferences tmPreferences=new TmPreferences(preferences);
            return tmPreferences;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }



}
