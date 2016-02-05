/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.api;

import Jama.Matrix;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPoint;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPointCollection;
import org.janelia.it.jacs.compute.annotation.to.NeuronBean;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmVersionedNeuronCollection;
import static org.janelia.it.jacs.model.user_data.tiledMicroscope.TmVersionedNeuronCollection.*;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.jacs.model.util.MatrixUtilities;
import org.janelia.it.jacs.shared.utils.StringUtils;
import sun.misc.BASE64Encoder;

/**
 * Delegated from Neuron API, to carry out the heaving lifting.
 *
 * @author fosterl
 */
public class AnnotationCollector {
    private Logger log = Logger.getLogger(AnnotationCollector.class);
    private final TmProtobufExchanger exchanger = new TmProtobufExchanger();
    private static final Long MINIMUM_GUID = 1000000000000000000L;
    
    private IdSource idSource = new IdSource();
    
    public AnnotationCollector() {
    }
    
    public void addNeuronImpl(
            NeuronBean neuron
    ) throws Exception {
        // Any 'GUID' for a point, which is smaller than this, will be mapped to
        // a newly 'generated' GUID for internal storage.  Apply this to points.
        Map<Long, Long> extIdToGUID = new HashMap<>();
        // Need to establish the containing of this neuron.
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        Entity collectionEntity = entityBean.getEntityById(DEFAULT_OWNER_KEY, neuron.collectionGUID);
        if (collectionEntity == null) {
            throw new Exception("Sorry, you must first establish the collection.  " + neuron.collectionGUID + " unknown.");
        }
        
        // Next, must construct the neuron.
        TmNeuron tmNeuron = new TmNeuron();
        // Not setting any ID on neuron.
        tmNeuron.setWorkspaceId(collectionEntity.getId());
        tmNeuron.setOwnerKey(DEFAULT_OWNER_KEY);
        tmNeuron.setCreationDate(new Date());
        tmNeuron.setName(neuron.name);
                        
        // Need info from sample, to properly convert the points.
        Entity sampleEntity = entityBean.getEntityById(neuron.sampleID);
        entityBean.loadLazyEntity(sampleEntity, true);
        Matrix micronToVoxMatrix = null;
        // Externally, we have microns.  Internally, we store as voxels.
        for (EntityData sed: sampleEntity.getEntityData()) {
            if (sed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX)) {
                String voxelMatrixStr = sed.getValue();
                micronToVoxMatrix = MatrixUtilities.deserializeMatrix(voxelMatrixStr, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
                break;
            }
        }
        if (micronToVoxMatrix == null) {
            throw new Exception("Failed to find conversion matrix from sample " + neuron.sampleID);
        }
        
        for (AnnotationPoint point: neuron.points) {
            // Convert to internal representation.
            Matrix pointAsMicron = new Matrix( 
                new double[] {
                    point.x,
                    point.y,
                    point.z,
                    1.0
                }, 4 
            );
            Matrix pointAsVoxel = micronToVoxMatrix.times( pointAsMicron );
            
            TmGeoAnnotation annotation = buildTmGeoAnnotation(
                    exchangeGUID(point.pointGUID, extIdToGUID), point.neuronGUID, exchangeGUID(point.parentPointGUID, extIdToGUID), 
                    pointAsVoxel.get(0, 0), pointAsVoxel.get(1, 0), pointAsVoxel.get(2, 0), point.radius
            );
            if (point.parentPointGUID == null  ||  point.parentPointGUID == -1) {
                // This is a root.
                tmNeuron.addRootAnnotation(annotation);
            }
            else {
                // Not a root.
                TmGeoAnnotation parent = tmNeuron.getGeoAnnotationMap().get(exchangeGUID(point.parentPointGUID, extIdToGUID));
                if (parent != null) {
                    parent.addChild(annotation);
                }
            }
            tmNeuron.getGeoAnnotationMap().put(exchangeGUID(point.pointGUID, extIdToGUID), annotation);
        }
        
        tmNeuron = pushNeuron(tmNeuron);

        // Now, we need to add any note associated with the neuron, to the collection.
        String neuronName = neuron.name;
        if (! StringUtils.isEmpty(neuron.tag)) {
            collectionEntity = entityBean.getEntityAndChildren(DEFAULT_OWNER_KEY, collectionEntity.getId());
            Entity propertySetEntity = getPropertySetEntity(collectionEntity);
            propertySetEntity.setValueByAttributeName(
                    EntityConstants.ATTRIBUTE_PROPERTY, neuronName + "=" + neuron.tag
            );
            entityBean.saveOrUpdateEntity(propertySetEntity);
        }

    }

    /** @deprecated */
    public void addPointImpl(
            Long pointGUID, Long collectionGUID, Long neuronGUID,
            int x, int y, int z,
            Double radius,
            int structureID,
            Long parentPointGUID) throws Exception {        
        TmNeuron tmNeuron = fetchNeuron(collectionGUID, neuronGUID); // Get neuron from DB.
        if (tmNeuron == null) {
            log.info("Creating neuron " + neuronGUID);
            tmNeuron = createNeuron(neuronGUID, collectionGUID);
        }
        TmGeoAnnotation annotation = buildTmGeoAnnotation(pointGUID, neuronGUID, parentPointGUID, x, y, z, radius);
        if (parentPointGUID == null  ||  parentPointGUID == -1) {
            // This is a root.
            log.info("Adding annotation as root.");
            tmNeuron.addRootAnnotation(annotation);
        }
        else {
            // Not a root.
            log.info("Adding annotation as sub.");
            TmGeoAnnotation parent = tmNeuron.getGeoAnnotationMap().get(parentPointGUID);
            if (parent != null) {
                parent.addChild(annotation);
            }
        }
        tmNeuron.getGeoAnnotationMap().put(pointGUID, annotation);
        
        // Push neuron to DB.
        log.info("Pushing neuron to DB.");
        tmNeuron = pushNeuron(tmNeuron); // Saving ref as reminder: object changes.
    }

    public void removeNeuronImpl(Long neuronGUID) throws Exception {
        log.info("Removed neuron " + neuronGUID);
        // Ensure this is in the database, and eliminate it.
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        entityBean.deleteEntityData(TmVersionedNeuronCollection.DEFAULT_OWNER_KEY, neuronGUID);
    }

    public void removeCollectionImpl(Long collectionGUID) throws Exception {
        log.info("Removed collection " + collectionGUID);
        // Ensure this is in the db and then eliminate it.
        //todo
    }

    public void addCollectionImpl(AnnotationPointCollection collection) throws Exception {
        if (collection.sampleID == -1  ||  collection.guid == null  || collection.name == null) {
            throw new Exception("Collection ID ("+collection.guid+"), Sample ID ("+collection.sampleID+"), name ("+collection.name+") required.");
        }
        
        // NOTE: must first check whether the sample ID exists.  If not, fail.
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        Entity sampleEntity = entityBean.getEntityById(DEFAULT_OWNER_KEY, collection.sampleID);
        if (sampleEntity != null  &&  sampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // Must create a new collection object, for the neurons.  That will
            // correspond to a workspace.  Add this to the designated folder.            
            List<Entity> parentFolderEntities =
                    entityBean.getEntitiesByNameAndTypeName(DEFAULT_OWNER_KEY, COLLECTIONS_FOLDER_NAME, EntityConstants.TYPE_FOLDER);
            if (parentFolderEntities != null  &&  parentFolderEntities.size() > 1) {
                log.warn("Multiple folders called " + COLLECTIONS_FOLDER_NAME + " found for user " + DEFAULT_OWNER_KEY + ".  Possible overuse of this folder name.");
            }

            // Does the parent folder exist?  If not, create it.
            Entity parentFolderEntity = null;
            Entity defaultWorkspaceEntity = entityBean.getDefaultWorkspace(DEFAULT_OWNER_KEY);
            defaultWorkspaceEntity = entityBean.getEntityAndChildren(DEFAULT_OWNER_KEY, defaultWorkspaceEntity.getId());            
            for ( Entity child: defaultWorkspaceEntity.getChildren() ) {
                if (child.getName().equals(COLLECTIONS_FOLDER_NAME)  &&  child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                    parentFolderEntity = child;
                    break;
                }
            }
            
            if (parentFolderEntity == null) {
                // Must make it, and it must belong to the owner, and it must be under the correct folder for the owner.
                parentFolderEntity = entityBean.createFolderInDefaultWorkspace(DEFAULT_OWNER_KEY, COLLECTIONS_FOLDER_NAME).getChildEntity();                
            }
            log.debug("Found or created parent folder.");
            
            // Create new collections object.
            //  It gets all metadata from the incoming collection object.
            Entity collectionEntity = new Entity();
            collectionEntity.setId(collection.guid);
            collectionEntity.setName(collection.name);
            collectionEntity.setEntityTypeName(EntityConstants.TYPE_ANNOTATION_COLLECTION);
            collectionEntity.setCreationDate(new Date());
            collectionEntity.setOwnerKey(DEFAULT_OWNER_KEY);
            collectionEntity.setEntityData(new HashSet<EntityData>());
            collectionEntity = entityBean.saveBulkEntityTree(collectionEntity);
            //collectionEntity = entityBean.saveOrUpdateEntity(collectionEntity); // No owner key.  
            entityBean.loadLazyEntity(collectionEntity, true);
            log.debug("Created collection entity. " + collectionEntity.getId());
            
            if (! StringUtils.isEmpty(collection.brain)) {
                collectionEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLLECTION_BRAIN_NAME, collection.brain);
            }
            if (! StringUtils.isEmpty(collection.notes)) {
                collectionEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLLECTION_NOTES, collection.notes);
            }
            collectionEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLLECTION_VERSION, collection.versionNumber + "_" + collectionEntity.getCreationDate().getTime());
            collectionEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLLECTION_SAMPLE_ID, sampleEntity.getId().toString());
            entityBean.saveOrUpdateEntity(collectionEntity);
            log.debug("Added all attributes: brain, notes, version, sample id.");
            
            // Need to establish parent/child relationship between folder and collection.
            parentFolderEntity = entityBean.getEntityById(parentFolderEntity.getId());
            EntityData ed = parentFolderEntity.addChildEntity(collectionEntity);
            log.debug("Added collection to parent folder.");
            entityBean.saveOrUpdateEntityData(ed);
            log.debug("Set parent/child relationship.");

            // Establish a property set under the collection.
            getPropertySetEntity(collectionEntity);
            log.debug("Established props under the new collection entity.");
        }
        else {
            throw new Exception("No such sample " + collection.sampleID + ", cannot create collection from " + sampleEntity);
        }
        
    }

    /**
     * Updates the sample id associated with the collection.
     * 
     * @param sampleID set this onto the collection.
     * @param collectionGUID this collection's sample id changes.
     * @throws Exception if invalid inputs given.
     */
    public void setCollectionSampleIDImpl(Long sampleID, Long collectionGUID) throws Exception {
        if (sampleID == null || collectionGUID == null) {
            throw new IllegalArgumentException("Must include both sampleID ("+sampleID+") and collectionGUID ("+collectionGUID+").");
            
        }
        log.info("Setting the sampleID to " + sampleID + " for collection " + collectionGUID);
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        Entity sampleEntity = entityBean.getEntityById(DEFAULT_OWNER_KEY, sampleID);
        if (sampleEntity != null) {
            Entity collectionEntity = entityBean.getEntityById(DEFAULT_OWNER_KEY, sampleID);
            if (collectionEntity != null) {
                collectionEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COLLECTION_SAMPLE_ID, sampleID.toString());
            }
            else {
                throw new Exception("No such collection.  ID " + collectionGUID + " does not refer to an existing collection.");
            }
        }
        else {
            throw new Exception("No such sample " + sampleID + ", cannot update.");
        }
    }

    public TmNeuron pushNeuron(TmNeuron neuron) throws Exception {

        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        // Workspace (or collection) containing neuron is the entity which
        // 'has' the neuron.  Neuron is an Entity-Data.
        Entity collectionEntity = entityBean.getEntityById(neuron.getOwnerKey(), neuron.getWorkspaceId());
        if (collectionEntity == null) {
            final String message = "Collection " + neuron.getWorkspaceId() + " not found.  Cannot push neuron.";
            log.warn(message);
            throw new Exception(message);
        }

        // May need to exchange this entity-data for existing one on workspace
        boolean rePushRequired = true;
        EntityData preExistingEntityData = null;
        if (neuron != null && idGiven(neuron)) {
            for (EntityData edata : collectionEntity.getEntityData()) {
                log.debug("Comparing neuron " + neuron.getId() + " to entity data " + edata.getId() + ".");
                if (edata.getId() == null) {
                    log.warn("No id in entity data. " + edata);
                } else if (edata.getId().equals(neuron.getId())) {
                    preExistingEntityData = edata;
                    break;
                }
            }
        }

        // Must now create a new entity data
        EntityData entityData = new EntityData();
        entityData.setOwnerKey(neuron.getOwnerKey());
        entityData.setCreationDate(neuron.getCreationDate());
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        if (idGiven(neuron)) {
            entityData.setId(neuron.getId());
            rePushRequired = false;
        }
        entityData.setParentEntity(collectionEntity);

        // save back.
        log.info("Saving back the neuron " + entityData.getId());
        EntityData savedEntityData = entityBean.saveOrUpdateEntityData(entityData);
        if (preExistingEntityData != null) {
            log.info("Removing old entitydata for id " + preExistingEntityData.getId());
            collectionEntity.getEntityData().remove(preExistingEntityData);
        }
        log.info("Adding neuron to its collection.");
        collectionEntity.getEntityData().add(savedEntityData);
        log.info("Saved back the neuron " + entityData.getId());
        //collectionEntity.getEntityData().add(savedEntityData);

        if (rePushRequired) {
            log.info("Re-saving neuron with neuron id embedded in geo annotations.");
            // Now that the neuron has been db-dipped, we can get its ID, and 
            // push that into all points.
            Long id = savedEntityData.getId();
            for (TmGeoAnnotation anno : neuron.getGeoAnnotationMap().values()) {
                anno.setNeuronId(id);
            }
            // Need to make serializable version of the data.
            neuron.setId(id);
            createEntityData(entityData, neuron);
            EntityData savedEd = entityBean.saveOrUpdateEntityData(entityData);
            neuron.setId(id);
        }

        return neuron;
    }

    private TmNeuron createNeuron(Long id, Long collectionGUID) throws Exception {
        TmNeuron neuron = new TmNeuron();
        neuron.setId(id);
        // Not really a workspace.  If/when this neuron is copied into a
        // real workspace, this must be addressed.
        neuron.setWorkspaceId(collectionGUID);
        neuron.setName("Neuron_" + id);
        neuron.setOwnerKey(TmVersionedNeuronCollection.DEFAULT_OWNER_KEY);
        neuron.setCreationDate(new Date());
     
        // Save neuron to db
        neuron = pushNeuron(neuron);
        return neuron;
    }
    
    private TmNeuron fetchNeuron(Long collectionGUID, Long neuronGUID) throws Exception {
        TmNeuron rtnVal = null;
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        Entity parentEntity = entityBean.getEntityById(TmVersionedNeuronCollection.DEFAULT_OWNER_KEY, collectionGUID);
        if (parentEntity != null  &&  parentEntity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
            for (EntityData edata: parentEntity.getEntityData()) {
                if (edata.getId().equals(neuronGUID)) {
                    rtnVal = extractTmNeuron(entityBean, collectionGUID, neuronGUID, rtnVal);
                }                        
            }
        }
        return rtnVal;
    }

    private TmNeuron extractTmNeuron(EntityBeanLocal entityBean, Long collectionGUID, Long neuronGUID, TmNeuron rtnVal) throws ComputeException, Exception {
        // Got the neuron's entity-data.
        byte[] de64Data = entityBean.getB64DecodedEntityDataValue(collectionGUID, neuronGUID, EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
        if (de64Data != null) {
            rtnVal = exchanger.deserializeNeuron(de64Data);
        }
        return rtnVal;
    }
    
    private void createEntityData(EntityData entityData, TmNeuron neuron) throws Exception {
        byte[] serializableBytes = exchanger.serializeNeuron(neuron);
        // Encoding on the client side for convenience: the save-or-update
        // method already exists.  We expect to see this carried out one
        // neuron (or two) at a time, not wholesale.
        // @todo is there real danger of this being removed?
        BASE64Encoder encoder = new BASE64Encoder();
        entityData.setValue(encoder.encode(serializableBytes));
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
    }

    private boolean idGiven(TmNeuron neuron) {
        return neuron.getId() != null  &&  neuron.getId() != -1;
    }
    
    /**
     * Ensure we have a new, internal GUID version of the external GUID.
     * 
     * @param id given by REST call.
     * @param extIdToGUID mapping to generated GUIDs.
     * @return internal version of this ID.
     */
    private Long exchangeGUID( Long id, Map<Long, Long> extIdToGUID ) {
        Long rtnVal = -1L;
        if (id == -1L) {
            rtnVal = id;
        }
        else if (id > MINIMUM_GUID) {
            rtnVal = id;
        }
        else if (extIdToGUID.containsKey(id)) {
            rtnVal = extIdToGUID.get(id);
        }
        else {
            rtnVal = idSource.next();
            extIdToGUID.put(id, rtnVal);
        }
        return rtnVal;
    }
    
    private TmGeoAnnotation buildTmGeoAnnotation(Long pointGUID, Long neuronGUID, Long parentPointGUID, double x, double y, double z, Double radius) {
        TmGeoAnnotation annotation = new TmGeoAnnotation();
        annotation.setId(pointGUID);
        annotation.setCreationDate(new Date());
        annotation.setIndex(1);
        annotation.setNeuronId(neuronGUID);
        annotation.setParentId(parentPointGUID);
        annotation.setX(x);
        annotation.setY(y);
        annotation.setZ(z);
        annotation.setRadius(radius);
        return annotation;
    }
    
    private Entity getPropertySetEntity(Entity collectionEntity) {
        Entity propertySetEntity = null;
        for (Entity childEntity : collectionEntity.getChildren()) {
            if (EntityConstants.TYPE_PROPERTY_SET.equals(childEntity.getEntityTypeName())
                    && TmVersionedNeuronCollection.ID_TO_TAG_NAME.equals(childEntity.getName())) {
                propertySetEntity = childEntity;
            }
        }
        if (propertySetEntity == null) {
            // Must create the property set.
            propertySetEntity = new Entity();
            propertySetEntity.setName(TmVersionedNeuronCollection.ID_TO_TAG_NAME);
            propertySetEntity.setEntityTypeName(EntityConstants.TYPE_PROPERTY_SET);
            propertySetEntity.setOwnerKey(DEFAULT_OWNER_KEY);
            propertySetEntity.setCreationDate(new Date());
            collectionEntity.addChildEntity(propertySetEntity, EntityConstants.ATTRIBUTE_ENTITY);
        }
        return propertySetEntity;
    }

}
