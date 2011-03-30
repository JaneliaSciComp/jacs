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

package org.janelia.it.jacs.web.gwt.map.client;

import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Polyline;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

public class MapTest extends BaseEntryPoint {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.Status");

    public void onModuleLoad() {
        // Create and fade in the page contents
        _logger.debug("in MapTest()");

        // Setup
        clear();
        setBreadcrumb(new Breadcrumb("Google Maps"), Constants.ROOT_PANEL_NAME);

        // Show it
        RootPanel.get(Constants.ROOT_PANEL_NAME).add(createMap());
        show();
    }

    public Widget createMap() {
        _logger.debug("creating map");

        GoogleMap map = new GoogleMap(getSites()[0], /*zoom*/3, /*markers*/null, 400, 400);
        map.setMapType(MapType.getNormalMap());
        drawPolylines(getSites(), map);

        _logger.debug("map complete");
        return map;
    }

    private LatLng[] getSites() {
        LatLng[] sites = new LatLng[5];
        sites[0] = LatLng.newInstance(10, -80);
        sites[1] = LatLng.newInstance(10, -60);
        sites[2] = LatLng.newInstance(30, -60);
        sites[3] = LatLng.newInstance(30, -80);
        sites[4] = LatLng.newInstance(10, -80);
        return sites;
    }

    private void drawPolylines(LatLng[] sites, GoogleMap map) {
        map.getMapWidget().addOverlay(new Polyline(sites));
    }
}
