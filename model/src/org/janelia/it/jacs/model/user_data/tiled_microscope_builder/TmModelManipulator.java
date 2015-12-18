/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_builder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        TmNeuron neuron = new TmNeuron();
        neuron.setId(idSource.next());
        neuron.setOwnerKey(ownerKey);
        neuron.setName(name);
        neuron.setCreationDate(new Date());
        workspace.getNeuronList().add(neuron);
        
        return neuron;        
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

        // If non-root, add this as a child of its parent.
        if (parentAnnotationId != null) {
            TmGeoAnnotation parent = tmNeuron.getGeoAnnotationMap().get(parentAnnotationId);
            parent.addChild(rtnVal);
        }
        
        return rtnVal;        
    }

    /**
     * Change the parentage of the annotation to the new annotation ID.
     * 
     * @todo may need to add create, update dates + ownerKey
     * @param annotation this gets different parent.
     * @param newParentAnnotationID this becomes the new parent.
     * @param neuron the annotation and new parent must be under this neuron.
     * @throws Exception thrown if condition(s) above not met.
     */
    public void reparentGeometricAnnotation(
            TmGeoAnnotation annotation, Long newParentAnnotationID,
            TmNeuron neuron
    ) throws Exception {
        // verify that both annotations are in the input neuron
        if (!neuron.getGeoAnnotationMap().containsKey(annotation.getId())) {
            throw new Exception("input neuron doesn't contain child annotation " + annotation.getId());
        }
        if (!neuron.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new Exception("input neuron doesn't contain new parent annotation " + newParentAnnotationID);
        }
        
        // is it already the parent?
        if (annotation.getParentId().equals(newParentAnnotationID)) {
            return;
        }

        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation : neuron.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
        }
        
        // Change parent ID.
        annotation.setParentId(newParentAnnotationID);

    }

    // @todo may need to add create, update dates + ownerKey
    public TmStructuredTextAnnotation addStructuredTextAnnotation(TmNeuron tmNeuron, Long parentID, int parentType, int formatVersion,
            String data) throws Exception {
        
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
    
    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws Exception {
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
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
            testAnnotation = neuron.getParentOf(testAnnotation);
        }
        //TmGeoAnnotation oldRoot = testAnnotation;
        parentList.add(testAnnotation);
        testAnnotation.setParentId(neuron.getId());

        // reparent intervening annotations; skip the first item, which is the
        //  new root (which we've already dealt with)
        for (int i = 1; i < parentList.size(); i++) {
            // change the parent ID and save
            TmGeoAnnotation ann = parentList.get(i);
            Long newParentAnnotationID = parentList.get(i - 1).getId();
            ann.setParentId(newParentAnnotationID);
        }
        
        // Make sure the neuron knows about its new root.
        neuron.getRootAnnotations().add( newRoot );
    }
    
    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws Exception {
        
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new Exception(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been 
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // Ensure neuron knows this root; reset its parent
        //  to the neuron (as one does for a root)
        newRoot.setParentId(neuron.getId());
        neuron.getRootAnnotations().add( newRoot );
        
    }    

    /**
     * Moves the annotation, and its tree, from old to new neuron.
     * 
     * @todo ensure that the new neuron is available at call time.
     * @param annotation this will be moved.
     * @param oldNeuron this is the current container of the annotation.
     * @param newNeuron this will be the container of the annotation.
     * @throws Exception thrown by called methods.
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron oldNeuron, TmNeuron newNeuron) throws Exception {
        // already in the neuron?  we're done
        if (newNeuron.getId() == oldNeuron.getId()) {
            //if (annotation.getParentId() == newNeuron.getId()) {
            return;
        }

        // Find the root annotation.  Ultimate parent of the annotation.
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldNeuron.getParentOf(rootAnnotation);
        }

        // Move all the geo-annotations from the old to the new neuron.
        Map<Long,TmGeoAnnotation> movedAnnotationIDs = new HashMap<>();
        final Map<Long, TmStructuredTextAnnotation> oldStructuredTextAnnotationMap = oldNeuron.getStructuredTextAnnotationMap();
        final Map<Long, TmStructuredTextAnnotation> newStructuredTextAnnotationMap = newNeuron.getStructuredTextAnnotationMap();
        for (TmGeoAnnotation ann: oldNeuron.getSubTreeList(rootAnnotation)) {
            movedAnnotationIDs.put(ann.getId(), ann);
            ann.setParentId(newNeuron.getId());            
            newNeuron.getGeoAnnotationMap().put(ann.getId(), ann);
            // move any TmStructuredTextAnnotations as well:
            if (oldStructuredTextAnnotationMap.containsKey(ann.getId())) {
                TmStructuredTextAnnotation note = oldStructuredTextAnnotationMap.get(ann.getId());
                oldStructuredTextAnnotationMap.remove(ann.getId());
                newStructuredTextAnnotationMap.put(ann.getId(), note);
            }

        }
        
        // loop over anchored paths; if endpoints are in set of moved annotations,
        //  move the path as well
        Map<TmAnchoredPathEndpoints,TmAnchoredPath> oldNeuronAnchoredPathMap = oldNeuron.getAnchoredPathMap();
        Map<TmAnchoredPathEndpoints,TmAnchoredPath> newNeuronAnchoredPathMap = newNeuron.getAnchoredPathMap();
        for (TmAnchoredPathEndpoints endpoints : oldNeuron.getAnchoredPathMap().keySet()) {
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
            oldNeuron.getGeoAnnotationMap().remove(movedAnnotationID);
        }
        
        // if it's the root, also change its parent annotation to the new neuron
        rootAnnotation.setParentId(newNeuron.getId());
//        if (rootAnnotation.getId().equals(rootAnnotation.getId())) {
//        }

    }
    
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(TmWorkspace workspace) throws Exception {
        // Validate sample
        if (workspace == null) {
            throw new Exception("Neurons must be parented with valid Workspace Id");
        }
        List<TmNeuronDescriptor> descriptorList = new ArrayList<>();
        for (TmNeuron neuron: workspace.getNeuronList()) {
            TmNeuronDescriptor descriptor = new TmNeuronDescriptor(
                    neuron.getId(), neuron.getName(), neuron.getGeoAnnotationMap().size() + neuron.getRootAnnotations().size()
            );
            descriptorList.add(descriptor);
        }
        return descriptorList;
        /*
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
        
        */
    }    

    public void deleteAnchoredPath(TmWorkspace workspace, TmAnchoredPath path) throws Exception {
        // Remove the anchor path from its containing workspace
    }
    
    public void deleteNeuron(TmWorkspace tmWorkspace, TmNeuron tmNeuron) throws Exception {
    }

    public void deleteGeometricAnnotation(TmNeuron tmNeuron, TmGeoAnnotation tmAnno) throws Exception {
    }

    public void deleteStructuredText(TmNeuron tmNeuron, TmStructuredTextAnnotation tmStrucTextAnno) throws Exception {
    }
    
    
    
    /**
     * fix connectivity issues for all neurons in a workspace
     */
    private void fixConnectivityWorkspace(TmWorkspace tmWorkspace) throws Exception {
    }

    /**
     * fix connectity issues for a neuron (bad parents, since children aren't
     * stored in the entity data); fix in this case means breaking links
     */
    private void fixConnectivityNeuron(TmWorkspace tmWorkspace, TmNeuron tmNeuron) throws Exception {
    }

    
// RESERVE JUDGEMENT
//    public void addLinkedGeometricAnnotations(
//            Map<Integer, Integer> nodeParentLinkage,
//            Map<Integer, TmGeoAnnotation> annotations
//    ) throws Exception {
//        
//    }
}
