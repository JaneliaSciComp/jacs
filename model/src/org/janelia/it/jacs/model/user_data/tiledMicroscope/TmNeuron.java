package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import io.protostuff.Tag;

import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Requires no-args constructor for use with Protostuff/Protobuf
 * 
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */
public class TmNeuron implements IsSerializable, Serializable {

    // this enum is to be used in navigation requests for
    //  movement along a neuron; it is intentionally vague and
    //  conceptual, in order to let us rather than the requester
    //  interpret what the relationship means in gory detail
    public enum AnnotationNavigationDirection {
        // easy: toward or away from the root of the neuron
        ROOTWARD_JUMP,
        ROOTWARD_STEP,
        ENDWARD_JUMP,
        ENDWARD_STEP,
        // assuming the children of each branch are in some stable
        //  order, next/prev implies movement between sibling
        //  branches of the nearest rootward branch point
        NEXT_PARALLEL,
        PREV_PARALLEL,
    }

    @Tag(1)
    private Long id;
    @Tag(2)
    private Long workspaceId;
    @Tag(3)
    private String name;
    @Tag(4)
    private Date creationDate;
    @Tag(5)
    private String ownerKey;
    @Tag(6)
    private Map<Long, TmGeoAnnotation> geoAnnotationMap=new HashMap<>();
    @Tag(7)
    private List<Long> rootAnnotationIds = new ArrayList<>();
    @Tag(8)
    private Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = new HashMap<>();

    @Tag(9)
    private Map<Long, TmStructuredTextAnnotation> textAnnotationMap = new HashMap<>();
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(Date date) {
        this.creationDate = date;
    }
    
    /**
     * @return the ownerKey
     */
    public String getOwnerKey() {
        return ownerKey;
    }

    /**
     * @param ownerKey the ownerKey to set
     */
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    /**
     * @return the workspaceId
     */
    public Long getWorkspaceId() {
        return workspaceId;
    }

    /**
     * @param workspaceId the workspaceId to set
     */
    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public String toString() {
        return name;
    }

    // maps geo ann ID to geo ann object
    public Map<Long, TmGeoAnnotation> getGeoAnnotationMap() {
        return geoAnnotationMap;
    }

    // maps endpoints of path to path object
    public Map<TmAnchoredPathEndpoints, TmAnchoredPath> getAnchoredPathMap() {
        return anchoredPathMap;
    }

    /** 
     * Returns all the root annotations, such that:
     * + the contents of the returned collection may not be changed;
     * + the annotations within the collection _could_ be changed.
     * 
     * Note that this contract guarantees that no new root annotations may
     * be placed into the returned value, nor any added.  This collection is
     * meant for read-only purposes.  Any removal of root annotations, or
     * addition of root annotations, must be done via the id collection.
     * 
     * However, nothing may prevent a caller
     * from modifying state of any geo-annotation found in this collection.
     * 
     * This collection is immutable to avoid deceiving callers about the
     * effects of modifying the return value.
     */
    public List<TmGeoAnnotation> getRootAnnotations() {
        TmGeoAnnotation[] tempList = new TmGeoAnnotation[rootAnnotationIds.size()];
        int i = 0;        
        for (Long id: rootAnnotationIds) {
            tempList[ i++ ] = geoAnnotationMap.get(id);
        }
        return Collections.unmodifiableList(Arrays.asList(tempList));
    }
    
    public TmGeoAnnotation getFirstRoot() {
        if (rootAnnotationIds.size() > 0) {
            return geoAnnotationMap.get(rootAnnotationIds.get(0));
        }
        else {
            return null;
        }
    }
    
    public void removeRootAnnotation(TmGeoAnnotation root) {
        removeRootAnnotation(root.getId());
    }
    
    public void removeRootAnnotation(Long rootId) {
        rootAnnotationIds.remove(rootId);
    }
    
    public void addRootAnnotation(TmGeoAnnotation root) {
        addRootAnnotation(root.getId());
    }
    
    public void addRootAnnotation(Long rootId) {
        rootAnnotationIds.add(rootId);
    }
    
    public boolean containsRootAnnotation(TmGeoAnnotation root) {
        return containsRootAnnotation(root.getId());
    }
    
    public boolean containsRootAnnotation(Long rootId) {
        return rootAnnotationIds.contains(rootId);
    }
    
    public int getRootAnnotationCount() { return rootAnnotationIds.size(); }
    
    public void clearRootAnnotations() { rootAnnotationIds.clear(); }

    // maps ID of parent to text annotation
    public Map<Long, TmStructuredTextAnnotation> getStructuredTextAnnotationMap() {
        return textAnnotationMap;
    }

    public TmNeuron(Long id, String name) {
        this.id=id;
        this.name=name;
    }

    public TmNeuron() {}

    public TmGeoAnnotation getParentOf(TmGeoAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        // arguably this should throw an exception (annotation not in neuron)
        if (!getGeoAnnotationMap().containsKey(annotation.getId())) {
            return null;
        }
        return getGeoAnnotationMap().get(annotation.getParentId());
    }

    public List<TmGeoAnnotation> getChildrenOf(TmGeoAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        // arguably this should throw an exception (annotation not in neuron)
        if (!getGeoAnnotationMap().containsKey(annotation.getId())) {
            return null;
        }
        ArrayList<TmGeoAnnotation> children = new ArrayList<TmGeoAnnotation>(annotation.getChildIds().size());
        for (Long childID: annotation.getChildIds()) {
            children.add(getGeoAnnotationMap().get(childID));
        }
        return children;
    }

    /**
     * returns a list of the child annotations of the input annotation in
     * a predictable, stable order
     *
     * current implementation is based on the angle from the x-axis
     * of the x-y projection of the line connecting the branch to its child
     */
    public List<TmGeoAnnotation> getChildrenOfOrdered(TmGeoAnnotation annotation) {
        List<TmGeoAnnotation> unorderedList = getChildrenOf(annotation);
        if (unorderedList.size() < 2) {
            return unorderedList;
        }

        // we're going to use the angle in the xy plane of the lines between
        //  the parent and children to sort; for convenience, I'll use
        //  the value returned by math.atan2
        Collections.sort(unorderedList, new Comparator<TmGeoAnnotation>() {
            @Override
            public int compare(TmGeoAnnotation ann1, TmGeoAnnotation ann2) {
                double tan1 = Math.atan2(ann1.getY(), ann1.getX());
                double tan2 = Math.atan2(ann2.getY(), ann2.getX());
                if (tan1 > tan2) {
                    return 1;
                } else if (tan1 < tan2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        return unorderedList;
    }

    /**
     * this method returns a list of all children in the subtree of the input
     * annotation, plus the annotation itself; the order is such that the
     * annotation itself is first, and each child is guaranteed to appear
     * after its parent
     * @return list of annotations in subtree rooted at given annotation
     */
    public List<TmGeoAnnotation> getSubTreeList(TmGeoAnnotation annotation) {
        ArrayList<TmGeoAnnotation> subtreeList = new ArrayList<TmGeoAnnotation>();
        appendSubTreeList(subtreeList, annotation);
        return subtreeList;
    }

    private void appendSubTreeList(List<TmGeoAnnotation> annList, TmGeoAnnotation ann) {
		if (ann == null) {
			// For this to happen (below), one of the children of a
			// annotation would have to be null, for "a" in the method call
			// becomes "ann" in the argument list.
			System.out.println("Null annotation in TmNeuron " + getName() + ", " + System.identityHashCode(this));
			new Exception("recursed into null").printStackTrace();
			return;
		}
        annList.add(ann);
        for (TmGeoAnnotation a: getChildrenOf(ann)) {
            appendSubTreeList(annList, a);
        }
    }

    /**
     * check a neuron for problems and potentially repair;
     * check that its relationships are consistent, and any
     * referred-to annotations are actually present
     *
     * returns list of problems found and/or fixed; empty
     * list = no problems
     */
    public List<String> checkRepairNeuron(boolean repair) {
        List<String> results = new ArrayList<>();

        // are all roots in ann map?
        Set<Long> rootIDsNotInMap = new HashSet<>();
        for (Long rootID: rootAnnotationIds) {
            if (!getGeoAnnotationMap().containsKey(rootID)) {
                results.add("neuron " + getName() + ": ann ID " + rootID + " is a root but not in ann map");
                rootIDsNotInMap.add(rootID);
            }
        }
        if (repair) {
            // remove bad ID from root ID list
            for (Long r: rootIDsNotInMap) {
                rootAnnotationIds.remove(r);
                results.add("removed root ID " + r + " from root list");
            }

            // check that no annotations have it as a parent;
            //  if one does, promote it to root (set parent to neuron,
            //  add to root list)
            for (TmGeoAnnotation ann: getGeoAnnotationMap().values()) {
                if (rootIDsNotInMap.contains(ann.getParentId())) {
                    ann.setParentId(getId());
                    rootAnnotationIds.add(ann.getId());
                    results.add("promoted ann ID " + ann.getId() + " to root annotation");
                }
            }
        }


        // check annotation parents
        // all anns have parent in map?  roots in root list?
        for (TmGeoAnnotation ann: getGeoAnnotationMap().values()) {
            if (ann.getParentId().equals(getId())) {
                // if parent = neuron, it's a root; in the root list?
                if (!getRootAnnotations().contains(ann)) {
                    results.add("neuron " + getName() + ": root ID " + ann.getId() + " not in root list");
                    if (repair) {
                        results.add("repair not implemented for this problem");
                    }
                }
            } else if (!getGeoAnnotationMap().containsKey(ann.getParentId())) {
                // otherwise, is parent in map?
                if (!getGeoAnnotationMap().containsKey(ann.getParentId())) {
                    results.add("neuron " + getName() + ": ann ID " + ann.getId() + "'s parent not in ann map");
                    if (repair) {
                        results.add("repair not implemented for this problem");
                    }
                }
            }
        }


        // check that endpoints of anchored paths are present; if not,
        //  remove the paths
        // (unimplemented)


        // check that structured text annotations are attached to valid things
        //  if not, remove them
        // (unimplemented)



        return results;
    }

    public List<String> checkRepairNeuron() {
        return checkRepairNeuron(true);
    }

    public List<String> checkNeuron() {
        return checkRepairNeuron(false);
    }
}
