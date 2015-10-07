
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
