package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:23 PM
 */
public class TmGeoAnnotation implements IsSerializable, Serializable {
    Long id;
    Long parentId;
    String comment;
    Integer index;
    Double x, y, z;
    TmGeoAnnotation parent=null;
    List<TmGeoAnnotation> children=new ArrayList<TmGeoAnnotation>();

    public TmGeoAnnotation(Long id, String comment, Double x, Double y, Double z, TmGeoAnnotation parent) {
        this.id=id;
        this.comment=comment;
        this.x=x;
        this.y=y;
        this.z=z;
        this.parent=parent;
    }

    public static String toStringFromArguments(Long id, Long parentId, int index, Double x, Double y, Double z, String comment) {
        return id + ":" + parentId + ":" + index + ":" + x.toString() + "," + y.toString() + "," + z.toString() + ":" + comment;
    }

    // format expected: <id>:<parentId>:<index>:<x,y,z>:<comment>
    // note1: can't currently handle empty comments
    // note2: can't currently handle comments including ":"
    public TmGeoAnnotation(String geoString) throws Exception {
        String[] fields=geoString.split(":");
        if (fields.length!=5) {
            throw new Exception("Could not parse geoString="+geoString);
        }
        id=new Long(fields[0]);
        parentId=new Long(fields[1]);
        index=new Integer(fields[2]);
        String coordinateString=fields[3];
        String[] cArr=coordinateString.split(",");
        x=new Double(cArr[0].trim());
        y=new Double(cArr[1].trim());
        z=new Double(cArr[2].trim());
        comment=fields[4];
    }

    public String toString() {
        //return String.format("ann id %d", id);
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
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

    public TmGeoAnnotation getParent() {
        return parent;
    }

    public void setParent(TmGeoAnnotation parent) {
        this.parent = parent;
    }

    public List<TmGeoAnnotation> getChildren() {
        return children;
    }

    /**
     * this method returns a list of all children in the subtree of the current
     * annotation, plus the annotation itself; the order is such that the
     * annotation itself is first, and each child is guaranteed to appear
     * after its parent
     * @return list of annotations in subtree rooted at current annotation
     */
    public List<TmGeoAnnotation> getSubTreeList() {
        ArrayList<TmGeoAnnotation> subtreeList = new ArrayList<TmGeoAnnotation>();
        appendSubTreeList(subtreeList, this);
        return subtreeList;
    }

    private void appendSubTreeList(List<TmGeoAnnotation> annList, TmGeoAnnotation ann) {
        annList.add(ann);
        for (TmGeoAnnotation a: ann.getChildren()) {
            appendSubTreeList(annList, a);
        }
    }

    public void addChild(TmGeoAnnotation child) {
        children.add(child);
    }

    public void setParentId(Long parentId) {
        this.parentId=parentId;
    }

    public Long getParentId() {
        return parentId;
    }

}
