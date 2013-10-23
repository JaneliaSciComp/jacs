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

    // two IDs of the annotations between which the path runs; we choose to maintain
    //  annotationID1 < annotationID2
    Long annotationID1;
    Long annotationID2;

    // VoxelIndex and Vec3 not available to model, so wing it; these
    //  will be 3-vectors (x, y, z):
    List<List<Integer>> pointList;

    public TmAnchoredPath(Long id, Long annotationID1, Long annotationID2, List<List<Integer>> pointList) throws Exception{
        this.id = id;
        setAnnotationIDs(annotationID1, annotationID2);
        setPointList(pointList);
    }

    /**
     * create from string from entity data; expected format is:
     * id:annID1:annID2:x,y,z:(repeat points)
     */
    public TmAnchoredPath(String pathString) throws Exception {
        String[] fields = pathString.split(":", -1);
        if (fields.length < 3) {
            throw new Exception("not enough separators in pathString");
        }
        id = new Long(fields[0]);
        annotationID1 = new Long(fields[1]);
        annotationID2 = new Long(fields[2]);

        for (int i=3; i<fields.length; i++) {
            String[] coords = fields[i].split(",");
            if (coords.length != 3) {
                throw new Exception(String.format("couldn't parse coordinates %s", fields[i]));
            }
            ArrayList<Integer> temp = new ArrayList<Integer>(3);
            temp.set(0, Integer.parseInt(coords[0]));
            temp.set(1, Integer.parseInt(coords[1]));
            temp.set(2, Integer.parseInt(coords[2]));
            pointList.add(temp);
        }

    }

    public static String toStringFromArguments(Long id, Long annotationID1, Long annotationID2, List<List<Integer>> pointList)
        throws Exception {
        // make a gross estimate at initial capacity, given format
        StringBuilder builder = new StringBuilder(30 + 15 * pointList.size());
        builder.append(String.format("%d:%d:%d", id, annotationID1, annotationID2));
        for (List<Integer> point : pointList) {
            builder.append(String.format(":%d,%d,%d", point.get(0), point.get(1), point.get(2)));
        }
        // 1G is the approx. limit on data transfer in MySQL in our environment
        if (builder.length() > 1000000000) {
            throw new Exception("too many points!");
        }
        return builder.toString();
    }

    public String toString() {
        return String.format("<path between %d, %d", annotationID1, annotationID2);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnnotationID1() {
        return annotationID1;
    }

    public Long getAnnotationID2() {
        return annotationID2;
    }

    public void setAnnotationIDs(Long annotationID1, Long annotationID2) {
        this.annotationID1 = annotationID1;
        this.annotationID2 = annotationID2;
        if (annotationID2 != null && annotationID1 != null && annotationID1 > annotationID2) {
            this.annotationID1 = annotationID2;
            this.annotationID2 = annotationID1;
        }
    }

    public List<List<Integer>> getPointList() {
        return pointList;
    }

    /**
     * points must be ordered list of 3-vectors (x, y, z)
     */
    public void setPointList(List<List<Integer>> pointList) throws Exception {
        for (List<Integer> point: pointList) {
            if (point.size() != 3) {
                throw new Exception("found point with dimension not equal to three!");
            }
        }
        this.pointList = pointList;
    }
}
