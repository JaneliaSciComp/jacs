package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */
public class TmNeuron implements IsSerializable, Serializable {

    Long id;
    String name;
    Date creationDate;
    Map<Long, TmGeoAnnotation> geoAnnotationMap=new HashMap<Long, TmGeoAnnotation>();
    List<TmGeoAnnotation> rootAnnotations=new ArrayList<TmGeoAnnotation>();
    Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = new HashMap<TmAnchoredPathEndpoints, TmAnchoredPath>();

    Map<Long, TmStructuredTextAnnotation> textAnnotationMap = new HashMap<Long, TmStructuredTextAnnotation>();

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

    public List<TmGeoAnnotation> getRootAnnotations() {
        return rootAnnotations;
    }

    // maps ID of parent to text annotation
    public Map<Long, TmStructuredTextAnnotation> getStructuredTextAnnotationMap() {
        return textAnnotationMap;
    }

    public TmNeuron(Long id, String name) {
        this.id=id;
        this.name=name;
    }

    public TmNeuron(Entity entity) throws Exception {
        if (!entity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
            throw new Exception("Entity type must be "+EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
        }
        this.id=entity.getId();
        this.name=entity.getName();
        this.creationDate = entity.getCreationDate();

        // First step is to take all those entity data and put them into the
        //  appropriate objects, and the objects into the right collections
        for (EntityData ed : entity.getEntityData()) {
            String edAttr = ed.getEntityAttrName();
            if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE) ||
                    edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                TmGeoAnnotation ga = new TmGeoAnnotation(ed.getValue());
                if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                    rootAnnotations.add(ga);
                }
                geoAnnotationMap.put(ga.getId(), ga);
                // these values only set when we do it here, in the context of a neuron
                ga.setNeuronId(id);
                ga.setEmptyChildList();
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                TmAnchoredPath path = new TmAnchoredPath(ed.getValue());
                anchoredPathMap.put(path.getEndpoints(), path);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT)) {
                TmStructuredTextAnnotation ann = new TmStructuredTextAnnotation(ed.getValue());
                textAnnotationMap.put(ann.getParentId(), ann);
            }
        }
        // Second step is to link childen to produce the graph for
        //  the GeoAnnotations
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            Long parentId = ga.getParentId();
            // if parent ID is the neuron ID, it's a root, the ID won't be in
            //  the map, and we don't need to connect it:
            if (!parentId.equals(this.id)) {
                TmGeoAnnotation parent = geoAnnotationMap.get(parentId);
                if (parent==null) {
                    throw new TmConnectivityException(String.format("Could not find parent for TmGeoAnnotation id = %d in neuron id = %d", ga.getId(), id));
                }
                parent.addChild(ga);
            }
        }
    }

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
        annList.add(ann);
        for (TmGeoAnnotation a: getChildrenOf(ann)) {
            appendSubTreeList(annList, a);
        }
    }


}
