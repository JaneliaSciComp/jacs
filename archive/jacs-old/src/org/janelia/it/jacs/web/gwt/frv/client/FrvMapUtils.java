
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMapUtils;

/**
 * Frv-specific Google Map utilities, generally to convert between lat/long and FrvBounds (pctId/basePair scales)
 *
 * @author Michael Press
 */
public class FrvMapUtils extends GoogleMapUtils {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.FrvMapUtils");

    /**
     * Convert the map's currently visible bounds from lat/long to a FrvBounds instance (pctId/basePair)
     *
     * @param job job to convert bounds for
     * @param map MapWidget object to work against
     * @return returns the FrvBounds object
     */
    public static FrvBounds convertLatLngToFrvBounds(RecruitableJobInfo job, MapWidget map) {
        // Convert the map's NE and SW corners to the NW and SE that the conversion needs
        LatLng ne = map.getBounds().getNorthEast();
        LatLng sw = map.getBounds().getSouthWest();
        return convertLatLngToFrvBounds(job, sw.getLongitude(), ne.getLatitude(), ne.getLongitude(), sw.getLatitude());
    }

    /**
     * Convert a map region specified by NW and SE lat/lng corners to FrvBounds (base pair and pctId ranges).
     *
     * @param job job to convert data for
     * @param nwX northwest x coordinate
     * @param nwY northwest y coordinate
     * @param seX southeast x coordinate
     * @param seY southeast y coordinate
     * @return returns the FrvBounds object
     */
    public static FrvBounds convertLatLngToFrvBounds(RecruitableJobInfo job, double nwX, double nwY, double seX, double seY) {
        long startCoord = job.getRefAxisBeginCoord();
        long endCoord = job.getRefAxisEndCoord();
        long coordDist = endCoord - startCoord;

        int startPctId = job.getPercentIdentityMin();
        int endPctId = job.getPercentIdentityMax();
        int pctIdDist = endPctId - startPctId;

        // convert lat to 0..180 range
        double x1abs = nwX + DEGREES_LONGITUDE_PER_HEMISPHERE;
        double x1pct = x1abs / TOTAL_DEGREES_LONGITUDE;
        long selectionStartCoord = (long) (coordDist * x1pct + startCoord);

        double x2abs = seX + DEGREES_LONGITUDE_PER_HEMISPHERE;
        double x2pct = x2abs / TOTAL_DEGREES_LONGITUDE;
        long selectionEndCoord = (long) (coordDist * x2pct + startCoord);

        double y1abs = nwY + DEGREES_LATITUDE_PER_HEMISPHERE;
        double y1pct = y1abs / TOTAL_DEGREES_LATITUDE;
        double selectionStartPctId = pctIdDist * y1pct + startPctId;

        double y2abs = seY + DEGREES_LATITUDE_PER_HEMISPHERE;
        double y2pct = y2abs / TOTAL_DEGREES_LATITUDE;
        double selectionEndPctId = pctIdDist * y2pct + startPctId;

        // Handle drags that extend off the right (can't drag off the left since the drag only progresses from
        // top left to bottom right).  If end base coord is < start, that means the drag was off the right side
        // and into the start of the (not visible) wrapped image, so just maximize the end coord to the max value.
        if (selectionStartCoord > selectionEndCoord)
            selectionEndCoord = job.getRefAxisEndCoord();

        // Start pctId is the higher number since drags go top-left to bottom right, so swap them for readability
        if (selectionStartPctId > selectionEndPctId) {
            double tmp = selectionStartPctId;
            selectionStartPctId = selectionEndPctId;
            selectionEndPctId = tmp;
        }

        if (_logger.isDebugEnabled())
            _logger.debug("\npctId=" + selectionStartPctId + " - " + selectionEndPctId +
                    "\ncoords=" + selectionStartCoord + " - " + selectionEndCoord);

        return new FrvBounds(selectionStartPctId, selectionEndPctId, selectionStartCoord, selectionEndCoord);
    }

    /**
     * Convert a Frv region (bounded by percentId range and base pair range) into Lat/Long corners on the map.
     *
     * @param bounds Frv bounds object to convert
     * @param job    job to convert data for
     * @return returns the GLatLnd array values
     */
    public static LatLng[] convertFrvBoundsToLatLng(FrvBounds bounds, RecruitableJobInfo job) {
        // Calculate the NE and SW points as percentages of the world
        double pctIdRange = job.getPercentIdentityMax() - job.getPercentIdentityMin();
        double basePairRange = job.getRefAxisEndCoord() - job.getRefAxisBeginCoord();
        double neLatPctOfBounds = (bounds.getEndPctId() - job.getPercentIdentityMin()) / pctIdRange;
        double neLngPctOfBounds = (bounds.getEndBasePairCoord() - job.getRefAxisBeginCoord()) / basePairRange;
        double swLatPctOfBounds = (bounds.getStartPctId() - job.getPercentIdentityMin()) / pctIdRange;
        double swLngPctOfBounds = (bounds.getStartBasePairCoord() - job.getRefAxisBeginCoord()) / basePairRange;

        LatLng sw = LatLng.newInstance((TOTAL_DEGREES_LATITUDE * swLatPctOfBounds) - DEGREES_LATITUDE_PER_HEMISPHERE,
                (TOTAL_DEGREES_LONGITUDE * swLngPctOfBounds) - DEGREES_LONGITUDE_PER_HEMISPHERE);
        LatLng ne = LatLng.newInstance((TOTAL_DEGREES_LATITUDE * neLatPctOfBounds) - DEGREES_LATITUDE_PER_HEMISPHERE,
                (TOTAL_DEGREES_LONGITUDE * neLngPctOfBounds) - DEGREES_LONGITUDE_PER_HEMISPHERE);

        if (_logger.isDebugEnabled())
            _logger.debug("Converted bounds to lat/lng:\n" +
                    "   bp    range: " + bounds.getStartBasePairCoord() + "-" + bounds.getEndBasePairCoord() + "\n" +
                    "   pctId range: " + bounds.getStartPctId() + "-" + bounds.getEndPctId() + "\n" +
                    "   ne pct lat/lng : " + neLatPctOfBounds + "/" + neLngPctOfBounds + "\n" +
                    "   sw pct lat/lng : " + swLatPctOfBounds + "/" + swLngPctOfBounds + "\n" +
                    "   sw lat/lng : " + sw.getLatitude() + "/" + sw.getLongitude() + "\n" +
                    "   ne lat/lng : " + ne.getLatitude() + "/" + ne.getLongitude());
        return new LatLng[]{sw, ne};
    }
}
