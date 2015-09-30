
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Extension of GMercatorProjection that won't repeat tiles at low zoom levels.
 *
 * @author Michael Press
 * @see NonRepeatingGMercatorProjectionImpl
 */
public class NonRepeatingGMercatorProjection //extends MercatorProjection
{
    protected NonRepeatingGMercatorProjection(JavaScriptObject element) {
        //super(element);
    }

    public NonRepeatingGMercatorProjection(int zoomlevels) {
        this(NonRepeatingGMercatorProjectionImpl.create(zoomlevels));
    }

//    public static MercatorProjection narrowToGMercatorProjection(JavaScriptObject element)
//	{
//		return (element == null)?null: new NonRepeatingGMercatorProjection(element);
//	}
}
