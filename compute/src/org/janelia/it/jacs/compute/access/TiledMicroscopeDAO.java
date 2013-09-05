package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.omg.PortableServer.ID_UNIQUENESS_POLICY_ID;

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
    ComputeDAO computeDAO;

    public final static String TMP_GEO_VALUE="@@@ new geo value string @@@";

    public TiledMicroscopeDAO(Logger logger) {
        super(logger);
        annotationDAO=new AnnotationDAO(logger);
        computeDAO=new ComputeDAO(logger);

    }

    public void createTiledMicroscopeEntityTypes() throws DaoException {
        _logger.debug("createTiledMicroscopeEntityTypes() - TiledMicroscopeDAO layer");

        try {

            _logger.debug("Creating attributes");
            createEntityAttribute(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            createEntityAttribute(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            createEntityAttribute(EntityConstants.ATTRIBUTE_PROPERTY);
            createEntityAttribute(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);

            _logger.debug("Creating Workspace entity");
            Set<String> workspaceAttributeSet = new HashSet<String>();
            workspaceAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
            workspaceAttributeSet.add(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
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

    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityType().getName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Tiled Microscope Workspace must be created with valid 3D Tile Microscope Sample Id");
            }
            Entity workspace=new Entity();
            workspace.setCreationDate(new Date());
            workspace.setUpdatedDate(new Date());
            workspace.setName(name);
            User user = computeDAO.getUserByNameOrKey(ownerKey);
            if (user==null) {
                throw new Exception("Owner Key="+ownerKey+" is not valid");
            }
            workspace.setOwnerKey(ownerKey);
            EntityType tiledMicroscopeWorkspaceType=annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            workspace.setEntityType(tiledMicroscopeWorkspaceType);
            annotationDAO.saveOrUpdate(workspace);
            // create preferences
            TmPreferences preferences=createTiledMicroscopePreferences(workspace.getId());
            Entity parentEntity = annotationDAO.getEntityById(parentId);
            EntityData ed = parentEntity.addChildEntity(workspace);
            annotationDAO.saveOrUpdate(ed);
            annotationDAO.saveOrUpdate(parentEntity);
            // associate brain sample
            EntityData sampleEd = new EntityData();
            sampleEd.setOwnerKey(workspace.getOwnerKey());
            sampleEd.setCreationDate(new Date());
            sampleEd.setUpdatedDate(new Date());
            EntityAttribute sampleAttr = annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
            sampleEd.setEntityAttribute(sampleAttr);
            // need this?
            // sampleEd.setOrderIndex(0);
            sampleEd.setParentEntity(workspace);
            sampleEd.setValue(brainSampleId.toString());
            annotationDAO.saveOrUpdate(sampleEd);
            workspace.getEntityData().add(sampleEd);
            annotationDAO.saveOrUpdate(workspace);
            // back to user
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
            Entity workspace = annotationDAO.getEntityById(workspaceId);
            if (!workspace.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity neuron=new Entity();
            neuron.setCreationDate(new Date());
            neuron.setUpdatedDate(new Date());
            neuron.setName(name);
            neuron.setOwnerKey(workspace.getOwnerKey());
            EntityType neuronType = annotationDAO.getEntityTypeByName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            neuron.setEntityType(neuronType);
            annotationDAO.saveOrUpdate(neuron);
            EntityData ed = workspace.addChildEntity(neuron, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.saveOrUpdate(ed);
            annotationDAO.saveOrUpdate(workspace);
            TmNeuron tmNeuron=new TmNeuron(neuron);
            return tmNeuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected TmPreferences createTiledMicroscopePreferences(Long workspaceId) throws DaoException {
        try {
            Entity workspace = annotationDAO.getEntityById(workspaceId);
            if (!workspace.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity preferences=new Entity();
            preferences.setName("preferences");
            preferences.setCreationDate(new Date());
            preferences.setUpdatedDate(new Date());
            preferences.setOwnerKey(workspace.getOwnerKey());
            EntityType propertyType = annotationDAO.getEntityTypeByName(EntityConstants.TYPE_PROPERTY_SET);
            preferences.setEntityType(propertyType);
            annotationDAO.saveOrUpdate(preferences);
            EntityData ed = workspace.addChildEntity(preferences, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.saveOrUpdate(ed);
            annotationDAO.saveOrUpdate(workspace);
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
            Entity neuron=annotationDAO.getEntityById(neuronId);
            if (!neuron.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type="+neuronId);
            }
            // Check if root; if not, find its parent
            boolean isRoot=false;
            if (parentAnnotationId==null) {
                isRoot=true;
            } else {
                // Validate
                boolean foundParent=false;
                for (EntityData ed : neuron.getEntityData()) {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE) ||
                            ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        String value=ed.getValue();
                        // note: really ought to unify this parsing with the parsing of EntityData in TmNeuron
                        String[] vArr=value.split(":");
                        Long pId=new Long(vArr[0]);
                        if (pId.equals(parentAnnotationId)) {
                            foundParent=true;
                        }
                    }
                }
                if (!foundParent) {
                    throw new Exception("Could not find parent matching parentId="+parentAnnotationId);
                }
            }
            EntityData geoEd=new EntityData();
            geoEd.setOwnerKey(neuron.getOwnerKey());
            geoEd.setCreationDate(new Date());
            geoEd.setUpdatedDate(new Date());
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
            annotationDAO.saveOrUpdate(geoEd);
            neuron.getEntityData().add(geoEd);
            annotationDAO.saveOrUpdate(neuron);
            // Find and update value string
            boolean valueStringUpdated=false;
            String valueString=null;
            for (EntityData ed : neuron.getEntityData()) {
                if (isRoot) {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        if (ed.getValue().equals(TMP_GEO_VALUE)) {
                            valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                            ed.setValue(valueString);
                            annotationDAO.saveOrUpdate(ed);
                            valueStringUpdated=true;
                        }
                    }
                } else {
                    if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                        if (ed.getValue().equals(TMP_GEO_VALUE)) {
                            valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                            ed.setValue(valueString);
                            annotationDAO.saveOrUpdate(ed);
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
            EntityData ed=(EntityData) computeDAO.genericLoad(EntityData.class, geoAnnotation.getId());
            String valueString=TmGeoAnnotation.toStringFromArguments(geoAnnotation.getId(), geoAnnotation.getParentId(),
                    index, x, y, z, comment);
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * reparent a geometric annotation to another one (taking its whole subtree with it);
     * both annotations must be in the input neuron
     *
     * @param annotation
     * @param newParentAnnotationID
     * @param neuron
     * @throws DaoException
     */
    public void reparentGeometricAnnotation(TmGeoAnnotation annotation, Long newParentAnnotationID,
        TmNeuron neuron) throws DaoException {

        // verify that both annotations are in the input neuron
        if (!neuron.getGeoAnnotationMap().containsKey(annotation.getId())) {
            throw new DaoException("input neuron doesn't contain annotation " + annotation.getId());
        }
        if (!neuron.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new DaoException("input neuron doesn't contain annotation " + newParentAnnotationID);
        }

        // if annotation is a root annotation, change its attribute and save
        EntityData ed;
        try {
            ed=(EntityData) computeDAO.genericLoad(EntityData.class, annotation.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

        // if the annotation is a root annotation, change its attribute:
        if (ed.getEntityAttribute().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
            ed.setEntityAttribute(annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE));
        }

        // change the parent ID and save
        String valueString=TmGeoAnnotation.toStringFromArguments(annotation.getId(), newParentAnnotationID,
                annotation.getIndex(), annotation.getX(), annotation.getY(), annotation.getZ(),
                annotation.getComment());
        ed.setValue(valueString);
        annotationDAO.saveOrUpdate(ed);

    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
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
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
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
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
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
                        computeDAO.genericDelete(ed);
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
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            if (!workspaceEntity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            EntityAttribute propertyAttribute=annotationDAO.getEntityAttributeByName(EntityConstants.ATTRIBUTE_PROPERTY);
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
                        EntityData ed=new EntityData(null, propertyAttribute, e, null, e.getOwnerKey(), key+"="+value, new Date(), null, 0);
                        annotationDAO.genericSave(ed);
                        e.getEntityData().add(ed);
                    } else {
                        edToUpdate.setValue(key+"="+value);
                        annotationDAO.genericSave(edToUpdate);
                    }
                    annotationDAO.genericSave(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected void generalTreeDelete(String ownerKey, Long entityId, String type) throws DaoException {
        try {
            Entity entity = annotationDAO.getEntityById(entityId);
            if (!entity.getEntityType().getName().equals(type)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            if (entity.getOwnerKey().equals(ownerKey)) {
                annotationDAO.deleteEntityTree(ownerKey, entity);
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
            EntityData ed=(EntityData) annotationDAO.genericLoad(EntityData.class, geoId);
            annotationDAO.genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws DaoException {
        try {
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            TmWorkspace workspace=new TmWorkspace(workspaceEntity);
            return workspace;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmNeuron loadNeuron(Long neuronId) throws DaoException {
        try {
            Entity neuronEntity = annotationDAO.getEntityById(neuronId);
            TmNeuron neuron=new TmNeuron(neuronEntity);
            return neuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }


}
