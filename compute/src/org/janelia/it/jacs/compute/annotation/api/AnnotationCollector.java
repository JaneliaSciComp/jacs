/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.annotation.to.AnnotationPointCollection;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmVersionedNeuronCollection;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import sun.misc.BASE64Encoder;

/**
 * Delegated from Neuron API, to carry out the heaving lifting.
 *
 * @author fosterl
 */
public class AnnotationCollector {
    private Logger log = Logger.getLogger(AnnotationCollector.class);
    private final TmProtobufExchanger exchanger = new TmProtobufExchanger();
    
    public AnnotationCollector() {
    }
    
    public void addPointImpl(
            Long pointGUID, Long collectionGUID, Long neuronGUID,
            int x, int y, int z,
            int structureID,
            Long parentPointGUID) throws Exception {        
        TmNeuron neuron = fetchNeuron(collectionGUID, neuronGUID); // Get neuron from DB.
        if (neuron == null) {
            neuron = createNeuron(neuronGUID, collectionGUID);
        }
        TmGeoAnnotation annotation = new TmGeoAnnotation();
        annotation.setId(pointGUID);
        annotation.setCreationDate(new Date());
        annotation.setIndex(1);
        annotation.setNeuronId(neuronGUID);
        annotation.setParentId(parentPointGUID);
        annotation.setX((double)x);
        annotation.setY((double)y);
        annotation.setZ((double)z);
        if (parentPointGUID == null) {
            // This is a root.
            neuron.addRootAnnotation(annotation);
        }
        else {
            // Not a root.
            TmGeoAnnotation parent = neuron.getGeoAnnotationMap().get(parentPointGUID);
            if (parent != null) {
                parent.addChild(annotation);
            }
        }
        neuron.getGeoAnnotationMap().put(pointGUID, annotation);
        
        // Push neuron to DB.
        neuron = pushNeuron(neuron); // Saving ref as reminder: object changes.
    }

    public void removeNeuronImpl(Long neuronGUID) throws Exception {
        log.info("Removed neuron " + neuronGUID);
        // Ensure this is in the database, and eliminate it.
        EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        entityBean.deleteEntityData(TmVersionedNeuronCollection.DEFAULT_OWNER_KEY, neuronGUID);
    }

    public void removeCollectionImpl(Long collectionGUID) throws Exception {
        log.info("Removed collection " + collectionGUID);
        // Ensure this is in the db and then eliminate it.
        //todo
    }

    public void addCollectionImpl(AnnotationPointCollection collection) throws Exception {
        TmVersionedNeuronCollection tmCollection = new TmVersionedNeuronCollection();
        tmCollection.setId(collection.guid);
        tmCollection.setName(collection.name);
        tmCollection.setSampleID(collection.sampleID);        
        tmCollection.setNeuronList(new ArrayList<TmNeuron>());
        Date createDate = new Date();
        tmCollection.setCreateDate(createDate);
        tmCollection.setVersion(createDate.getTime() + "_" + collection.versionNumber);
        
        // NOTE: must first check whether the sample ID exists.  If not, fail.
        EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        Entity sampleEntity = entityBean.getEntityById(tmCollection.getOwnerKey(), collection.sampleID);
        if (sampleEntity != null  &&  sampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            // Must create a new collection object, for the neurons.  That will
            // correspond to a workspace.  Add this to the designated folder.
            //TODO
        }
        else {
            throw new Exception("No such sample " + collection.sampleID + ", cannot create collection.");
        }
        
    }

    public void setCollectionSampleIDImpl(Long sampleID, Long collectionGUID) throws Exception {
        log.info("Setting the sampleID to " + sampleID + " for collection " + collectionGUID);
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
        EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        Entity parentEntity = entityBean.getEntityById(TmVersionedNeuronCollection.DEFAULT_OWNER_KEY, collectionGUID);
        if (parentEntity != null  &&  parentEntity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
            for (EntityData edata: parentEntity.getEntityData()) {
                if (edata.getId().equals(neuronGUID)) {
                    // Got the neuron's entity-data.
                    byte[] de64Data = entityBean.getB64DecodedEntityDataValue(collectionGUID, neuronGUID, EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
                    if (de64Data != null) {
                        rtnVal = exchanger.deserializeNeuron(de64Data);
                    }
                }                        
            }
        }
        return rtnVal;
    }
    
    private TmNeuron pushNeuron(TmNeuron neuron) throws Exception {
        // Need to make serializable version of the data.
        byte[] serializableBytes = exchanger.serializeNeuron(neuron);
        
        EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
        // Workspace (or collection) containing neuron is the entity which
        // 'has' the neuron.  Neuron is an Entity-Data.
        Entity collectionEntity = entityBean.getEntityById(neuron.getOwnerKey(), neuron.getWorkspaceId());

        // May need to exchange this entity-data for existing one on workspace
        EntityData preExistingEntityData = null;
        if (neuron != null && neuron.getId() != null) {
            for (EntityData edata : collectionEntity.getEntityData()) {
                log.debug("Comparing neuron "+neuron.getId()+" to entity data "+edata.getId()+".");
                if (edata.getId() == null) {
                    log.warn("No id in entity data. "+edata);
                } else if (edata.getId().equals(neuron.getId())) {
                    preExistingEntityData = edata;
                    break;
                }
            }
        }
        else {
            throw new Exception("Incomplete neuron. Null or no ID.");
        }

        // Must now push a new entity data
        EntityData entityData = new EntityData();
        entityData.setOwnerKey(neuron.getOwnerKey());
        entityData.setCreationDate(neuron.getCreationDate());
        entityData.setId(neuron.getId());  // May have been seeded as null.
        entityData.setParentEntity(collectionEntity);
        // Encoding on the client side for convenience: the save-or-update
        // method already exists.  We expect to see this carried out one
        // neuron (or two) at a time, not wholesale.
        // @todo is there real danger of this being removed?
        BASE64Encoder encoder = new BASE64Encoder();
        entityData.setValue(encoder.encode(serializableBytes));
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        
        // save back.
        EntityData savedEntityData = entityBean.saveOrUpdateEntityData(neuron.getOwnerKey(), entityData);
        if (preExistingEntityData != null) {
            collectionEntity.getEntityData().remove(preExistingEntityData);
        }
        collectionEntity.getEntityData().add(savedEntityData);
        return neuron;
    }
}
