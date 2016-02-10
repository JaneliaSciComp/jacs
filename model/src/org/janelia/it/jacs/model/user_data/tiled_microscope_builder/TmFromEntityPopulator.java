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
    public TmWorkspace loadWorkspace(Entity entity, Entity sampleEntity, TmWorkspace.Version wsVersion) throws Exception {
        TmWorkspace rtnVal = new TmWorkspace();
        rtnVal.setWorkspaceVersion(wsVersion);
        populateFromEntity(entity, sampleEntity, rtnVal);
        return rtnVal;
    }
    
    public TmWorkspace loadWorkspace(Entity entity, Entity sampleEntity) throws Exception {
        return loadWorkspace(entity, sampleEntity, TmWorkspace.Version.PB_1);
    }
    
    public TmNeuron loadNeuron(Entity entity) throws Exception, TmConnectivityException {
        TmNeuron rtnVal = new TmNeuron();
        populateFromEntity(entity, rtnVal);
        return rtnVal;
    }
    
    public TmPreferences createTmPreferences(Entity entity) throws Exception {
        TmPreferences prefs = new TmPreferences();
        populateFromEntity(entity, prefs);
        return prefs;
    }
    
    /**
     * create from string from entity data; expected format is:
     * id:annID1:annID2:x,y,z:(repeat points)
     */
    public TmAnchoredPath createTmAnchoredPath(String pathString) throws Exception {
        String[] fields = pathString.split(":", -1);
        if (fields.length < 3) {
            throw new Exception("not enough separators in pathString");
        }
        Long id = new Long(fields[0]);
        TmAnchoredPathEndpoints endpoints = new TmAnchoredPathEndpoints(new Long(fields[1]), new Long(fields[2]));
        List<List<Integer>> pointList = new ArrayList<>();
        
        for (int i = 3; i < fields.length; i++) {
            String[] coords = fields[i].split(",");
            if (coords.length != 3) {
                throw new Exception(String.format("couldn't parse coordinates %s", fields[i]));
            }
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(Integer.parseInt(coords[0]));
            temp.add(Integer.parseInt(coords[1]));
            temp.add(Integer.parseInt(coords[2]));
            pointList.add(temp);
        }
        return new TmAnchoredPath(id, endpoints, pointList);
    }

    public String toAnchoredPathStringFromArguments(Long id, Long annotationID1, Long annotationID2, List<List<Integer>> pointList)
        throws Exception {
        // side note: we don't use TmAnchoredPathEndpoints here because this method is typically
        //  for the use of TiledMicroscopeDAO, which ends up working with the individual 
        //  annotations and strings thereof

        if (annotationID1 > annotationID2) {
            Long temp = annotationID1;
            annotationID1 = annotationID2;
            annotationID2 = temp;
        }

        // make a gross estimate at initial capacity, given format
        StringBuilder builder = new StringBuilder(30 + 15 * pointList.size());
        builder.append(String.format("%d:%d:%d", id, annotationID1, annotationID2));
        for (List<Integer> point : pointList) {
            builder.append(String.format(":%d,%d,%d", point.get(0), point.get(1), point.get(2)));
        }
        // 1G is the approx. limit on data transfer in MySQL in our environment
        if (builder.length() > 1000000000) {
            throw new Exception("too many points!");
        }
        return builder.toString();
    }

    public String toStructuredTextStringFromArguments(Long id, Long parentID, int parentType, int formatVersion,
        String dataString) {
        return String.format("%d:%d:%d:%d:%s", id, parentID, parentType, formatVersion, dataString);
    }
    
    public TmStructuredTextAnnotation createTmStructuredTextAnnotation(String annString) throws Exception {
        // expect: id:parentid:parenttype:formatversion:datastring
        // note that datastring will hold colons as well (it's JSON), so stop the split at 5
        String[] items = annString.split(":", 5);
        if (items.length < 5) {
            throw new Exception("could not parse annotation string " + annString);
        }

        Long id = new Long(items[0]);
        Long parentId = new Long(items[1]);

        // I'm not fond of this, but fiddling with an enum seemed overboard
        Integer parentType = Integer.parseInt(items[2]);
        if (parentType < 1 || parentType > 3) {
            throw new Exception(String.format("annotation string %s has bad parent type %s", annString, parentType));
        }

        // here we make sure we can handle the stored data; when we have v2, and we read
        //  v1, we'll do conversion and update here
        int storedFormatVersion = Integer.parseInt(items[3]);
        if (storedFormatVersion > TmStructuredTextAnnotation.FORMAT_VERSION) {
            throw new Exception(String.format("annotation string %s has newer format version %d than we can handle!", annString, storedFormatVersion));
        }
        // and someday... else storedFormatVersion < FORMAT_VERSION then does an update

        String dataString = items[4];
        TmStructuredTextAnnotation annotation = new TmStructuredTextAnnotation(
                id, parentId, parentType,dataString
        );
        return annotation;
    }
    
    public TmGeoAnnotation createTmGeoAnnotation(EntityData data) throws Exception {
        TmGeoAnnotation rtnVal = new TmGeoAnnotation();
        // format expected: <id>:<parentId>:<index>:<x,y,z>:<comment>
        String geoString = data.getValue();
        String[] fields = geoString.split(":", -1);
        if (fields.length < 5) {
            throw new Exception("Could not parse geoString=" + geoString);
        }
        rtnVal.setId(new Long(fields[0]));
        rtnVal.setParentId(new Long(fields[1]));
        rtnVal.setIndex(new Integer(fields[2]));
        String coordinateString = fields[3];
        String[] cArr = coordinateString.split(",");
        rtnVal.setX(new Double(cArr[0].trim()));
        rtnVal.setY(new Double(cArr[1].trim()));
        rtnVal.setZ(new Double(cArr[2].trim()));

        if (fields.length > 5) {
            // comment field had a : in it; reassemble:
            // (I'd like to use Guava Joiner here, but it's not happy for some reason)
            StringBuilder builder = new StringBuilder();
            builder.append(fields[4]);
            for (int i = 5; i < fields.length; i++) {
                builder.append(":");
                builder.append(fields[i]);
            }
            rtnVal.setComment(builder.toString());
        } else {
            rtnVal.setComment(fields[4]);
        }
        rtnVal.setCreationDate(data.getCreationDate());
        return rtnVal;
    }

    private void populateFromEntity(Entity entity, Entity sampleEntity, TmWorkspace workspace) throws Exception {
        if (entity.getEntityTypeName() == null || !entity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            throw new Exception("Entity type must be=" + EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
        }
        workspace.setId(entity.getId());
        workspace.setName(entity.getName());
        workspace.setOwnerKey(entity.getOwnerKey());

        // This avoids loading old-style, entity-based neurons, unless the
        // workspace is from the previous workstation version.        
        List<TmNeuron> neuronList = new ArrayList<>();
        for (Entity child : entity.getChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)
                &&  (!workspace.getVersion().equals(TmWorkspace.Version.PB_1))) {
                neuronList.add(loadNeuron(child));
            } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                workspace.setPreferences(createTmPreferences(child));
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

    private void populateFromEntity(Entity neuronEntity, TmNeuron tmNeuron) throws Exception, TmConnectivityException {
        if (!neuronEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
            throw new Exception("Entity type must be " + EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
        }

        final Long id = neuronEntity.getId();

        tmNeuron.setId(id);
        tmNeuron.setName(neuronEntity.getName());
        tmNeuron.setCreationDate(neuronEntity.getCreationDate());

        Map<Long, TmStructuredTextAnnotation> textAnnotationMap = tmNeuron.getStructuredTextAnnotationMap();
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = tmNeuron.getAnchoredPathMap();
        Map<Long, TmGeoAnnotation> geoAnnotationMap = tmNeuron.getGeoAnnotationMap();

        // First step is to take all those entity data and put them into the
        //  appropriate objects, and the objects into the right collections
        for (EntityData ed : neuronEntity.getEntityData()) {
            String edAttr = ed.getEntityAttrName();
            if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)
                    || edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                TmGeoAnnotation ga = createTmGeoAnnotation(ed);
                if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                    tmNeuron.addRootAnnotation(ga);
                }
                geoAnnotationMap.put(ga.getId(), ga);
                ga.setNeuronId(id);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                TmAnchoredPath path = createTmAnchoredPath(ed.getValue());
                anchoredPathMap.put(path.getEndpoints(), path);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT)) {
                TmStructuredTextAnnotation ann = createTmStructuredTextAnnotation(ed.getValue());
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

    private void populateFromEntity(Entity entity, TmPreferences preferences) throws Exception {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
            throw new Exception("Entity type must be " + EntityConstants.TYPE_PROPERTY_SET);
        }
        preferences.setId(entity.getId());
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                String propertyString = ed.getValue();
                int eIndex = propertyString.indexOf("=");
                String key = propertyString.substring(0, eIndex);
                String value = propertyString.substring(eIndex + 1, propertyString.length());
                preferences.setProperty(key, value);
            }
        }
    }

    private Matrix deserializeMatrix(String matrixString, String matrixName) {
        return MatrixUtilities.deserializeMatrix(matrixString, matrixName);
    }
    
}
