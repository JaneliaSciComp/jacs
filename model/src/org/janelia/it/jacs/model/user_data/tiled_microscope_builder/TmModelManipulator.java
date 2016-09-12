/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_builder;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

/**
 * Manages the relationships, additions and deletions from Tiled Microscope
 * data, based at a Workspace.
 *
 * Implementation note: be careful regarding neuron persistance!  Some methods
 * in this class persist neurons, and others do not (letting the calling
 * routine persist).  You should only persist neurons exactly once, after all
 * operations are complete.
 *
 * @author fosterl
 */
public class TmModelManipulator {
    private IdSource idSource = new IdSource();
    private TmModelAdapter dataSource;
    private Logger log = Logger.getLogger(TmModelManipulator.class);

    // NOTE: workspaces are still stored as entities.  As such, they should
    // be created in a DAO.
    
    
    public TmModelManipulator(TmModelAdapter dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * The workspace's neurons will not have been loaded with the workspace,
     * because we wish to be able to load them from any client, including
     * one which happens to be on the server.
     */
    public void loadWorkspaceNeurons(TmWorkspace workspace) throws Exception {
        dataSource.loadNeurons(workspace);
    }
    
    /**
     * Makes a new neuron.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param workspace will contain this neuron.
     * @param name of new neuron.
     * @return that neuron
     * @throws Exception 
     */
    public TmNeuron createTiledMicroscopeNeuron(TmWorkspace workspace, String name) throws Exception {
        return createTiledMicroscopeNeuron(workspace, name, null);
    }
    
    /**
     * Makes a new neuron.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param workspace will contain this neuron.
     * @param name of new neuron.
     * @return that neuron
     * @throws Exception 
     */
    public TmNeuron createTiledMicroscopeNeuron(TmWorkspace workspace, String name, Long precomputedId) throws Exception {
        if (workspace == null || name == null) {
            throw new Exception("Tiled Neuron must be created in a valid workspace.");
        }

        final String ownerKey = workspace.getOwnerKey();
        TmNeuron tmNeuron = new TmNeuron();
        tmNeuron.setId(precomputedId);
        tmNeuron.setOwnerKey(ownerKey);
        tmNeuron.setName(name);
        tmNeuron.setCreationDate(new Date());
        tmNeuron.setWorkspaceId(workspace.getId());
        if (dataSource != null) {
            saveNeuronData(tmNeuron);
        }
        
        return tmNeuron;        
    }
    
    /**
     * Add an anchored path to an existing neuron.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param tmNeuron add path to this.
     * @param annotationID1 end point annotations.
     * @param annotationID2
     * @param pointlist
     * @return the anchored path thus created.
     * @throws Exception 
     */
    public TmAnchoredPath addAnchoredPath(TmNeuron tmNeuron, Long annotationID1, Long annotationID2,
            List<List<Integer>> pointlist) throws Exception {
        if (!tmNeuron.getGeoAnnotationMap().containsKey(annotationID1)
                || !tmNeuron.getGeoAnnotationMap().containsKey(annotationID2)) {
            throw new Exception("both annotations must be in neuron");
        }
        final TmAnchoredPathEndpoints key = new TmAnchoredPathEndpoints(annotationID1, annotationID2);
        final TmAnchoredPath value = new TmAnchoredPath( idSource.next(), key, pointlist );
        tmNeuron.getAnchoredPathMap().put(key,value);
        saveNeuronData(tmNeuron);
        return value;        
    }

    /**
     * Geometric annotations are essentially points in the growing neuron.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param tmNeuron will receive this point.
     * @param parentAnnotationId linkage.
     * @param index offset counter.
     * @param x cords...
     * @param y
     * @param z
     * @param comment may be null.
     * @return the completed point-rep.
     * @throws Exception 
     */
    public TmGeoAnnotation addGeometricAnnotation(
            TmNeuron tmNeuron, Long parentAnnotationId, int index,
            double x, double y, double z, 
            String comment
    ) throws Exception {
        TmGeoAnnotation rtnVal = new TmGeoAnnotation();
        rtnVal.setX(x);
        rtnVal.setY(y);
        rtnVal.setZ(z);
        rtnVal.setIndex(index);
        rtnVal.setCreationDate(new Date());
        rtnVal.setComment(comment);
        rtnVal.setNeuronId(tmNeuron.getId());
        rtnVal.setParentId(parentAnnotationId);
        rtnVal.setId(idSource.next());

        // If non-root, add this as a child of its parent.
        if (parentAnnotationId != null) {
            TmGeoAnnotation parent = tmNeuron.getGeoAnnotationMap().get(parentAnnotationId);
            // Parent might be the neuron itself, if this is a root.
            // Otherwise, ensure the inter-annotation linkage.
            if (parent != null) {
                parent.addChild(rtnVal);
            }
        }
		// Handle root geo annotations.
		if (parentAnnotationId == null  ||  parentAnnotationId == tmNeuron.getId()) {
	        tmNeuron.addRootAnnotation(rtnVal);
		}

        tmNeuron.getGeoAnnotationMap().put(rtnVal.getId(), rtnVal);

        saveNeuronData(tmNeuron);
        // Ensure that the geo-annotation known to the neuron, after the
        // save, is the one we return.  Get the other one out of circulation.
        rtnVal = tmNeuron.getGeoAnnotationMap().get(rtnVal.getId());
        return rtnVal;        
    }

    /**
     * Change the parentage of the annotation to the new annotation ID.  This
     * operation is partial.  It can be applied to annotations which are
     * incomplete.  Therefore, this operation does not carry out exchange with
     * the database.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param annotation this gets different parent.
     * @param newParentAnnotationID this becomes the new parent.
     * @param tmNeuron the annotation and new parent must be under this neuron.
     * @throws Exception thrown if condition(s) above not met.
     */
    public void reparentGeometricAnnotation(
            TmGeoAnnotation annotation, Long newParentAnnotationID,
            TmNeuron tmNeuron
    ) throws Exception {
        
        // verify that both annotations are in the input neuron
        if (!tmNeuron.getGeoAnnotationMap().containsKey(annotation.getId())) {
            throw new Exception("input neuron doesn't contain child annotation " + annotation.getId());
        }
        if (!tmNeuron.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new Exception("input neuron doesn't contain new parent annotation " + newParentAnnotationID);
        }
        
        // is it already the parent?
        if (annotation.getParentId().equals(newParentAnnotationID)) {
            return;
        }
        
        // Ensure: using the very same copy that is known to the neuron.
        annotation = tmNeuron.getGeoAnnotationMap().get(annotation.getId());
        
        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation : tmNeuron.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
        }
        
        // The reparented annotation will no longer be a root.
        if (annotation.isRoot()) {
            tmNeuron.removeRootAnnotation(annotation);
        }

        // Change child/down linkage.
        TmGeoAnnotation parentAnnotation = tmNeuron.getParentOf(annotation);
        if (parentAnnotation != null) {
            parentAnnotation.getChildIds().remove(annotation.getId());
        }
        // Change parent ID.
        annotation.setParentId(newParentAnnotationID);
        TmGeoAnnotation newParentAnnotation = tmNeuron.getGeoAnnotationMap().get(newParentAnnotationID);
        // Belt-and-suspenders: above tests that the map has this ID.
        if (newParentAnnotation != null) {
            newParentAnnotation.getChildIds().add(annotation.getId());
        }
    }

    // @todo may need to add create, update dates + ownerKey
    public TmStructuredTextAnnotation addStructuredTextAnnotation(TmNeuron neuron, Long parentID, int parentType, int formatVersion,
            String data) throws Exception {
        
        // parent must be neuron or geoann:
        if (parentType != TmStructuredTextAnnotation.GEOMETRIC_ANNOTATION
                && parentType != TmStructuredTextAnnotation.NEURON) {
            throw new Exception("parent must be a geometric annotation or a neuron");
        }

        // parent must not already have a structured text annotation
        if (neuron.getStructuredTextAnnotationMap().containsKey(parentID)) {
            throw new Exception("parent ID already has a structured text annotation; use update, not add");
        }

        TmStructuredTextAnnotation annotation = new TmStructuredTextAnnotation(
                idSource.next(), parentID, parentType, data
        );
        
        neuron.getStructuredTextAnnotationMap().put( parentID, annotation );

        return annotation;
    }
	
	public void updateStructuredTextAnnotation(TmNeuron neuron, TmStructuredTextAnnotation annotation, String jsonString) throws Exception {
        // note for the future: this method and the delete one below ought to be just
        //  in-lined in AnnotationModel; maybe the add method above, too
        annotation.setDataString(jsonString);
		neuron.getStructuredTextAnnotationMap().put(annotation.getParentId(), annotation);
    }
	
	public void deleteStructuredTextAnnotation(TmNeuron neuron, long annotationId) {
		if (neuron.getStructuredTextAnnotationMap().containsKey(annotationId)) {
			neuron.getStructuredTextAnnotationMap().remove(annotationId);
		}
    }

    // for debugging, if needed
    private void printStructureTextAnnotationMap(TmNeuron neuron) {
        Map<Long, TmStructuredTextAnnotation> staMap = neuron.getStructuredTextAnnotationMap();
        System.out.println("structure text map for neuron " + neuron.getName() + ", neuron ID " + neuron.getId());
        for (Long annID: staMap.keySet()) {
            TmStructuredTextAnnotation sta = staMap.get(annID);
            System.out.println("    key:" + annID + " str text ID: " + sta.getId() + " parent ID: "  + annID +
                " parent type: " + sta.getParentType() + " data string:" + sta.getDataString());
        }
    }

    /**
     * This can be a partial operation.  Therefore, it will not carry out any
     * communication with the databse.
     * 
     * @param tmNeuron gets a new root.
     * @param newRoot becomes a root, and added to neuron.
     * @throws Exception 
     */
    public void rerootNeurite(TmNeuron tmNeuron, TmGeoAnnotation newRoot) throws Exception {
        if (newRoot == null || tmNeuron == null) {
            return;
        }
                
        if (!tmNeuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    tmNeuron.getId(), newRoot.getId()));
        }

        // is it already a root?
        if (newRoot.isRoot()) {
            return;
        }

        // from input, follow parents up to current root, keeping them all
        List<TmGeoAnnotation> parentList = new ArrayList<>();
        TmGeoAnnotation testAnnotation = newRoot;
        while (!testAnnotation.isRoot()) {
            parentList.add(testAnnotation);
            testAnnotation = tmNeuron.getParentOf(testAnnotation);            
        }
        parentList.add(testAnnotation);
        Long neuronId = tmNeuron.getId();
        testAnnotation.setNeuronId(neuronId);        

        // reparent intervening annotations; skip the first item, which is the
        //  new root (which we've already dealt with)
        log.info("Inverting child/parent relationships.");
        for (int i = 1; i < parentList.size(); i++) {
            // change the parent ID/add as child, and save
            TmGeoAnnotation ann = parentList.get(i);

            // Remove from old parent.
            TmGeoAnnotation oldParentAnnotation = tmNeuron.getParentOf(ann);
            if (oldParentAnnotation != null) {
                oldParentAnnotation.getChildIds().remove(ann.getId());
            }
            else if (ann.isRoot()) {
                tmNeuron.removeRootAnnotation(ann);
            }
            
            Long newParentAnnotationID = parentList.get(i - 1).getId();
            ann.setParentId(newParentAnnotationID);
            // If it is now the parent, it cannot any longer be a child.
            ann.getChildIds().remove(newParentAnnotationID);
            ann.setNeuronId(neuronId);
            
            // Add to new parent.
            TmGeoAnnotation newParentAnnotation = tmNeuron.getGeoAnnotationMap().get(newParentAnnotationID);
            newParentAnnotation.addChild(ann);            
        }
        
        // Finally, make this thing a root.
        makeRoot(tmNeuron, newRoot);
    }
    
    public void splitNeurite(TmNeuron tmNeuron, TmGeoAnnotation newRoot) throws Exception {        
        if (newRoot == null || tmNeuron == null) {
            return;
        }

        if (!tmNeuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    tmNeuron.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been 
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // Remove this new root as a child of its parent.
        TmGeoAnnotation oldParent = tmNeuron.getParentOf(newRoot);
        if (oldParent != null) {
            oldParent.getChildIds().remove(newRoot.getId());
        }
        // Ensure neuron knows this root; reset its parent
        //  to the neuron (as one does for a root).
        if (tmNeuron.getId() == null) {
            log.error("Neuron ID= null.  Setting annotation as root will fail.");
        }
        makeRoot(tmNeuron, newRoot);
    }    
    
    /**
     * Moves the annotation and its tree from source to destination neuron.
     * Does not refresh from database, or save to database.
     * 
     * @param annotation this will be moved.
     * @param oldTmNeuron this is the current (source) container of the annotation.
     * @param newTmNeuron this is the destination container of the annotation.
     * @throws Exception thrown by called methods.
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron oldTmNeuron, TmNeuron newTmNeuron) throws Exception {
        long newNeuronId = newTmNeuron.getId();

        // already same neuron?  done!
        if (oldTmNeuron.getId().equals(newNeuronId)) {
            return;
        }

        // Find the root annotation.  Ultimate parent of the annotation.
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldTmNeuron.getParentOf(rootAnnotation);
        }

        // Move all the geo-annotations from the old to the new neuron.
        Map<Long,TmGeoAnnotation> movedAnnotationIDs = new HashMap<>();
        final Map<Long, TmStructuredTextAnnotation> oldStructuredTextAnnotationMap = oldTmNeuron.getStructuredTextAnnotationMap();
        final Map<Long, TmStructuredTextAnnotation> newStructuredTextAnnotationMap = newTmNeuron.getStructuredTextAnnotationMap();
        for (TmGeoAnnotation ann: oldTmNeuron.getSubTreeList(rootAnnotation)) {
            movedAnnotationIDs.put(ann.getId(), ann);
            ann.setNeuronId(newNeuronId);
            newTmNeuron.getGeoAnnotationMap().put(ann.getId(), ann);
            // move any TmStructuredTextAnnotations as well:
            if (oldStructuredTextAnnotationMap.containsKey(ann.getId())) {
                TmStructuredTextAnnotation note = oldStructuredTextAnnotationMap.get(ann.getId());
                oldStructuredTextAnnotationMap.remove(ann.getId());
                newStructuredTextAnnotationMap.put(ann.getId(), note);
            }

        }
        
        // loop over anchored paths; if endpoints are in set of moved annotations,
        //  move the path as well
        Map<TmAnchoredPathEndpoints,TmAnchoredPath> oldNeuronAnchoredPathMap = oldTmNeuron.getAnchoredPathMap();
        Map<TmAnchoredPathEndpoints,TmAnchoredPath> newNeuronAnchoredPathMap = newTmNeuron.getAnchoredPathMap();

        Iterator<TmAnchoredPathEndpoints> iter = oldNeuronAnchoredPathMap.keySet().iterator();
        while(iter.hasNext()) {
            TmAnchoredPathEndpoints endpoints = iter.next();
            // both endpoints are necessarily in the same neurite, so only need
            //  to test one:
            if (movedAnnotationIDs.containsKey(endpoints.getFirstAnnotationID())) {
                TmAnchoredPath anchoredPath = oldNeuronAnchoredPathMap.get(endpoints);
                iter.remove();
                // TmAnchoredPath anchoredPath = oldNeuronAnchoredPathMap.remove(endpoints);
                newNeuronAnchoredPathMap.put(endpoints, anchoredPath);
            }
        }


        // Need to remove all these annotations from the old map, after
        // iteration through the map above, to avoid concurrent modification.
        for (Long movedAnnotationID: movedAnnotationIDs.keySet()) {
            oldTmNeuron.getGeoAnnotationMap().remove(movedAnnotationID);
        }
        
        // if it's the root, also change its parent annotation to the new neuron
        rootAnnotation.setParentId(newTmNeuron.getId());
        oldTmNeuron.removeRootAnnotation(rootAnnotation);
        newTmNeuron.addRootAnnotation(rootAnnotation);
    }
    
    private static final String UNREMOVE_NEURON_WARN_FMT = "Attempted to remove neuron %d that was not in workspace %d.";
    public void deleteNeuron(TmWorkspace tmWorkspace, TmNeuron tmNeuron) throws Exception {
        boolean wasRemoved = tmWorkspace.getNeuronList().remove(tmNeuron);
        if (wasRemoved) {
            // Need to signal to DB that this entitydata must be deleted.
            deleteNeuronData(tmNeuron);
        }
        else {
            log.warn(String.format(UNREMOVE_NEURON_WARN_FMT, tmNeuron.getId(), tmWorkspace.getId()));
        }
    }

    public void saveNeuronData(TmNeuron neuron) throws Exception {
        dataSource.saveNeuron(neuron);
    }

    /** Encapsulating all steps linking an annotation as a root. */
    private void makeRoot(TmNeuron tmNeuron, TmGeoAnnotation newRoot) {
        if (tmNeuron.getId() == null) {
            log.warn("Attempting to set null parent from null neuron-id.");
        }
        newRoot.setParentId(tmNeuron.getId());
		if (! tmNeuron.containsRootAnnotation( newRoot )) {
			tmNeuron.addRootAnnotation( newRoot );
		}        
    }

    private void deleteNeuronData(TmNeuron neuron) throws Exception {
        dataSource.deleteEntityData(neuron);
    }

    /**
     * Given a collection of annotations, under a common neuron, make
     * annotations for each in the database, preserving the linkages implied in
     * the "value" target of the map provided.
     *
     * @param annotations map of node offset id vs "unserialized" annotation.
     * @param nodeParentLinkage map of node offset id vs parent node offset id.
     */
    public void addLinkedGeometricAnnotationsInMemory(
            Map<Integer, Integer> nodeParentLinkage,
            Map<Integer, TmGeoAnnotation> annotations,
            TmNeuron tmNeuron,
            Iterator<Long> idSource) {
        Long neuronId = tmNeuron.getId();
        int putativeRootCount = 0;
        // Cache to avoid re-fetch.
        Map<Integer, Long> nodeIdToAnnotationId = new HashMap<>();
        // Ensure the order of progression through nodes matches node IDs.
        Set<Integer> sortedKeys = new TreeSet<>(annotations.keySet());
        for (Integer nodeId : sortedKeys) {
            boolean isRoot = false;
            TmGeoAnnotation unlinkedAnnotation = annotations.get(nodeId);

            // Establish node linkage.
            Integer parentIndex = nodeParentLinkage.get(nodeId);
            Long parentAnnotationId = null;
            if (parentIndex != null && parentIndex != -1) {
                // NOTE: unless the annotation has been processed as
                // below, prior to now, the parent ID will be null.
                parentAnnotationId = nodeIdToAnnotationId.get(parentIndex);
                if (parentAnnotationId == null) {
                    parentAnnotationId = neuronId;
                }
            } else {
                putativeRootCount++;
                parentAnnotationId = neuronId;
                isRoot = true;
            }

            // Make the actual annotation, and save its linkage
            // through its original node id.
            TmGeoAnnotation linkedAnnotation = createGeometricAnnotationInMemory(tmNeuron, isRoot, parentAnnotationId, unlinkedAnnotation, idSource);
            TmGeoAnnotation parentAnnotation = tmNeuron.getParentOf(linkedAnnotation);
            if (parentAnnotation != null) {
                parentAnnotation.addChild(linkedAnnotation);
            }
            nodeIdToAnnotationId.put(nodeId, linkedAnnotation.getId());

            log.trace("Node " + nodeId + " at " + linkedAnnotation.toString() + ", has id " + linkedAnnotation.getId()
                    + ", has parent " + linkedAnnotation.getParentId() + ", under neuron " + linkedAnnotation.getNeuronId());
        }

        if (putativeRootCount > 1) {
            log.warn("Number of nodes with neuron as parent is " + putativeRootCount);
        }
    }

    public void addLinkedGeometricAnnotationsInMemory(
            Map<Integer, Integer> nodeParentLinkage,
            Map<Integer, TmGeoAnnotation> annotations,
            TmNeuron tmNeuron) {
        addLinkedGeometricAnnotationsInMemory(nodeParentLinkage, annotations, tmNeuron, idSource);
    }

    private TmGeoAnnotation createGeometricAnnotationInMemory(TmNeuron neuron, boolean isRoot, Long parentAnnotationId, TmGeoAnnotation unserializedAnno, Iterator<Long> idSource) {
        return createGeometricAnnotationInMemory(neuron, isRoot, parentAnnotationId, 0, unserializedAnno.getX(), unserializedAnno.getY(), unserializedAnno.getZ(), unserializedAnno.getRadius(), unserializedAnno.getComment(), neuron.getId(), idSource);
    }

    private TmGeoAnnotation createGeometricAnnotationInMemory(
            TmNeuron tmNeuron, boolean isRoot, Long parentAnnotationId, int index, double x, double y, double z, double radius, String comment, Long neuronId, Iterator<Long> idSource) {

        long generatedId = idSource.next();
        TmGeoAnnotation geoAnnotation = new TmGeoAnnotation(generatedId, comment, x, y, z, parentAnnotationId, new Date());
        geoAnnotation.setNeuronId(neuronId);
        geoAnnotation.setIndex(index);
        geoAnnotation.setRadius(radius);
        tmNeuron.getGeoAnnotationMap().put(geoAnnotation.getId(), geoAnnotation);
        if (isRoot) {
            tmNeuron.addRootAnnotation(geoAnnotation);
        }
        return geoAnnotation;
    }

}
