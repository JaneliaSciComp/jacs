package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import io.protostuff.Tag;

import java.io.Serializable;


/**
 * this class encapsulates a pair of TmGeoAnnotation IDs, intended to be
 * neighboring, between which we will have a TmAnchoredPath; the
 * lesser of the two is stored as the first annotation
 *
 * User: olbrisd
 * Date: 10/23/13
 * Time: 12:52 PM
 */
public class TmAnchoredPathEndpoints implements IsSerializable, Serializable {
    @Tag(1)
    private Long annotationID1;
    @Tag(2)
    private Long annotationID2;

    public TmAnchoredPathEndpoints(Long annotationID1, Long annotationID2) {
        setAnnotations(annotationID1, annotationID2);
    }

    public TmAnchoredPathEndpoints(TmGeoAnnotation annotation1, TmGeoAnnotation annotation2) {
        setAnnotations(annotation1.getId(), annotation2.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TmAnchoredPathEndpoints)) {
            return false;
        }

        return (annotationID1.equals(((TmAnchoredPathEndpoints) o).getAnnotationID1()) &&
                annotationID2.equals(((TmAnchoredPathEndpoints) o).getAnnotationID2()));
    }

    @Override
    public int hashCode() {
        // taken from Joshua Bloch's Effective Java Ch. 3 item 9
        // (widely quoted on the Internet):
        int result = (int) (annotationID1 ^ (annotationID1 >>> 32));
        result = 31 * result + (int) (annotationID2 ^ (annotationID2 >>> 32));
        return result;
    }

    public Long getAnnotationID1() {
        return annotationID1;
    }

    public Long getAnnotationID2() {
        return annotationID2;
    }

    public void setAnnotations(Long annotationID1, Long annotationID2) {
        if (annotationID2 < annotationID1) {
            this.annotationID1 = annotationID2;
            this.annotationID2 = annotationID1;
        } else {
            this.annotationID1 = annotationID1;
            this.annotationID2 = annotationID2;
        }
    }

}
