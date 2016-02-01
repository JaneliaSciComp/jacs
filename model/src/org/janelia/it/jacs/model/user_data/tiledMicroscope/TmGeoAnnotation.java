    package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import io.protostuff.Tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:23 PM
 */
public class TmGeoAnnotation implements IsSerializable, Serializable {
    @Tag(1)
    private Long id;
    // parentID is the neuron (if root annotation) or another TmGeoAnn
    @Tag(2)
    private Long parentId;
    @Tag(3)
    private String comment;
    @Tag(4)
    private Integer index;
    @Tag(5)
    private Double x;
    @Tag(6)
    private Double y;
    @Tag(7)
    private Double z;
    @Tag(8)
    private Date creationDate;

    // child and neuron ID fields only filled in when the annotation is in a neuron!
    //  they are null otherwise
    // I'd like to have a flag that is set when these are correct, but there's no
    //  way for the GeoAnn to keep it up-to-date, as it's not involved when operations
    //  are performed on other GeoAnns (creation, deletion, update), so the info
    //  would get stale fast
    @Tag(9)
    private Long neuronId = null;
    @Tag(10)
    private List<Long> childIds = new ArrayList<>();;

    // implementation note: at one point we stored the parent and child objects,
    //  but serializing them for calling remote server routines caused the
    //  whole tree to get walked recursively, overflowing the stack; so
    //  now we just use the IDs (FW-2728)

    public TmGeoAnnotation(Long id, String comment, Double x, Double y, Double z, Long parentId, Date creationDate) {
        this.id=id;
        this.comment=comment;
        this.x=x;
        this.y=y;
        this.z=z;
        this.parentId=parentId;
        this.creationDate = creationDate;
    }

    public static String toStringFromArguments(Long id, Long parentId, int index, Double x, Double y, Double z, String comment) {
        return id + ":" + parentId + ":" + index + ":" + x.toString() + "," + y.toString() + "," + z.toString() + ":" + comment;
    }

    public TmGeoAnnotation() {        
    }
    
    @Override
    public String toString() {
        //return String.format("ann id %d", id);
        // return String.format("(%.1f, %.1f, %.1f)", x, y, z);
        return String.format("%d, %d, %d", x.intValue(), y.intValue(), z.intValue());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIndex(int index) {
        this.index=index;
    }

    public Integer getIndex() {
        return index;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public void addChild(TmGeoAnnotation child) {
        childIds.add(child.getId());
    }

    public void setParentId(Long parentId) {
        this.parentId=parentId;
    }

    public Long getParentId() {
        return parentId;
    }

    public List<Long> getChildIds() {
        return childIds;
    }

    public Long getNeuronId() {
        return neuronId;
    }

    public void setNeuronId(Long neuronId) {
        this.neuronId = neuronId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isRoot() {
        return neuronId != null && parentId.equals(neuronId);
    }

    public boolean isBranch() {
        return getChildIds().size() > 1;
    }

    public boolean isEnd() {
        return getChildIds().size() == 0;
    }

    public boolean isLink() {
        return !isRoot() && getChildIds().size() == 1;
    }

}

