
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Extension of GEuclideanProjection that won't repeat tiles like a map of the earth does.
 *
 * @author Michael Press
 * @see NonRepeatingGMercatorProjectionImpl
 */
public class NonRepeatingGEuclideanProjection extends GEuclideanProjection {
    protected NonRepeatingGEuclideanProjection(JavaScriptObject element) {
        super(element);
    }

    public NonRepeatingGEuclideanProjection(int zoomlevels) {
        this(NonRepeatingGEuclideanProjectionImpl.create(zoomlevels));
    }

    public static GEuclideanProjection narrowToEuclideanProjection(JavaScriptObject element) {
        return (element == null) ? null : new NonRepeatingGEuclideanProjection(element);
    }
}
