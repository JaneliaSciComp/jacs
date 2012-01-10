
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Projection;
import com.google.gwt.maps.client.geom.TileIndex;

/**
 * Extension of GProjection that represents the map as a flat surface, not a sphere.
 *
 * @author Michael Press
 * @see org.janelia.it.jacs.web.gwt.map.client.googleMaps.NonRepeatingGMercatorProjectionImpl
 */
public class GEuclideanProjection extends Projection {
    protected GEuclideanProjection(JavaScriptObject element) {
        super(element);
    }

    @Override
    public Point fromLatLngToPixel(LatLng latLng, int i) {
        return null;
    }

    @Override
    public LatLng fromPixelToLatLng(Point point, int i, boolean b) {
        return null;
    }

    @Override
    public double getWrapWidth(int i) {
        return 0;
    }

    @Override
    public boolean tileCheckRange(TileIndex tileIndex, int i, int i1) {
        return false;
    }

    public GEuclideanProjection(int zoomlevels) {
        this(GEuclideanProjectionImpl.create(zoomlevels));
    }

    public static GEuclideanProjection narrowToGEuclideanProjection(JavaScriptObject element) {
        return (element == null) ? null : new GEuclideanProjection(element);
    }
}
