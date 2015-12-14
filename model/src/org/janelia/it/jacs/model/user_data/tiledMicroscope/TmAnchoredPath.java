package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represents a list of points, probably computer generated, that
 * trace a path between two anchors/annotations.  Although we typically reserve
 * anchors to mean the glyphs/objects drawn in the 2D view, and this class is
 * more like an annotation class, the word "anchored" nonetheless well describes
 * the path.
 *
 * User: olbrisd
 * Date: 10/17/13
 * Time: 10:14 AM
 */
public class TmAnchoredPath implements IsSerializable, Serializable {
    Long id;

    // two IDs of the annotations between which the path runs
    TmAnchoredPathEndpoints endpoints;

    // VoxelIndex and Vec3 not available to model, so wing it; these
    //  will be 3-vectors (x, y, z):
    List<List<Integer>> pointList;

    public TmAnchoredPath(Long id, TmAnchoredPathEndpoints endpoints, List<List<Integer>> pointList) throws Exception{
        this.id = id;
        this.endpoints = endpoints;
        setPointList(pointList);
    }

    public String toString() {
        if (endpoints != null) {
            return String.format("<path between %d, %d>", endpoints.getAnnotationID1(), endpoints.getAnnotationID2());
        } else {
            return "<uninitialized path>";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TmAnchoredPathEndpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(TmAnchoredPathEndpoints endpoints) {
        this.endpoints = endpoints;
    }

    public List<List<Integer>> getPointList() {
        return pointList;
    }

    /**
     * points must be ordered list of 3-vectors (x, y, z)
     */
    public final void setPointList(List<List<Integer>> pointList) throws Exception {
        for (List<Integer> point: pointList) {
            if (point.size() != 3) {
                throw new Exception("found point with dimension not equal to three!");
            }
        }
        this.pointList = pointList;
    }
}
