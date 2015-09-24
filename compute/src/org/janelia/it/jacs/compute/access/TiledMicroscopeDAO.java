package org.janelia.it.jacs.compute.access;

import com.google.common.base.Stopwatch;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.largevolume.RawFileFetcher;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.jacs.shared.img_3d_loader.TifVolumeFileLoader;

import java.util.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:57 PM
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
        log.debug("createTiledMicroscopeEntityTypes() - TiledMicroscopeDAO layer");

        try {
            log.debug("Creating Workspace entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE, EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);

            log.debug("Creating Neuron entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_ANCHORED_PATH);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            
            log.debug("Creating PropertySet entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_PROPERTY_SET);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_PROPERTY_SET, EntityConstants.ATTRIBUTE_PROPERTY);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }

        log.debug("createTiledMicroscopeEntityTypes() - done");
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
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
            workspace.setEntityTypeName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
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
            sampleEd.setEntityAttrName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
            // need this?
            // sampleEd.setOrderIndex(0);
            sampleEd.setParentEntity(workspace);
            sampleEd.setValue(brainSampleId.toString());
            annotationDAO.saveOrUpdate(sampleEd);
            workspace.getEntityData().add(sampleEd);
            annotationDAO.saveOrUpdate(workspace);
            Entity sampleEntity = annotationDAO.getEntityById(brainSampleId);
            // back to user
            TmWorkspace tmWorkspace=new TmWorkspace(workspace, sampleEntity);
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
            if (!workspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity neuron=new Entity();
            neuron.setCreationDate(new Date());
            neuron.setUpdatedDate(new Date());
            neuron.setName(name);
            neuron.setOwnerKey(workspace.getOwnerKey());
            neuron.setEntityTypeName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            annotationDAO.saveOrUpdate(neuron);
            // old
            // EntityData ed = workspace.addChildEntity(neuron, EntityConstants.ATTRIBUTE_ENTITY);
            // annotationDAO.saveOrUpdate(ed);
            // Konrad said use annDAO instead so permissions are properly carried over:
            annotationDAO.addEntityToParent(workspace, neuron, 1, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.saveOrUpdate(workspace);
            TmNeuron tmNeuron=new TmNeuron(neuron);
            return tmNeuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws DaoException {
        try {
            String subjectKey = "user:"+user;
            String folderName = "3D Tile Microscope Samples";
            Collection<Entity> folders = annotationDAO.getEntitiesByName(subjectKey, folderName);
            Entity folder;
            if (folders!=null && folders.size()>0) {
                folder = folders.iterator().next();
            }
            else {
                folder = annotationDAO.createFolderInDefaultWorkspace(subjectKey, folderName).getChildEntity();
            }

            Entity sample = newEntity(sampleName, EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE, subjectKey, false);
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pathToRenderFolder);
            annotationDAO.saveOrUpdateEntity(sample);
            log.debug("Saved sample as " + sample.getId());
            annotationDAO.addEntityToParent(folder, sample, folder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
            return new TmSample(sample);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    private Entity newEntity(String name, String entityTypeName, String ownerKey, boolean isCommonRoot) throws ComputeException {
        Date createDate = new Date();
        Entity entity = new Entity();
        entity.setName(name);
        entity.setOwnerKey(ownerKey);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityTypeName(entityTypeName);
        if (isCommonRoot) {
            entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT, "Common Root");
        }
        return entity;
    }

    protected TmPreferences createTiledMicroscopePreferences(Long workspaceId) throws DaoException {
        try {
            Entity workspace = annotationDAO.getEntityById(workspaceId);
            if (!workspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled microscope preferences must be created with valid Workspace Id");
            }
            Entity preferences=new Entity();
            preferences.setName("preferences");
            preferences.setCreationDate(new Date());
            preferences.setUpdatedDate(new Date());
            preferences.setOwnerKey(workspace.getOwnerKey());
            preferences.setEntityTypeName(EntityConstants.TYPE_PROPERTY_SET);
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

    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
        List<List<Integer>> pointlist) throws Exception {

        try {
            for (List<Integer> point: pointlist) {
                if (point.size() != 3) {
                    throw new Exception("all points must be 3-vectors");
                }
            }

            // retrieve neuron; object is easier to check that annotations in neuron
            TmNeuron neuron = loadNeuron(neuronID);
            if (!neuron.getGeoAnnotationMap().containsKey(annotationID1) ||
                    !neuron.getGeoAnnotationMap().containsKey(annotationID2)) {
                throw new Exception("both annotations must be in neuron");
            }

            // to do real work, though, we need the entity:
            Entity neuronEntity = annotationDAO.getEntityById(neuronID);
            if (!neuronEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type="+neuronID);
            }

            EntityData pathData=new EntityData();
            pathData.setOwnerKey(neuronEntity.getOwnerKey());
            pathData.setCreationDate(new Date());
            pathData.setUpdatedDate(new Date());
            pathData.setEntityAttrName(EntityConstants.ATTRIBUTE_ANCHORED_PATH);
            pathData.setOrderIndex(0);
            pathData.setParentEntity(neuronEntity);
            // perhaps not entirely kosher to use this temp value, but it works
            pathData.setValue(TMP_GEO_VALUE);
            annotationDAO.saveOrUpdate(pathData);
            neuronEntity.getEntityData().add(pathData);
            annotationDAO.saveOrUpdate(neuronEntity);

            // Find and update value string
            boolean valueStringUpdated=false;
            String valueString=null;
            for (EntityData ed: neuronEntity.getEntityData()) {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                    if (ed.getValue().equals(TMP_GEO_VALUE)) {
                        valueString=TmAnchoredPath.toStringFromArguments(ed.getId(), annotationID1, annotationID2, pointlist);
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated=true;
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find anchor path entity data to update for value string");
            }
            TmAnchoredPath anchoredPath = new TmAnchoredPath(valueString);
            return anchoredPath;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws DaoException {
        try {
            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();
            // Retrieve neuron
            System.out.println("getting neuron entity: " + stopwatch);
            Entity neuron=annotationDAO.getEntityById(neuronId);
            if (!neuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type="+neuronId);
            }
            // Check if root; if not, find its parent
            boolean isRoot=false;
            if (parentAnnotationId==null) {
                isRoot=true;
            } else {
                // Validate
                System.out.println("finding parent: " + stopwatch);
                boolean foundParent=false;
                for (EntityData ed : neuron.getEntityData()) {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE) ||
                            ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        String value=ed.getValue();
                        // note: really ought to unify this parsing with the parsing of EntityData in TmNeuron
                        String[] vArr=value.split(":");
                        Long pId=new Long(vArr[0]);
                        if (pId.equals(parentAnnotationId)) {
                            foundParent=true;
                            break;
                        }
                    }
                }
                if (!foundParent) {
                    throw new Exception("Could not find parent matching parentId="+parentAnnotationId);
                }
            }
            System.out.println("creating, populating new EntityData: " + stopwatch);
            EntityData geoEd=new EntityData();
            geoEd.setOwnerKey(neuron.getOwnerKey());
            geoEd.setCreationDate(new Date());
            geoEd.setUpdatedDate(new Date());
            Long parentId=0L;
            if (isRoot) {
                parentId = neuron.getId();
                geoEd.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            } else {
                parentId=parentAnnotationId;
                geoEd.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            }
            geoEd.setOrderIndex(0);
            geoEd.setParentEntity(neuron);
            geoEd.setValue(TMP_GEO_VALUE);
            System.out.println("saving EntityData: " + stopwatch);
            annotationDAO.saveOrUpdate(geoEd);
            neuron.getEntityData().add(geoEd);
            System.out.println("saving neuron: " + stopwatch);
            annotationDAO.saveOrUpdate(neuron);
            // Find and update value string
            System.out.println("finding new ED again: " + stopwatch);
            boolean valueStringUpdated=false;
            String valueString=null;
            for (EntityData ed : neuron.getEntityData()) {
                if (isRoot) {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        if (ed.getValue().equals(TMP_GEO_VALUE)) {
                            valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                            ed.setValue(valueString);
                            System.out.println("saving EntityData, part 2: " + stopwatch);
                            annotationDAO.saveOrUpdate(ed);
                            valueStringUpdated=true;
                        }
                    }
                } else {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                        if (ed.getValue().equals(TMP_GEO_VALUE)) {
                            valueString=TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                            ed.setValue(valueString);
                            System.out.println("saving EntityData, part 2: " + stopwatch);
                            annotationDAO.saveOrUpdate(ed);
                            valueStringUpdated=true;
                        }
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find geo entry to update for value string");
            }
            System.out.println("creating new GeoAnn object: " + stopwatch);
            TmGeoAnnotation geoAnnotation=new TmGeoAnnotation(geoEd);
            // normally this is filled in automatically when the annotation is part of
            //  a neuron, but here it's not (explicitly); however, we know the value
            //  to put in:
            geoAnnotation.setNeuronId(neuronId);
            System.out.println("returning new annotation: " + stopwatch);
            return geoAnnotation;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * add a structured text annotation to a thing that doesn't have one
     */
    public TmStructuredTextAnnotation addStructuredTextAnnotation(Long neuronID, Long parentID, int parentType, int formatVersion,
        String data) throws DaoException {
        
        try {
            // get the neuron entity
            Entity neuron=annotationDAO.getEntityById(neuronID);
            if (!neuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type =" + neuronID);
            }

            // parent must be neuron or geoann:
            if (parentType != TmStructuredTextAnnotation.GEOMETRIC_ANNOTATION &&
                    parentType != TmStructuredTextAnnotation.NEURON) {
                throw new Exception("parent must be a geometric annotation or a neuron");
            }

            // parent must not already have a structured text annotation
            if (loadNeuron(neuronID).getStructuredTextAnnotationMap().containsKey(parentID)) {
                throw new Exception("parent ID already has a structured text annotation; use update, not add");
            }

            EntityData entityData = new EntityData();
            entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT);
            entityData.setOwnerKey(neuron.getOwnerKey());
            entityData.setCreationDate(new Date());
            entityData.setUpdatedDate(new Date());
            entityData.setOrderIndex(0);
            entityData.setParentEntity(neuron);
            // this is kind of bogus, but it works:
            entityData.setValue(TMP_GEO_VALUE);
            annotationDAO.saveOrUpdate(entityData);
            neuron.getEntityData().add(entityData);
            annotationDAO.saveOrUpdate(neuron);

            // Find and update value string
            boolean valueStringUpdated=false;
            String valueString=null;
            for (EntityData ed : neuron.getEntityData()) {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT)) {
                    if (ed.getValue().equals(TMP_GEO_VALUE)) {
                        valueString = TmStructuredTextAnnotation.toStringFromArguments(ed.getId(), parentID,
                                parentType, formatVersion, data);
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated = true;
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find temp geo entry to update for value string");
            }
            TmStructuredTextAnnotation structeredAnnotation = new TmStructuredTextAnnotation(valueString);
            return structeredAnnotation;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
       List<List<Integer>> pointList) throws DaoException {
        try {
            EntityData ed=(EntityData) computeDAO.genericLoad(EntityData.class, anchoredPath.getId());
            String valueString=TmAnchoredPath.toStringFromArguments(anchoredPath.getId(),
                    annotationID1, annotationID2, pointList);
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
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

    public void updateStructuredTextAnnotation(TmStructuredTextAnnotation textAnnotation, String data)
        throws DaoException {
        try {
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, textAnnotation.getId());
            String valueString = TmStructuredTextAnnotation.toStringFromArguments(textAnnotation.getId(),
                    textAnnotation.getParentId(), textAnnotation.getParentType(), textAnnotation.getFormatVersion(), data);
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
            throw new DaoException("input neuron doesn't contain child annotation " + annotation.getId());
        }
        if (!neuron.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new DaoException("input neuron doesn't contain new parent annotation " + newParentAnnotationID);
        }

        // is it already the parent?
        if (annotation.getParentId().equals(newParentAnnotationID)) {
            return;
        }

        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation: neuron.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
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
        if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
        }

        // change the parent ID and save
        String valueString=TmGeoAnnotation.toStringFromArguments(annotation.getId(), newParentAnnotationID,
                annotation.getIndex(), annotation.getX(), annotation.getY(), annotation.getZ(),
                annotation.getComment());
        ed.setValue(valueString);
        annotationDAO.saveOrUpdate(ed);

    }

    /**
     * @param neuron = neuron containing annotation
     * @param newRoot = annotation to make root in neurite within neuron
     */
    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws DaoException {
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new DaoException(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
        }

        // is it already a root?
        if (newRoot.isRoot()) {
            return;
        }

        // from input, follow parents up to current root, keeping them all
        List<TmGeoAnnotation> parentList = new ArrayList<TmGeoAnnotation>();
        TmGeoAnnotation testAnnotation = newRoot;
        while (!testAnnotation.isRoot()) {
            parentList.add(testAnnotation);
            testAnnotation = neuron.getParentOf(testAnnotation);
        }
        TmGeoAnnotation oldRoot = testAnnotation;
        parentList.add(testAnnotation);

        try {
            // change new root to GEO_ROOT entity data type; reset its parent
            //  to the neuron (as one does for a root)
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, newRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            String valueString = TmGeoAnnotation.toStringFromArguments(newRoot.getId(), neuron.getId(),
                    newRoot.getIndex(), newRoot.getX(), newRoot.getY(), newRoot.getZ(), newRoot.getComment());
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);

            // reparent intervening annotations; skip the first item, which is the
            //  new root (which we've already dealt with)
            for (int i=1; i<parentList.size(); i++) {
                // change the parent ID and save
                TmGeoAnnotation ann = parentList.get(i);
                Long newParentAnnotationID = parentList.get(i - 1).getId();
                ed = (EntityData) computeDAO.genericLoad(EntityData.class, ann.getId());
                valueString = TmGeoAnnotation.toStringFromArguments(ann.getId(), newParentAnnotationID,
                    ann.getIndex(), ann.getX(), ann.getY(), ann.getZ(), ann.getComment());
                ed.setValue(valueString);
                annotationDAO.saveOrUpdate(ed);
            }

            // change old root to GEO_TREE entity data type
            ed=(EntityData) computeDAO.genericLoad(EntityData.class, oldRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            annotationDAO.saveOrUpdate(ed);

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * split a neurite into two
     *
     * @param neuron = neuron containing the neurite
     * @param newRoot = annotation within neurite that will become root of new neurite,
     *                taking all its descendants with it
     * @throws DaoException
     */
    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws DaoException {
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new DaoException(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been 
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // turns out this is an easy, one step operation; all we need to do is
        //  change the annotation's parent to the neuron entity, and change
        //  its type to GEO_ROOT from GEO_TREE
        try {
            // change new root to GEO_ROOT entity data type; reset its parent
            //  to the neuron (as one does for a root)
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, newRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            String valueString = TmGeoAnnotation.toStringFromArguments(newRoot.getId(), neuron.getId(),
                    newRoot.getIndex(), newRoot.getX(), newRoot.getY(), newRoot.getZ(), newRoot.getComment());
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    /**
     * move the neurite containing the input annotation to the specified neuron;
     * also moves TmStructuredTextAnnotations and TmAnchoredPaths
     *
     * @throws DaoException
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron newNeuron) throws DaoException {

        // already in the neuron?  we're done
        EntityData annotationED = (EntityData) computeDAO.genericLoad(EntityData.class, annotation.getId());
        Entity oldNeuronEntity = annotationED.getParentEntity();
        if (oldNeuronEntity.getId().equals(newNeuron.getId())) {
            return;
        }

        // find root annotation of neurite; we need the neuron to help us with connectivity
        TmNeuron oldNeuron;
        try {
            oldNeuron = new TmNeuron(oldNeuronEntity);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldNeuron.getParentOf(rootAnnotation);
        }

        try {
            // move each annotation's entity data to a new entity (the new neuron)
            Set<Long> movedAnnotationIDs = new HashSet<>();
            Entity newNeuronEntity = annotationDAO.getEntityById(newNeuron.getId());
            for (TmGeoAnnotation ann : oldNeuron.getSubTreeList(rootAnnotation)) {
                movedAnnotationIDs.add(ann.getId());
                EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, ann.getId());
                ed.setParentEntity(newNeuronEntity);
                annotationDAO.saveOrUpdate(ed);
                // move any TmStructuredTextAnnotations as well:
                if (oldNeuron.getStructuredTextAnnotationMap().containsKey(ann.getId())) {
                    TmStructuredTextAnnotation note = oldNeuron.getStructuredTextAnnotationMap().get(ann.getId());
                    ed = (EntityData) computeDAO.genericLoad(EntityData.class, note.getId());
                    ed.setParentEntity(newNeuronEntity);
                    annotationDAO.saveOrUpdate(ed);
                }
            }

            // loop over anchored paths; if endpoints are in set of moved annotations,
            //  move the path as well
            for (TmAnchoredPathEndpoints endpoints: oldNeuron.getAnchoredPathMap().keySet()) {
                // both endpoints are necessarily in the same neurite, so only need
                //  to test one:
                if (movedAnnotationIDs.contains(endpoints.getAnnotationID1())) {
                    EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class,
                            oldNeuron.getAnchoredPathMap().get(endpoints).getId());
                    ed.setParentEntity(newNeuronEntity);
                    annotationDAO.saveOrUpdate(ed);
                }
            }

            // if it's the root, also change its parent annotation to the new neuron
            if (rootAnnotation.getId().equals(rootAnnotation.getId())) {
                EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, rootAnnotation.getId());
                String valueString = TmGeoAnnotation.toStringFromArguments(rootAnnotation.getId(), newNeuron.getId(),
                        rootAnnotation.getIndex(), rootAnnotation.getX(), rootAnnotation.getY(), rootAnnotation.getZ(), rootAnnotation.getComment());
                ed.setValue(valueString);
                annotationDAO.saveOrUpdate(ed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Workspaces must be parented with valid 3D Tile Microscope Sample Id");
            }
            List<TmWorkspaceDescriptor> descriptorList=new ArrayList<TmWorkspaceDescriptor>();
            for (Entity possibleWorkspace : brainSampleEntity.getChildren()) {
                if (possibleWorkspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                    if (possibleWorkspace.getOwnerKey().equals(ownerKey)) {
                        Long wId=possibleWorkspace.getId();
                        String wName=possibleWorkspace.getName();
                        int neuronCount=0;
                        for (EntityData ed : possibleWorkspace.getEntityData()) {
                            if (ed.getEntityAttrName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                                neuronCount++;
                            }
                        }
                        TmWorkspaceDescriptor descriptor=new TmWorkspaceDescriptor(wId, wName, neuronCount);
                        descriptorList.add(descriptor);
                    }
                }
            }
            Collections.sort(descriptorList, new Comparator<TmWorkspaceDescriptor>() {
                @Override
                public int compare(TmWorkspaceDescriptor a,
                                   TmWorkspaceDescriptor b) {
                    if (a.getId() < b.getId()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
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
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            List<TmNeuronDescriptor> descriptorList=new ArrayList<TmNeuronDescriptor>();
            for (Entity possibleNeuron : workspaceEntity.getChildren()) {
                if (possibleNeuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                    if (possibleNeuron.getOwnerKey().equals(ownerKey)) {
                        Long nId=possibleNeuron.getId();
                        String nName=possibleNeuron.getName();
                        int annoCount=0;
                        for (EntityData ed : possibleNeuron.getEntityData()) {
                            String edName=ed.getEntityAttrName();
                            if (edName.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE) || edName.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
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
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
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
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            String propertyAttrName=EntityConstants.ATTRIBUTE_PROPERTY;
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
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
                        EntityData ed=new EntityData(null, propertyAttrName, e, null, e.getOwnerKey(), key+"="+value, new Date(), null, 0);
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
            if (!entity.getEntityTypeName().equals(type)) {
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

    public void deleteAnchoredPath(Long pathID) throws DaoException {
        try {
            EntityData ed=(EntityData) annotationDAO.genericLoad(EntityData.class, pathID);
            annotationDAO.genericDelete(ed);
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

    public void deleteStructuredText(Long annID) throws DaoException {
        try {
            EntityData ed=(EntityData) annotationDAO.genericLoad(EntityData.class, annID);
            annotationDAO.genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws DaoException {
        Map<Integer,byte[]> rtnVal = new HashMap<>();
        try {
            // Get the bean of data around the point of interest.
            if ( log.isDebugEnabled() ) {
                log.debug("Getting nearest raw info to coord " + viewerCoord[0] + "," + viewerCoord[1] + "," + viewerCoord[2] + " from base path " + basePath);
            }
            RawFileInfo rawFileInfo =
                    getNearestFileInfo(basePath, viewerCoord);
            if ( rawFileInfo == null ) {
                throw new Exception("Failed to find any tiff files in " + basePath + "." );
            }
            if ( log.isDebugEnabled() ) {
                log.info("Got nearest raw info to coord " + viewerCoord[0] + "," + viewerCoord[1] + "," + viewerCoord[2] + " from base path " + basePath);
            }

            // Grab the channels.
            TifVolumeFileLoader loader = new TifVolumeFileLoader();
            if ( dimensions != null ) {
                loader.setOutputDimensions(dimensions);
            }
            loader.setConversionCharacteristics(
                    rawFileInfo.getTransformMatrix(),
                    rawFileInfo.getInvertedTransform(),
                    rawFileInfo.getMinCorner(),
                    rawFileInfo.getExtent(),
                    rawFileInfo.getQueryMicroscopeCoords()
            );

            loader.loadVolumeFile( rawFileInfo.getChannel0().getAbsolutePath() );
            rtnVal.put( 0, loader.getTextureByteArray() );

            loader.loadVolumeFile( rawFileInfo.getChannel1().getAbsolutePath() );
            rtnVal.put( 1, loader.getTextureByteArray() );

        } catch ( Exception ex ) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
        return rtnVal;
    }

    public CoordinateToRawTransform getTransform( String basePath ) throws DaoException {
        try {
            RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher( basePath );
            return fetcher.getTransform();
        } catch ( Exception ex ) {
            throw new DaoException(ex);
        }
    }

    private RawFileInfo getNearestFileInfo( String basePath, int[] viewerCoord ) throws DaoException {
        RawFileInfo rtnVal = null;
        try {
            RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher( basePath );
            rtnVal = fetcher.getNearestFileInfo( viewerCoord );
        } catch ( Exception ex ) {
            throw new DaoException(ex);
        }
        return rtnVal;
    }
    
    /**
     * fix connectivity issues for all neurons in a workspace
     */
    private void fixConnectivityWorkspace(Long workspaceID) throws DaoException {
        // remember, can't load workspace object, because that's what we're fixing!
        Entity entity = annotationDAO.getEntityById(workspaceID);
        for (TmNeuronDescriptor neuronDescriptor: getNeuronsForWorkspace(workspaceID, entity.getOwnerKey())) {
            fixConnectivityNeuron(neuronDescriptor.getId());
        }
    }

    /**
     * fix connectity issues for a neuron (bad parents, since children
     * aren't stored in the entity data); fix in this case means breaking links
     */
    private void fixConnectivityNeuron(Long neuronID) throws DaoException {
        // remember, can't load neuron or workspace objects, because those are what we're fixing!

        log.info("attempting to fix connectivity for neuron " + neuronID);

        // first we need to load all annotations (just like TmNeuron does);
        //  then we can loop over them again and check for missing parents/children
        ArrayList<EntityData> annList = new ArrayList<>();
        HashSet<Long> parentSet = new HashSet<>();
        Entity neuronEntity = annotationDAO.getEntityById(neuronID);
        for (EntityData ed: neuronEntity.getEntityData()) {
            String attributeName = ed.getEntityAttrName();
            if (attributeName.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE) ||
                attributeName.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                parentSet.add(ed.getId());
                // no need to check roots; they are children of neurons
                if (attributeName.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                    annList.add(ed);
                }
            }
        }

        for (EntityData ed: annList) {
            // I really don't get why we launder everything to DaoException, but
            //  that seems to be the pattern:
            TmGeoAnnotation annotation;
            try {
                annotation = new TmGeoAnnotation(ed);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DaoException(e);
            }
            if (annotation != null && !parentSet.contains(annotation.getParentId())) {
                log.info(annotation.getId() + " had missing parent; promoted to root");

                // when a missing parent is found:
                //     edit value: set parent ID to neuron ID
                //     edit attribute name: set to root not tree
                String valueString=TmGeoAnnotation.toStringFromArguments(annotation.getId(),
                    neuronID, annotation.getIndex(), annotation.getX(), annotation.getY(),
                    annotation.getZ(), annotation.getComment());
                ed.setValue(valueString);
                ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
                annotationDAO.saveOrUpdate(ed);
            }
        }
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws DaoException {
        try {
            Long sampleID = null;
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);            
            Entity sampleEntity = null;
            if (workspaceEntity != null) {
                EntityData sampleEd = workspaceEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
                if (sampleEd == null) {
                    throw new Exception("workspace " + workspaceEntity.getName() + " has no associated brand sample!");
                } else {
                    sampleID = Long.valueOf(sampleEd.getValue());
                    sampleEntity = annotationDAO.getEntityById(sampleID);
                }
            }

            // see notes in TmNeuron() on the connectivity retry scheme
            TmWorkspace workspace = null;
            boolean connectivityException;
            try {
                workspace = new TmWorkspace(workspaceEntity, sampleEntity);
                connectivityException = false;
            }
            catch (TmConnectivityException e) {
                e.printStackTrace();
                connectivityException = true;
            }
            if (connectivityException) {
                fixConnectivityWorkspace(workspaceId);
                workspaceEntity = annotationDAO.getEntityById(workspaceId);
                workspace = new TmWorkspace(workspaceEntity, sampleEntity);
            }
            return workspace;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmNeuron loadNeuron(Long neuronId) throws DaoException {
        try {
            Entity neuronEntity = annotationDAO.getEntityById(neuronId);
            TmNeuron neuron = null;
            // fixing potential connectivity errors is inelegant; we try
            //  once, then let the exception bubble up the second time;
            //  I did the if-statement because I was worried about
            //  putting the retry in the catch, for fear of infinite recursion
            boolean connectivityException;
            try {
                neuron = new TmNeuron(neuronEntity);
                connectivityException = false;
            }
            catch (TmConnectivityException e) {
                connectivityException = true;
            }
            if (connectivityException) {
                fixConnectivityNeuron(neuronId);
                neuronEntity = annotationDAO.getEntityById(neuronId);
                neuron = new TmNeuron(neuronEntity);
            }
            return neuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

}
