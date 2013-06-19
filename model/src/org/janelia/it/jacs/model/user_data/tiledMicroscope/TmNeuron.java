package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */
public class TmNeuron implements IsSerializable, Serializable {

    Long id;
    String name;
    Map<Long, TmGeoAnnotation> geoAnnotationMap=new HashMap<Long, TmGeoAnnotation>();
    TmGeoAnnotation rootAnnotation=null;

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

    public Map<Long, TmGeoAnnotation> getGeoAnnotationMap() {
        return geoAnnotationMap;
    }

    public TmGeoAnnotation getRootAnnotation() {
        return rootAnnotation;
    }

    public TmNeuron(Long id, String name) {
        this.id=id;
        this.name=name;
    }

    protected void addGeoAnnotation(TmGeoAnnotation annotation) throws Exception {
        if (rootAnnotation==null) {
            if (getGeoAnnotationMap().size()>0) {
                throw new Exception("Unexpectedly have null rootAnnotation but nonempty annotationMap");
            }
            rootAnnotation=annotation;
            rootAnnotation.setParentId(this.id);
            geoAnnotationMap.put(rootAnnotation.getId(), rootAnnotation);
        } else {
            Long parentId=annotation.getParentId();
            boolean foundParent=false;
            for (Long key : geoAnnotationMap.keySet()) {
                if (key.equals(parentId)) {
                    TmGeoAnnotation parent=geoAnnotationMap.get(key);
                    parent.addChild(annotation);
                    annotation.setParent(parent);
                    foundParent=true;
                    break;
                }
            }
            if (!foundParent) {
                throw new Exception("Could not find parent for annotation Id="+annotation.getId());
            }
        }
    }

    public TmNeuron(Entity entity) throws Exception {
        if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
            throw new Exception("Entity type must be "+EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
        }
        this.id=entity.getId();
        this.name=entity.getName();
        boolean foundRoot=false;
        // First step is to create TmGeoAnnotation objects
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                TmGeoAnnotation ga = new TmGeoAnnotation(ed.getValue());
                if (ga.getParentId()==this.id) {
                    if (foundRoot) {
                        throw new Exception("Only single root node permitted");
                    }
                    foundRoot=true;
                    rootAnnotation=ga;
                }
                geoAnnotationMap.put(ga.getId(), ga);
            }
        }
        // Second step to to use child/parent fields to construct graph
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            Long parentId = ga.getParentId();
            TmGeoAnnotation parent = geoAnnotationMap.get(parentId);
            if (parent==null) {
                throw new Exception("Could not find parent for TmGeoAnnotation id="+ga.getId());
            }
            parent.addChild(ga);
            ga.setParent(parent);
        }
        // Last step is check to make sure every Annotation has a parent
        for (TmGeoAnnotation ga : geoAnnotationMap.values()) {
            if (ga.getParent()==null && ga.getParentId()!=this.id) {
                throw new Exception("TmGeoAnnotation unexpectedly does not have a valid parent="+ga.getId());
            }
        }
    }

}
