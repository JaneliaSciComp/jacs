
package org.janelia.it.jacs.shared.blast;

import org.janelia.it.jacs.model.genomics.BlastHit;

/**
 * User: aresnick
 * Date: Jun 1, 2009
 * Time: 1:33:36 PM
 * <p/>
 * <p/>
 * Description:
 * Converts HSP coordinates to appropriately oriented btab output coordinates.
 * <p/>
 * Coordinates with a forward orientation have ascending beginning and end coordinates;
 * coordiantes with a reverse orientation have descending beginning and end coordinates
 * <p/>
 * Assumes input ParsedBlastHSP object's coordinates have been normalized so that
 * HSP begin coordinate is < HSP end coordinate
 */
public class HSPCoordinates {
    private CoordinatePair queryCoordinates;
    private CoordinatePair subjectCoordinates;

    public HSPCoordinates(ParsedBlastHSP hsp) {
        queryCoordinates = getCoordinates(hsp.getQueryBegin(), hsp.getQueryEnd(), hsp.getQueryOrientation());
        subjectCoordinates = getCoordinates(hsp.getSubjectBegin(), hsp.getSubjectEnd(), hsp.getSubjectOrientation());
    }

    /*
        if sequence orientation is reverse orientation, use endCoordiante
        as begin coordiante of coordinate pair and beginCoordinate as
        end coordinate of coordinate pair

        regardless of re-orientation, add 1 to beginCoordinate to re-adjust
        spacing coordiante system
     */
    private CoordinatePair getCoordinates(Integer beginCoordinate, Integer endCoordinate, Integer orientation) {
        int adjustedBeginCoordinate = beginCoordinate + 1;

        CoordinatePair coordinatePair = new CoordinatePair();
        if (BlastHit.ALGN_ORI_REVERSE.equals(orientation)) {
            coordinatePair.setBeginCoordinate(endCoordinate);
            coordinatePair.setEndCoordinate(adjustedBeginCoordinate);
        }
        else {
            coordinatePair.setBeginCoordinate(adjustedBeginCoordinate);
            coordinatePair.setEndCoordinate(endCoordinate);
        }
        return coordinatePair;
    }

    public Integer getQueryBeginCoordinate() {
        return queryCoordinates.getBeginCoordinate();
    }

    public Integer getQueryEndCoordinate() {
        return queryCoordinates.getEndCoordinate();
    }

    public Integer getSubjectBeginCoordinate() {
        return subjectCoordinates.getBeginCoordinate();
    }

    public Integer getSubjectEndCoordinate() {
        return subjectCoordinates.getEndCoordinate();
    }

    private class CoordinatePair {
        private Integer beginCoordinate;
        private Integer endCoordinate;

        public CoordinatePair() {
        }

        public CoordinatePair(Integer beginCoordinate, Integer endCoordinate) {
            this.beginCoordinate = beginCoordinate;
            this.endCoordinate = endCoordinate;
        }

        public Integer getBeginCoordinate() {
            return beginCoordinate;
        }

        public void setBeginCoordinate(Integer beginCoordinate) {
            this.beginCoordinate = beginCoordinate;
        }

        public Integer getEndCoordinate() {
            return endCoordinate;
        }

        public void setEndCoordinate(Integer endCoordinate) {
            this.endCoordinate = endCoordinate;
        }
    }
}
