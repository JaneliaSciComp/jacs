/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_builder;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmConnectivityException;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmPreferences;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.util.MatrixUtilities;

/**
 * Code to help TM objects be populated from entities.
 *
 * @author fosterl
 */
public class TmFromEntityPopulator {
    public void populateWorkspace(Entity entity, Entity sampleEntity, TmWorkspace workspace) throws Exception {
        if (entity.getEntityTypeName()==null || !entity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            throw new Exception("Entity type must be="+EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
        }
        workspace.setFromEntity(entity);
        List<TmNeuron> neuronList = new ArrayList<>();        
        for (Entity child : entity.getChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                TmNeuron neuron=new TmNeuron(child);
                neuronList.add(neuron);
            } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                workspace.setPreferences(new TmPreferences(child));
            }
        }
        workspace.setNeuronList(neuronList);

        if (sampleEntity != null) {
            workspace.setSampleID(sampleEntity.getId());
            String matrixStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX);
            if (matrixStr != null) {
                Matrix matrix = deserializeMatrix(matrixStr, EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX);
                workspace.setVoxToMicronMatrix(matrix);
            }
            matrixStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
            if (matrixStr != null) {
                Matrix matrix = deserializeMatrix(matrixStr, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
                workspace.setMicronToVoxMatrix(matrix);
            }
        }
    }
    
    public void populateNeuron(Entity entity, TmNeuron neuron) throws Exception, TmConnectivityException {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
            throw new Exception("Entity type must be " + EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
        }

        neuron.setFromEntity(entity);
        final Long id = neuron.getId();
        Map<Long, TmStructuredTextAnnotation> textAnnotationMap = neuron.getStructuredTextAnnotationMap();
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = neuron.getAnchoredPathMap();
        List<TmGeoAnnotation> rootAnnotations = neuron.getRootAnnotations();
        Map<Long, TmGeoAnnotation> geoAnnotationMap = neuron.getGeoAnnotationMap();

        // First step is to take all those entity data and put them into the
        //  appropriate objects, and the objects into the right collections
        for (EntityData ed : entity.getEntityData()) {
            String edAttr = ed.getEntityAttrName();
            if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)
                    || edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                TmGeoAnnotation ga = new TmGeoAnnotation(ed);
                if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                    rootAnnotations.add(ga);
                }
                geoAnnotationMap.put(ga.getId(), ga);
                ga.setNeuronId(id);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                TmAnchoredPath path = new TmAnchoredPath(ed.getValue());
                anchoredPathMap.put(path.getEndpoints(), path);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT)) {
                TmStructuredTextAnnotation ann = new TmStructuredTextAnnotation(ed.getValue());
                textAnnotationMap.put(ann.getParentId(), ann);
            }
        }
        // Second step is to link children to produce the graph for
        //  the GeoAnnotations
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            Long parentId = ga.getParentId();
            // if parent ID is the neuron ID, it's a root, the ID won't be in
            //  the map, and we don't need to connect it:
            if (!parentId.equals(id)) {
                TmGeoAnnotation parent = geoAnnotationMap.get(parentId);
                if (parent == null) {
                    throw new TmConnectivityException(String.format("Could not find parent for TmGeoAnnotation id = %d in neuron id = %d", ga.getId(), id));
                }
                parent.addChild(ga);
            }
        }
    }

    public void populateFromEntity(Entity entity, TmPreferences preferences) throws Exception {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
            throw new Exception("Entity type must be " + EntityConstants.TYPE_PROPERTY_SET);
        }
        preferences.setId(entity.getId());
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                String propertyString=ed.getValue();
                int eIndex=propertyString.indexOf("=");
                String key=propertyString.substring(0,eIndex);
                String value=propertyString.substring(eIndex+1, propertyString.length());
                preferences.setProperty(key, value);
            }
        }
    }

    private Matrix deserializeMatrix(String matrixString, String matrixName) {
        return MatrixUtilities.deserializeMatrix(matrixString, matrixName);
    }
    
}
