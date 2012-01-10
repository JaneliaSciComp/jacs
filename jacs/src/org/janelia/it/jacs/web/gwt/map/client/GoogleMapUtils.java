
package org.janelia.it.jacs.web.gwt.map.client;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;


/**
 * Utility operations on Google Maps
 *
 * @author Michael Press
 */
public class GoogleMapUtils {
    public static final int DEGREES_LATITUDE_PER_HEMISPHERE = 90;
    public static final int DEGREES_LONGITUDE_PER_HEMISPHERE = 180;
    public static final int TOTAL_DEGREES_LATITUDE = DEGREES_LATITUDE_PER_HEMISPHERE * 2;
    public static final int TOTAL_DEGREES_LONGITUDE = DEGREES_LONGITUDE_PER_HEMISPHERE * 2;
    public static final int LEFT_LNG = -180;
    public static final int RIGHT_LNG = 180;

    /**
     * After a map drag in which the top or bottom edge is exposed, this method will move the map back to the edge.
     *
     * @param map map to restrict
     */
    public static void restrictTopAndBottomBounds(MapWidget map) {
        double lowfactor = map.getBounds().getNorthEast().getLatitude() - 89.5;  // how far down the top of the image is visible
        double highfactor = map.getBounds().getSouthWest().getLatitude() + 89.5; // how far up the bottom of the image is visible
        if (lowfactor > 0.25) {
            double oldLat = map.getBounds().getCenter().getLatitude();
            double newLat = oldLat - 3 * lowfactor;
            map.setCenter(LatLng.newInstance(newLat, map.getCenter().getLongitude()));
        }
        else if (highfactor < -0.25) {
            double oldLat = map.getBounds().getCenter().getLatitude();
            double newLat = oldLat - 3 * highfactor;
            map.setCenter(LatLng.newInstance(newLat, map.getCenter().getLongitude()));
        }
    }

    /**
     * After a map drag in which a left or right edge is exposed, this method will move the map back to the edge.
     *
     * @param map map to restrict
     */
    public static void restrictLeftAndRightBounds(MapWidget map) {
        double leftLng = map.getBounds().getSouthWest().getLongitude();
        double rightLng = map.getBounds().getNorthEast().getLongitude();

        int zoom = map.getZoomLevel();
        int safebound = 180; // right edge -180 to 0
        if (zoom == 3)
            safebound = 100; // right edge -180 to -80
        if (zoom == 4)
            safebound = 25;  // right edge -180 to -155

        //_logger.debug("   " + leftLng + " / " + rightLng + ", safe range = " + LEFT_LNG + " - " + (LEFT_LNG+safebound));
        if ((leftLng > LEFT_LNG + safebound) && (rightLng > LEFT_LNG + safebound) && leftLng > rightLng)
            map.setCenter(LatLng.newInstance(map.getCenter().getLatitude(), map.getCenter().getLongitude() + Math.abs(leftLng - 180.1)));
            //map.panTo(new GLatLng(map.getCenter().lat(), map.getCenter().lng() + Math.abs(leftLng - 180.1)));
        else if ((leftLng < RIGHT_LNG - safebound) && (rightLng < RIGHT_LNG - safebound) && leftLng > rightLng)
            map.setCenter(LatLng.newInstance(map.getCenter().getLatitude(), map.getCenter().getLongitude() - Math.abs(rightLng + 180.1)));
        //map.panTo(new GLatLng(map.getCenter().lat(), map.getCenter().lng() - Math.abs(rightLng + 180.1)));
        //_logger.debug("-->" + leftLng + " / " + rightLng + ", safe range = " + LEFT_LNG + " - " + (LEFT_LNG+safebound));
    }
}
