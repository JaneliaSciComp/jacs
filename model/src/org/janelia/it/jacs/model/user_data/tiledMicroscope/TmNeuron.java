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
    Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = new HashMap<TmAnchoredPathEndpoints, TmAnchoredPath>();
    List<TmGeoAnnotation> rootAnnotations=new ArrayList<TmGeoAnnotation>();

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

    public Map<Long, TmGeoAnnotation> getGeoAnnotationMap() {
        return geoAnnotationMap;
    }

    public Map<TmAnchoredPathEndpoints, TmAnchoredPath> getAnchoredPathMap() {
        return anchoredPathMap;
    }

    public List<TmGeoAnnotation> getRootAnnotations() {
        return rootAnnotations;
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

        // First step is to create TmGeoAnnotation and TmAnchoredPath objects
        for (EntityData ed : entity.getEntityData()) {
            String edAttr = ed.getEntityAttrName();
            if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE) ||
                    edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                TmGeoAnnotation ga = new TmGeoAnnotation(ed.getValue());
                if (edAttr.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                    rootAnnotations.add(ga);
                }
                geoAnnotationMap.put(ga.getId(), ga);
            } else if (edAttr.equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                TmAnchoredPath path = new TmAnchoredPath(ed.getValue());
                anchoredPathMap.put(path.getEndpoints(), path);
            }
        }
        // Second step to to use child/parent fields to construct graph for
        //  the GeoAnnotations
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            Long parentId = ga.getParentId();
            // if parent ID is the neuron ID, it's a root, the ID won't be in
            //  the map, and we don't need to connect it:
            if (!parentId.equals(this.id)) {
                TmGeoAnnotation parent = geoAnnotationMap.get(parentId);
                if (parent==null) {
                    throw new Exception(String.format("Could not find parent for TmGeoAnnotation id = %d in neuron id = %d", ga.getId(), id));
                }
                parent.addChild(ga);
                ga.setParent(parent);
            }
        }
        // Last step is check to make sure every non-root annotation has a parent
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            if (ga.getParent()==null && !ga.getParentId().equals(this.id)) {
                throw new Exception(String.format("TmGeoAnnotation id = %d unexpectedly does not have a valid parent in neuron id = %d", ga.getId(), id));
            }
        }
    }

}
