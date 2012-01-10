
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Extension of GEuclideanProjection that won't repeat tiles like a map of the earth does.
 *
 * @author Michael Press
 */
public class NonRepeatingGEuclideanProjectionImpl {
    public static native JavaScriptObject create(int zoomlevels)/*-{
        var proj = new $wnd.EuclideanProjection(zoomlevels);

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
