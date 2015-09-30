
package org.janelia.it.jacs.web.gwt.map.client;

import com.google.gwt.maps.client.geom.LatLng;


/**
 * @author Michael Press
 */
public interface MapMoveRequestListener {
    public void moveTo(LatLng center);
}
