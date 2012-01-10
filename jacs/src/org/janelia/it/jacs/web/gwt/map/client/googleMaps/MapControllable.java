
package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.maps.client.geom.LatLng;
import org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 27, 2007
 * Time: 1:45:42 PM
 */
public interface MapControllable {
    public void reset(FrvImagePanel frv);

    public void redraw(FrvImagePanel frv, LatLng[] bounds);
}
