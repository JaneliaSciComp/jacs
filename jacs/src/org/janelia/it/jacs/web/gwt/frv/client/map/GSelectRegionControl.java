/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
