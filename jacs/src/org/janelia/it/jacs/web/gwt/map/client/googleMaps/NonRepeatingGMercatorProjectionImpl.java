
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Implementation for NonRepeatingGMercatorProjection, which is an extension of GMercatorProjection, with one
 * behavioral difference - the tileCheckRange() method is overriden to tell Google Maps not to wrap tiles outside
 * of the main "world view".  That is, show only 1 set of tiles for the map, but when the user drags the map to
 * the outside boundaries, the map tiles are not repeated past the boundary.  At the extreme (zoom level 0 - zoomed
 * all the way out), the entire image will consist of 1 tile, which will not be repeated horizontally or vertically.
 *
 * @author Michael Press
 */
public class NonRepeatingGMercatorProjectionImpl {
    public static native JavaScriptObject create(int zoomlevels)/*-{
        var proj = new $wnd.GMercatorProjection(zoomlevels);

        proj.tileCheckRange = function(tile, zoom, tilesize) {
            var tileBounds = Math.pow(2, zoom);
            if (tile.y < 0 || tile.y >= tileBounds)
                return false;
            else if (tile.x < 0 || tile.x >= tileBounds)
                return false;
            else
                return true;
      }

      return proj;
	}-*/;
}