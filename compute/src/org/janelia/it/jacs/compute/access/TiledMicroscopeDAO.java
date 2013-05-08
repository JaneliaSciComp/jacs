package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */

public class TiledMicroscopeDAO extends ComputeBaseDAO {

    AnnotationDAO annotationDAO;
    public final static String TMP_GEO_VALUE="@@@ new geo value string @@@";

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
            brainSampleEntity.addChildEntity(workspace);
            EJBFactory.getLocalEntityBean().saveOrUpdateEntity(brainSampleEntity);
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
            workspace.addChildEntity(neuron, EntityConstants.ATTRIBUTE_ENTITY);
            EJBFactory.getLocalEntityBean().saveOrUpdateEntity(workspace);
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

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws DaoException {
        try {
            // Retrieve neuron
            Entity neuron=EJBFactory.getLocalEntityBean().getEntityById(neuronId);
            if (!neuron.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type="+neuronId);
            }
            // Check if root
            boolean isRoot=false;
            if (parentAnnotationId==null) {
                boolean neuronAlreadyHasRoot=false;
                // Assume this is root - check for other roots
                for (EntityData ed : neuron.getEntityData()) {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        neuronAlreadyHasRoot=true;
                    }
                }
                if (neuronAlreadyHasRoot) {
                    throw new Exception("Neuron already has root element - two are not permitted");
                }
                isRoot=true;
            } else {
                // Validate
                boolean foundParent=false;
                for (EntityData ed : neuron.getEntityData()) {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                        String value=ed.getValue();
                        String[] vArr=value.split(":");
                        Long pId=new Long(vArr[0]);
                        if (pId==parentAnnotationId) {
                            foundParent=true;
                        }
                    }
                }
                if (!foundParent) {
                    throw new Exception("Could not find parent matching parendId="+parentAnnotationId);
                }
            }
            EntityData geoEd=new EntityData();
            geoEd.setOwnerKey(neuron.getOwnerKey());
            geoEd.setCreationDate(new Date());
            EntityAttribute geoAttr;
            Long parentId=0L;
            if (isRoot) {
                parentId = neuron.getId();
                geoAttr=annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            } else {
                parentId=parentAnnotationId;
                geoAttr=annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            }
            geoEd.setEntityAttribute(geoAttr);
            geoEd.setOrderIndex(0);
            geoEd.setParentEntity(neuron);
            geoEd.setValue(TMP_GEO_VALUE);
            neuron.getEntityData().add(geoEd);
            neuron=EJBFactory.getLocalEntityBean().saveOrUpdateEntity(neuron);
            // Find and update value string
            boolean valueStringUpdated=false;
            String valueString=null;
            for (EntityData ed : neuron.getEntityData()) {
                if (isRoot) {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                        ed.setValue(valueString);
                        EJBFactory.getLocalEntityBean().saveOrUpdateEntityData(ed);
                        valueStringUpdated=true;
                    }
                } else {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                        if (ed.getValue().equals(TMP_GEO_VALUE)) {
                            valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                            ed.setValue(valueString);
                            EJBFactory.getLocalEntityBean().saveOrUpdateEntityData(ed);
                            valueStringUpdated=true;
                        }
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find geo entry to update for value string");
            }
            TmGeoAnnotation geoAnnotation=new TmGeoAnnotation(valueString);
            return geoAnnotation;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                       int index, double x, double y, double z, String comment) throws DaoException {
        try {
            EntityData ed=(EntityData) EJBFactory.getLocalComputeBean().genericLoad(EntityData.class, geoAnnotation.getId());
            String valueString=TmGeoAnnotation.toStringFromArguments(geoAnnotation.getId(), geoAnnotation.getParentId(),
                    index, x, y, z, comment);
            ed.setValue(valueString);
            EJBFactory.getLocalComputeBean().genericSave(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = EJBFactory.getLocalEntityBean().getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Workspaces must be parented with valid 3D Tile Microscope Sample Id");
            }
            List<TmWorkspaceDescriptor> descriptorList=new ArrayList<TmWorkspaceDescriptor>();
            EntityType tiledMicroscopeWorkspaceType=annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            EntityType neuronType=annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            for (Entity possibleWorkspace : brainSampleEntity.getChildren()) {
                if (possibleWorkspace.getEntityType().getName().equals(tiledMicroscopeWorkspaceType.getName())) {
                    if (possibleWorkspace.getOwnerKey().equals(ownerKey)) {
                        Long wId=possibleWorkspace.getId();
                        String wName=possibleWorkspace.getName();
                        int neuronCount=0;
                        for (EntityData ed : possibleWorkspace.getEntityData()) {
                            String edName=ed.getEntityAttribute().getName();
                            if (edName.equals(neuronType)) {
                                neuronCount++;
                            }
                        }
                        TmWorkspaceDescriptor descriptor=new TmWorkspaceDescriptor(wId, wName, neuronCount);
                        descriptorList.add(descriptor);
                    }
                }
            }
            Collections.sort(descriptorList, new Comparator<TmWorkspaceDescriptor>() { @Override public int compare(TmWorkspaceDescriptor a,
                    TmWorkspaceDescriptor b) { if (a.getId() < b.getId()) { return 1; } else { return 0; } } });
            return descriptorList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity workspaceEntity = EJBFactory.getLocalEntityBean().getEntityById(workspaceId);
            if (!workspaceEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            List<TmNeuronDescriptor> descriptorList=new ArrayList<TmNeuronDescriptor>();
            EntityType neuronType=annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            EntityAttribute geoRoot=annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            EntityAttribute geoTree=annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            for (Entity possibleNeuron : workspaceEntity.getChildren()) {
                if (possibleNeuron.getEntityType().getName().equals(neuronType.getName())) {
                    if (possibleNeuron.getOwnerKey().equals(ownerKey)) {
                        Long nId=possibleNeuron.getId();
                        String nName=possibleNeuron.getName();
                        int annoCount=0;
                        for (EntityData ed : possibleNeuron.getEntityData()) {
                            String edName=ed.getEntityAttribute().getName();
                            if (edName.equals(geoRoot) || edName.equals(geoTree)) {
                                annoCount++;
                            }
                        }
                        TmNeuronDescriptor descriptor=new TmNeuronDescriptor(nId, nName, annoCount);
                        descriptorList.add(descriptor);
                    }
                }
            }
            Collections.sort(descriptorList, new Comparator<TmNeuronDescriptor>() { @Override public int compare(TmNeuronDescriptor a,
                  TmNeuronDescriptor b) { if (a.getId() < b.getId()) { return 1; } else { return 0; } } });
            return descriptorList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void removeWorkspacePreference(Long workspaceId, String key) throws DaoException {
        try {
            Entity workspaceEntity = EJBFactory.getLocalEntityBean().getEntityById(workspaceId);
            if (!workspaceEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityType().getName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                    Set<EntityData> edToRemove=new HashSet<EntityData>();
                    for (EntityData ed : e.getEntityData()) {
                        String pString=ed.getValue();
                        String[] pArr=pString.split("=");
                        String pKey=pArr[0];
                        if (pKey.equals(key)) {
                            edToRemove.add(ed);
                        }
                    }
                    for (EntityData ed : edToRemove) {
                        EJBFactory.getLocalEntityBean().deleteEntityData(ed);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws DaoException {
        try {
            Entity workspaceEntity = EJBFactory.getLocalEntityBean().getEntityById(workspaceId);
            if (!workspaceEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            EntityAttribute propertyAttribute=EJBFactory.getLocalEntityBean().getEntityAttributeByName(EntityConstants.ATTRIBUTE_PROPERTY);
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityType().getName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                    EntityData edToUpdate=null;
                    for (EntityData ed : e.getEntityData()) {
                        String pString=ed.getValue();
                        String[] pArr=pString.split("=");
                        String pKey=pArr[0];
                        if (pKey.equals(key)) {
                            edToUpdate=ed;
                        }
                    }
                    if (edToUpdate==null) {

                    //public EntityData(Long id, EntityAttribute entityAttribute, Entity parentEntity, Entity childEntity, String ownerKey,
//                            String value, Date creationDate, Date updatedDate, Integer orderIndex) {


                        EntityData ed=new EntityData(null, propertyAttribute, e, null, e.getOwnerKey(), key+"="+value, new Date(), null, 0);
                        e.getEntityData().add(ed);
                    } else {
                        edToUpdate.setValue(key+"="+value);
                    }
                    EJBFactory.getLocalEntityBean().saveOrUpdateEntity(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected void generalTreeDelete(String ownerKey, Long entityId, String type) throws DaoException {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(entityId);
            if (!entity.getEntityType().getName().equals(type)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            if (entity.getOwnerKey().equals(ownerKey)) {
                EJBFactory.getLocalEntityBean().deleteEntityTree(ownerKey, entityId);
            } else {
                throw new Exception("Owners do not match");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void deleteNeuron(String ownerKey, Long neuronId) throws DaoException {
        generalTreeDelete(ownerKey, neuronId, EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
    }

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws DaoException {
        generalTreeDelete(ownerKey, workspaceId, EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
    }

    public void deleteGeometricAnnotation(Long geoId) throws DaoException {
        try {
            EntityData ed=(EntityData) EJBFactory.getLocalComputeBean().genericLoad(EntityData.class, geoId);
            EJBFactory.getLocalComputeBean().genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }


}
