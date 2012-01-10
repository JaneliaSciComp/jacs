
package org.janelia.it.jacs.web.gwt.frv.client.map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.control.Control;
import com.google.gwt.maps.client.geom.LatLng;
import org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel;
import org.janelia.it.jacs.web.gwt.map.client.googleMaps.MapControllable;

/**
 * Custom Google Maps control to allow a region to be selected via rubber-band box
 *
 * @author Michael Press
 */
public class GSelectRegionControl extends Control implements MapControllable {
    /**
     * unused but retained for superclass consistency
     *
     * @param obj javascript object
     */
    protected GSelectRegionControl(JavaScriptObject obj) {
        super(obj);
    }

    public GSelectRegionControl(FrvImagePanel frv) {
        super(null);
        JavaScriptObject jsObj = GSelectRegionControlImpl.create(frv);
        //setJSObject(jsObj);
    }

    public static GSelectRegionControl narrowToGSelectRegionControl(JavaScriptObject element) {
        return (element == null) ? null : new GSelectRegionControl(element);
    }

    public void reset(FrvImagePanel frv) {
        GSelectRegionControlImpl.reset(GSelectRegionControlImpl.create(frv));
    }

    public void promptInit(FrvImagePanel frv) {
        GSelectRegionControlImpl.promptedInit(GSelectRegionControlImpl.create(frv));
    }

    public void redraw(FrvImagePanel frv, LatLng[] bounds) {
        LatLng sw = bounds[0];
        LatLng ne = bounds[1];
        GSelectRegionControlImpl.redraw(GSelectRegionControlImpl.create(frv), sw.getLatitude(), sw.getLongitude(),
                ne.getLatitude(), ne.getLongitude());
    }
}
