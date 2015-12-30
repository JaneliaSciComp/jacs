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
        if (workspace == null || name == null) {
            throw new Exception("Tiled Neuron must be created in a valid workspace.");
        }

        final String ownerKey = workspace.getOwnerKey();
        TmNeuron tmNeuron = new TmNeuron();
        tmNeuron.setId(null);
        tmNeuron.setOwnerKey(ownerKey);
        tmNeuron.setName(name);
        tmNeuron.setCreationDate(new Date());
        tmNeuron.setWorkspaceId(workspace.getId());
        saveNeuronData(tmNeuron);
        
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

        tmNeuron.getRootAnnotations().add(rtnVal);
        tmNeuron.getGeoAnnotationMap().put(rtnVal.getId(), rtnVal);

        saveNeuronData(tmNeuron);
        return rtnVal;        
    }

    /**
     * Change the parentage of the annotation to the new annotation ID.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param annotation this gets different parent.
     * @param newParentAnnotationID this becomes the new parent.
     * @param oldTmNeuron the annotation and new parent must be under this neuron.
     * @throws Exception thrown if condition(s) above not met.
     */
    public void reparentGeometricAnnotation(
            TmGeoAnnotation annotation, Long newParentAnnotationID,
            TmNeuron oldTmNeuron
    ) throws Exception {
        
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
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

        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation : tmNeuron.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
        }
        
        // Change parent ID.
        annotation.setParentId(newParentAnnotationID);
        saveNeuronData(tmNeuron);
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
	
	public void updateStructuredTextAnnotation(TmNeuron neuron, TmStructuredTextAnnotation annotation, String jsonString) throws Exception {
		neuron.getStructuredTextAnnotationMap().put(annotation.getId(), annotation);
	}
	
	public void deleteStructuredTextAnnotation(TmNeuron neuron, long annotationId) {
		if (neuron.getStructuredTextAnnotationMap().containsKey(annotationId)) {
			neuron.getStructuredTextAnnotationMap().remove(annotationId);
		}
	}
    
    public void rerootNeurite(TmNeuron oldTmNeuron, TmGeoAnnotation newRoot) throws Exception {
        if (newRoot == null || oldTmNeuron == null) {
            return;
        }
                
        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
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
        //TmGeoAnnotation oldRoot = testAnnotation;
        parentList.add(testAnnotation);
        testAnnotation.setParentId(tmNeuron.getId());

        // reparent intervening annotations; skip the first item, which is the
        //  new root (which we've already dealt with)
        for (int i = 1; i < parentList.size(); i++) {
            // change the parent ID and save
            TmGeoAnnotation ann = parentList.get(i);
            Long newParentAnnotationID = parentList.get(i - 1).getId();
            ann.setParentId(newParentAnnotationID);
        }
        
        // Make sure the neuron knows about its new root.
        tmNeuron.getRootAnnotations().add( newRoot );
        saveNeuronData(tmNeuron);
    }
    
    public void splitNeurite(TmNeuron oldTmNeuron, TmGeoAnnotation newRoot) throws Exception {
        
        if (newRoot == null || oldTmNeuron == null) {
            return;
        }

        TmNeuron tmNeuron = refreshFromData(oldTmNeuron);
        if (!tmNeuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    tmNeuron.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been 
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // Ensure neuron knows this root; reset its parent
        //  to the neuron (as one does for a root)
        newRoot.setParentId(tmNeuron.getId());
        tmNeuron.getRootAnnotations().add( newRoot );
        
    }    

    /**
     * Moves the annotation, and its tree, from old to new neuron.
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

        TmNeuron oldTmNeuron = refreshFromData(inMemOldTmNeuron);
        TmNeuron newTmNeuron = refreshFromData(inMemNewTmNeuron);

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
            ann.setParentId(newTmNeuron.getId());            
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
        saveNeuronData(newTmNeuron);
        saveNeuronData(oldTmNeuron);

    }
    
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(TmWorkspace tmWorkspace) throws Exception {
        // Validate sample
        if (tmWorkspace == null) {
            throw new Exception("Neurons must be parented with valid Workspace Id");
        }
        List<TmNeuronDescriptor> descriptorList = new ArrayList<>();
        for (TmNeuron tmNeuron: tmWorkspace.getNeuronList()) {
            TmNeuronDescriptor descriptor = new TmNeuronDescriptor(
                    tmNeuron.getId(), tmNeuron.getName(), tmNeuron.getGeoAnnotationMap().size() + tmNeuron.getRootAnnotations().size()
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
            tmNeuron.getRootAnnotations().remove(tmAnno);
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
