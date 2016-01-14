/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_builder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuronDescriptor;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmStructuredTextAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

/**
 * Manages the relationships, additions and deletions from Tiled Microscope
 * data, based at a Workspace.
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
    public TmStructuredTextAnnotation addStructuredTextAnnotation(TmNeuron oldTmNeuron, Long parentID, int parentType, int formatVersion,
            String data) throws Exception {
        
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
        // parent must be neuron or geoann:
        if (parentType != TmStructuredTextAnnotation.GEOMETRIC_ANNOTATION
                && parentType != TmStructuredTextAnnotation.NEURON) {
            throw new Exception("parent must be a geometric annotation or a neuron");
        }

        // parent must not already have a structured text annotation
        if (tmNeuron.getStructuredTextAnnotationMap().containsKey(parentID)) {
            throw new Exception("parent ID already has a structured text annotation; use update, not add");
        }

        TmStructuredTextAnnotation annotation = new TmStructuredTextAnnotation(
                idSource.next(), parentID, parentType, data
        );
        
        tmNeuron.getStructuredTextAnnotationMap().put( parentID, annotation );

        return annotation;
    }
	
	public void updateStructuredTextAnnotation(TmNeuron neuron, TmGeoAnnotation geoAnnotation, TmStructuredTextAnnotation textAnnotation) throws Exception {
		neuron.getStructuredTextAnnotationMap().put(geoAnnotation.getId(), textAnnotation);
	}
	
	public void deleteStructuredTextAnnotation(TmNeuron neuron, long annotationId) {
		if (neuron.getStructuredTextAnnotationMap().containsKey(annotationId)) {
			neuron.getStructuredTextAnnotationMap().remove(annotationId);
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
     * Moves the annotation, and its tree, from old to new neuron.  This is a
     * complete operation, which refreshes from database, and flushes back
     * to database.
     * 
     * @todo ensure that the new neuron is available at call time.
     * @param annotation this will be moved.
     * @param inMemOldTmNeuron this is the current container of the annotation.
     * @param inMemNewTmNeuron this will be the container of the annotation.
     * @throws Exception thrown by called methods.
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron inMemOldTmNeuron, TmNeuron inMemNewTmNeuron) throws Exception {
        // already in the neuron?  we're done
        if (inMemNewTmNeuron.getId() == inMemOldTmNeuron.getId()) {
            return;
        }
        
        log.debug("Moving from old neuron " + System.identityHashCode(inMemOldTmNeuron) + " to " + System.identityHashCode(inMemNewTmNeuron));

        TmNeuron oldTmNeuron = refreshFromData(inMemOldTmNeuron);
        TmNeuron newTmNeuron = refreshFromData(inMemNewTmNeuron);
        log.debug("Refreshed old neuron " + System.identityHashCode(inMemOldTmNeuron) + ", refreshed new neuron " + System.identityHashCode(inMemNewTmNeuron));
        
        moveNeuriteInMem(annotation, oldTmNeuron, newTmNeuron);
        saveNeuronData(newTmNeuron);
        saveNeuronData(oldTmNeuron);                
    }
    
    /**
     * Moves the annotation, and its tree, from old to new neuron.  Do
     * not refresh from database, or save to database.  This is a partial
     * operation.
     * 
     * @todo ensure that the new neuron is available at call time.
     * @param annotation this will be moved.
     * @param inMemOldTmNeuron this is the current container of the annotation.
     * @param inMemNewTmNeuron this will be the container of the annotation.
     * @throws Exception thrown by called methods.
     */
    public void moveNeuriteInMem(TmGeoAnnotation annotation, TmNeuron oldTmNeuron, TmNeuron newTmNeuron) throws Exception {
        long newNeuronId = newTmNeuron.getId();

        // Find the root annotation.  Ultimate parent of the annotation.
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldTmNeuron.getParentOf(rootAnnotation);
        }

        // DEBUG: find out if subtree list is accurate.
        //List<TmGeoAnnotation> debug = oldTmNeuron.getSubTreeList(rootAnnotation);
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
        for (TmAnchoredPathEndpoints endpoints : oldTmNeuron.getAnchoredPathMap().keySet()) {
            // both endpoints are necessarily in the same neurite, so only need
            //  to test one:
            if (movedAnnotationIDs.containsKey(endpoints.getAnnotationID1())) {
                TmAnchoredPath anchoredPath = oldNeuronAnchoredPathMap.remove(endpoints);
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
    
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(TmWorkspace tmWorkspace) throws Exception {
        // Validate sample
        if (tmWorkspace == null) {
            throw new Exception("Neurons must be parented with valid Workspace Id");
        }
        List<TmNeuronDescriptor> descriptorList = new ArrayList<>();
        for (TmNeuron tmNeuron: tmWorkspace.getNeuronList()) {
            TmNeuronDescriptor descriptor = new TmNeuronDescriptor(
                    tmNeuron.getId(), tmNeuron.getName(), tmNeuron.getGeoAnnotationMap().size() + tmNeuron.getRootAnnotationCount()
            );
            descriptorList.add(descriptor);
        }
        return descriptorList;
    }    

    /**
     * Given the neuron containing the path, and the path itself, remove the
     * path from the neuron.
     * 
     * @param oldTmNeuron container.
     * @param path content to remove.
     * @throws Exception 
     */
    public void deleteAnchoredPath(TmNeuron oldTmNeuron, TmAnchoredPath path) throws Exception {
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
        // Remove the anchor path from its containing neuron        
        tmNeuron.getAnchoredPathMap().remove(path.getEndpoints());
        saveNeuronData(tmNeuron);
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

    public TmNeuron refreshFromData(TmNeuron neuron) throws Exception {
        return dataSource.refreshFromEntityData(neuron);
    }

    private static final String UNREMOVE_GEO_ANNO_WARN_FMT = "Attempted to remove geo-annotation %d that was not in neuron %d.";
    public void deleteGeometricAnnotation(TmNeuron oldTmNeuron, TmGeoAnnotation tmAnno) throws Exception {
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
        TmGeoAnnotation removedAnno = tmNeuron.getGeoAnnotationMap().remove(tmAnno.getId());
        if (removedAnno != null) {
            // Only removed, if it actually existed in the list.
            tmNeuron.removeRootAnnotation(tmAnno);
            saveNeuronData(tmNeuron);
        }
        else {
            log.warn(String.format(UNREMOVE_GEO_ANNO_WARN_FMT, tmAnno.getId(), tmNeuron.getId()));
        }
    }

    private static final String UNREMOVE_TEX_ANNO_WARN_FMT = "Attempted to remove structured-text-annotation %d that was not in neuron %d.";
    public void deleteStructuredText(TmNeuron oldTmNeuron, TmStructuredTextAnnotation tmStrucTextAnno) throws Exception {
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
        
        TmStructuredTextAnnotation removedAnno = tmNeuron.getStructuredTextAnnotationMap().remove(tmStrucTextAnno.getId());
        if (removedAnno != null) {
           saveNeuronData(tmNeuron);
        }
        else {
            log.warn(String.format(UNREMOVE_TEX_ANNO_WARN_FMT, tmStrucTextAnno.getId(), tmNeuron.getId()));
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

    /**
     * fix connectivity issues for all neurons in a workspace
     */
    private void fixConnectivityWorkspace(TmWorkspace tmWorkspace) throws Exception {
        // remember, can't load workspace object, because that's what we're fixing!
        for (TmNeuron tmNeuron: tmWorkspace.getNeuronList()) {
            fixConnectivityNeuron(tmWorkspace, tmNeuron);
        }
    }

    /**
     * fix connectity issues for a neuron (bad parents, since children aren't
     * stored in the entity data); fix in this case means breaking links
     */
    private void fixConnectivityNeuron(TmWorkspace tmWorkspace, TmNeuron tmNeuron) throws Exception {
        // Not doing the refresh here: may corrupt an intermediate.
        final StringBuilder errorResults = new StringBuilder();
        // Check whether the end points are actually known to this neuron.
        final List<TmAnchoredPathEndpoints> toRemoveEP = new ArrayList<>();
        for (TmAnchoredPathEndpoints endPoint: tmNeuron.getAnchoredPathMap().keySet()) {
            if (tmNeuron.getGeoAnnotationMap().get(endPoint.getAnnotationID1()) == null  ||
                tmNeuron.getGeoAnnotationMap().get(endPoint.getAnnotationID2()) == null) {
                // Must discard this point.
                toRemoveEP.add(endPoint);
                errorResults
                        .append(endPoint)
                        .append(" removed from ")
                        .append(tmNeuron)
                        .append(" because its endpoints were not found among annotations.")
                        .append("\n");
            }
        }
        // Need do this in separate pass, to avoid concurrent-mod.
        for (TmAnchoredPathEndpoints endPoints: toRemoveEP) {
            tmNeuron.getAnchoredPathMap().remove(endPoints);
        }
        
        // Check whether the roots are really known annotations.
        final List<TmGeoAnnotation> toRepairRoots = new ArrayList<>();
        for (TmGeoAnnotation root: tmNeuron.getRootAnnotations()) {
            if (! tmNeuron.getGeoAnnotationMap().containsKey(root.getId())) {
                toRepairRoots.add(root);
                errorResults
                        .append(root)
                        .append(" repaired in ")
                        .append(tmNeuron)
                        .append(" because it was not found among annotations.")
                        .append("\n");
            }
        }        
        // Separate pass to avoid concurrent-mod.
        for (TmGeoAnnotation geo: toRepairRoots) {
            tmNeuron.getGeoAnnotationMap().put(geo.getId(), geo);
        }
        
        // Ensure parentage ids are properly established.
        for (TmGeoAnnotation geo: tmNeuron.getGeoAnnotationMap().values()) {
            if (geo.getNeuronId() != tmNeuron.getId()) {
                geo.setNeuronId( tmNeuron.getId() );
                errorResults
                        .append(geo)
                        .append(" had parentage corrected to ")
                        .append(tmNeuron)
                        .append(".\n");
            }
        }
        
    }

// RESERVE JUDGEMENT
//    public void addLinkedGeometricAnnotations(
//            Map<Integer, Integer> nodeParentLinkage,
//            Map<Integer, TmGeoAnnotation> annotations
//    ) throws Exception {
//        
//    }
    
    private void deleteNeuronData(TmNeuron neuron) throws Exception {
        dataSource.deleteEntityData(neuron);
    }
    
}
