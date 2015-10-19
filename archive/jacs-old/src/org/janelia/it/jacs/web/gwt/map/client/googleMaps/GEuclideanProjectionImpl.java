
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Implements a projection of a flat map instead of the normal Mercator sphere-to-flat-map projection.
 * Relies on the EuclideanProjection JavaScript object in webroot/js/euclideanProjection.js
 *
 * @author Michael Press
 */
public class GEuclideanProjectionImpl {
    public static native JavaScriptObject create(int zoomlevels)/*-{
        return new $wnd.EuclideanProjection(zoomlevels);
    }-*/;
}
