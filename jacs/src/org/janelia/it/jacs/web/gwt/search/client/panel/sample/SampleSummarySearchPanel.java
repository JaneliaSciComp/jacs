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

package org.janelia.it.jacs.web.gwt.search.client.panel.sample;

import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SampleSummarySearchPanel extends CategorySummarySearchPanel {

    public SampleSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new SampleSearchDataBuilder(searchId, searchQuery);
    }

    public void populatePanel() {
        addSearchIcon();
        addLoadingMessage();
        getDataBuilder().getSearchResultCharts();
    }

    public void populateResultCharts(List resultChartNames) {
        super.populateResultCharts(resultChartNames);
        ((SampleSearchDataBuilder) getDataBuilder()).retrieveMapInfo();
    }

    void populateMap(Set sites) {
        Set<Marker> markers = getSiteMarkers(sites);
        VerticalPanel mapPanel = new VerticalPanel();

        LatLng mapCenter = null;
        if (markers.size() > 0) {
            mapCenter = (markers.iterator().next()).getLatLng();
        }
        int zoomControlWidth = 2;
        int zoomControlHeight = 3;
        int mapTypeControlWidth = -1; // nasty flag for no map type control
        int mapTypeControlHeight = -1; // nasty flag for no map type control
        GoogleMap map = new GoogleMap(
                MapType.getHybridMap(),
                3,/*zoom*/
                mapCenter,
                null,
                markers,
                250, /*width*/
                200, /*height*/
                GoogleMap.SMALLZOOMCONTROL,
                zoomControlWidth,
                zoomControlHeight,
                mapTypeControlWidth,
                mapTypeControlHeight,
                true, /*realizeNow*/
                null/*busyListener*/);

        SimplePanel mapWrapper = new SimplePanel();
        mapWrapper.setStyleName("SampleSearchMap"); // puts border on map
        mapWrapper.add(map);

        mapPanel.add(HtmlUtils.getHtml("Geographic Location", "SampleSearchMapTitle"));
        mapPanel.add(mapWrapper);

        addItem(mapPanel);
    }

    private Set<Marker> getSiteMarkers(Set sites) {
        Set<Marker> markers = new HashSet<Marker>();
        for (Object site1 : sites) {
            Site site = (Site) site1;
            if (site.getLatitudeDouble() != null && site.getLongitudeDouble() != null) {
                final Marker marker = new Marker(LatLng.newInstance(
                        site.getLatitudeDouble(),
                        site.getLongitudeDouble()));
                final String html = getSiteMarkerHtml(site);
                marker.addMarkerClickHandler(new MarkerClickHandler() {
                    public void onClick(MarkerClickEvent markerClickEvent) {
                        markerClickEvent.getSender().showMapBlowup(new InfoWindowContent(html));
                    }
                });
                markers.add(marker);
            }
        }
        return markers;
    }

    private String getSiteMarkerHtml(Site site) {
        return
                "<span class='infoPrompt'>" + site.getSiteId() + "</span><br>" +
                        "<span class='infoText'>" + site.getSampleLocation() + "</span>";
    }

}
