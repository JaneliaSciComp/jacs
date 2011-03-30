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

package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is responsible for storing the list of sites that a Google map marker refers to
 *
 * @author Tareq Nabeel
 */
public class MapPoint {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.PointManager");

    private Marker marker;
    private List sites = new ArrayList();

    public MapPoint(Marker marker, BioMaterial site) {
        this.marker = marker;
        sites = new ArrayList();
        sites.add(site);
    }

    public void addSite(BioMaterial site) {
        sites.add(site);
    }

    public LatLng getPoint() {
        return marker.getLatLng();
    }

    public Marker getMarker() {
        return marker;
    }


    public List getSites() {
        return sites;
    }

    protected String getMarkerHtml() {
        try {
            //logger.debug("MapPoint getMarkerHtml sites.size()=" + sites.size());
            StringBuffer buff = new StringBuffer();
            for (Iterator iterator = sites.iterator(); iterator.hasNext();) {
                BioMaterial site = (BioMaterial) iterator.next();
                buff.append("<span class='infoPrompt'>");
                buff.append(site.getMaterialAcc());
                buff.append("</span><br>");
                buff.append("<span class='infoText'>");
                buff.append(site.getCollectionSite().getLocation());
                buff.append("</span><br><br>");
            }
            return buff.toString();
        }
        catch (RuntimeException e) {
            logger.error("SiteManager getSiteMarkerHtml caught exception " + e.getMessage());
            throw e;
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        MapPoint that = (MapPoint) o;

        // GLatLng.equals() isn't implemented unfortunately
        return marker.getPoint().toString().equals(that.marker.getPoint().toString());
    }

    public int hashCode() {
        // GLatLng.hashCode() isn't implemented unfortunately
        return marker.getPoint().toString().hashCode();
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("GLatLng:" + this.getPoint().toString());
        buff.append(" GMarker:" + this.getMarker().toString());
        //buff.append(" MyMarker Html:"+this.getMarkerHtml());
        return buff.toString();
    }
}
