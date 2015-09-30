
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.util.*;


/**
 * The class keeps the Map panel markers in sync with Site panel sites so that user selections on Map panel would cause the corresponding
 * site to surface and conversely user selections on Sites panel would cause map marker html to popup accordingly
 *
 * @author Tareq Nabeel
 */
public class PointManager {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.bse.client.PointManager");

    // Mapping of GLatLng.toString() to MapPoint - which contains a GMarker and a list of sites
    // Unfortunately, GLatLng does not implement equals() and hashCode(), so we have store GLatLng.toString() instead
    private Map pointMPointMap = new HashMap();

    private SiteManager siteManager;

    public PointManager(SiteManager siteManager) {
        this.siteManager = siteManager;
    }

    /**
     * Creates the Google map markers for the given sites.
     *
     * @param sites the sites to create markers for
     * @return a set of markers needed by Google map to display
     */
    protected Set createSiteMarkers(List sites) {
        pointMPointMap.clear();
        if (sites == null) {
            return null;
        }
        Set markers = new HashSet();
        try {
            for (Iterator iterator = sites.iterator(); iterator.hasNext();) {
                BioMaterial site = (BioMaterial) iterator.next();
                if (site.getCollectionSite() != null) {
                    if (((GeoPoint) site.getCollectionSite()).getLatitudeAsDouble() != null &&
                            ((GeoPoint) site.getCollectionSite()).getLongitudeAsDouble() != null) {
                        Marker gMarker = addMarker(site);
                        if (gMarker != null) markers.add(gMarker);
                    }
                }
            }
            return markers;
        }
        catch (RuntimeException e) {
            logger.error("SiteManager createSiteMarkers caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a marker for the site if it's location isn't already represented on the map.  If it is represented already,
     * then the site is added to the map point's collection of sites
     *
     * @param site the site to create marker for
     * @return GMarker instance
     */
    protected Marker addMarker(BioMaterial site) {
        try {
            LatLng gLatLng = LatLng.newInstance(((GeoPoint) site.getCollectionSite()).getLatitudeAsDouble().doubleValue(),
                    ((GeoPoint) site.getCollectionSite()).getLongitudeAsDouble().doubleValue());
            Marker gMarker = null;
            // Check if the site's location (GLatLng) already exists on the map
            // If it doesn't, creat a new MapPOint
            // Otherwise, add the site to the list of sites referenced by that point
            MapPoint mapPoint = getMapPoint(site);
            if (mapPoint == null) {
                gMarker = createMarker(gLatLng);
                mapPoint = new MapPoint(gMarker, site);
            }
            else {
                mapPoint.addSite(site);
            }
            pointMPointMap.put(mapPoint.getPoint().toString(), mapPoint);
            return gMarker;
        }
        catch (RuntimeException e) {
            logger.error("PointManager addSite caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create a Google map marker for the given latitude/longitude
     *
     * @param gLatLng the given Google GLatLng instance representing a latitude/longitude
     * @return GMarker instance
     */
    protected Marker createMarker(LatLng gLatLng) {
        Marker marker = new Marker(gLatLng);
        MyMarkerClickHandler handler = new MyMarkerClickHandler();
        marker.addMarkerClickHandler(handler);
        // markers.add(marker); WILL NOT WORK - MarkerClickListeners won't fire if you store markers in instance variable
        // before displaying map
        return marker;
    }

    /**
     * Return the sites represented by the given marker
     *
     * @param marker GMarker instance
     * @return the sites represented by the given marker
     */
    protected List getSites(Marker marker) {
        try {
            MapPoint mapPoint = getMapPoint(marker.getLatLng());
            //logger.debug("PointManager getSites(GMarker marker) fetched mapPoint="+mapPoint.toString() + "\nand returned sites=" + mapPoint.getSites());
            return mapPoint.getSites();
        }
        catch (RuntimeException e) {
            logger.error("PointManager getSite(GMarker marker) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Return the site indices on the listbox or tab panel (Site Metadata box) represented by the given marker
     *
     * @param marker GMarker instance
     * @return the site indices on the listbox or tab panel (Site Metadata box)
     */
    private List getSiteTabIndices(Marker marker) {
        try {
            List sites = getSites(marker);
            List tabIndices = new ArrayList();
            for (Iterator iterator = sites.iterator(); iterator.hasNext();) {
                BioMaterial site = (BioMaterial) iterator.next();
                tabIndices.add(new Integer(siteManager.getCurrentSites().indexOf(site)));
            }
            //logger.debug("PointManager getSiteTabIndex site.getSiteId()=" + (site == null ? "null" : site.getSiteId()));
            return tabIndices;
        }
        catch (RuntimeException e) {
            logger.error("PointManager getSiteTabIndex(GMarker marker) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the MapPoint representing the site index on the listbox or tab panel (Site Metadata box)
     *
     * @param tabIndex the site index on the listbox or tab panel (Site Metadata box)
     * @return the MapPoint instance
     */
    protected MapPoint getMapPoint(int tabIndex) {
        try {
            BioMaterial site = (BioMaterial) siteManager.getCurrentSites().get(tabIndex);
            MapPoint mPoint = getMapPoint(site);
            return mPoint;
        }
        catch (RuntimeException e) {
            logger.error("PointManager getMapPoint(int tabIndex) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the GMarker instance representing the given site index on the listbox or tab panel (Site Metadata box)
     *
     * @param tabIndex the site index on the listbox or tab panel (Site Metadata box)
     * @return the GMarker instance
     */
    protected Marker getMarker(int tabIndex) {
        try {
            return getMapPoint(tabIndex).getMarker();
        }
        catch (RuntimeException e) {
            logger.error("PointManager getMarker(int tabIndex) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the GMarker popup html to pop-up for given the site index on the listbox or tab panel (Site Metadata box)
     *
     * @param tabIndex the site index on the listbox or tab panel (Site Metadata box)
     * @return Returns the GMarker popup html
     */
    protected String getMarkerHtml(int tabIndex) {
        try {
            return getMapPoint(tabIndex).getMarkerHtml();
        }
        catch (RuntimeException e) {
            logger.error("PointManager getMarkerHtml(int tabIndex) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the MapPoint instance corresponding to the GLatLng instance
     *
     * @param gLatLng GLatLng instance
     * @return the MapPoint instance
     */
    protected MapPoint getMapPoint(LatLng gLatLng) {
        try {
            MapPoint mPoint = (MapPoint) pointMPointMap.get(gLatLng.toString());
            //logger.debug("PointManager getMapPoint mPoint.getPoint()=" + (mPoint == null ? "null" : mPoint.getPoint().toString()));
            return mPoint;
        }
        catch (RuntimeException e) {
            logger.error("PointManager getMapPoint(GLatLng gLatLng) caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the MapPoint instance corresponding to the given site.
     *
     * @param site Site instance
     * @return MapPoint instance
     */
    protected MapPoint getMapPoint(BioMaterial site) {
        if (site.getCollectionSite() == null) return null;
        LatLng gLatLng = LatLng.newInstance(((GeoPoint) site.getCollectionSite()).getLatitudeAsDouble().doubleValue(),
                ((GeoPoint) site.getCollectionSite()).getLongitudeAsDouble().doubleValue());
        return getMapPoint(gLatLng);
    }

    /**
     * Listener to capture user clicks on Google map markers
     */
    private class MyMarkerClickHandler implements MarkerClickHandler {
        public void onClick(MarkerClickEvent event) {
            try {
                List tabIndices = getSiteTabIndices(event.getSender());
                //logger.debug("PointManager MarkerEventClickListener tabIndices=" + tabIndices.toString());
                int firstTabIndex = ((Integer) tabIndices.get(0)).intValue();
                siteManager.selectSiteOption(firstTabIndex);
            }
            catch (RuntimeException e) {
                logger.error("PointManager MarkerEventClickListener onClick caught exception " + e.getMessage());
                throw e;
            }
        }

        public void onDblClick(Marker gMarker) {
        }
    }

}
