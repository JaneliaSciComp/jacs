
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.BusyListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;
import org.janelia.it.jacs.web.gwt.map.client.panel.MapBox;

import java.util.Iterator;
import java.util.Set;

/**
 * This class is responsible for managing the Sequence Geography panel contents in ReadDetail.
 *
 * @author Tareq Nabeel
 */
public class MapPanel {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.MapPanel");

    private SiteManager siteManager;
    private int googleMapHeight = 360;  // Unfortunately, this cannot be set through css at this point
    private int googleMapWidth = 450;   // Unfortunately, this cannot be set through css at this point

    private MapBox googleMapPanel;
    private Panel numSitesPanel;

    /**
     * This only creates the Sequence Geography panel (and loading message) but does not fill it
     *
     * @param siteManager SiteManager instance
     */
    public MapPanel(SiteManager siteManager) {
        this.siteManager = siteManager;
        createGoogleMapBox();
    }

    /**
     * Creates the outer structure of the Google map box.  Borrowed from JobDetail page //todo refactor to common call
     *
     * @return MapBox TitleBox containing the Google map
     */
    protected MapBox createGoogleMapBox() {
        try {
            // Panel for the map
            SimplePanel mapHints = new SimplePanel();
            mapHints.setStyleName("jobDetailsHitTableHint");
            mapHints.add(HtmlUtils.getHtml(
                    "&bull; Each sample site is marked on the map.<br>" +
                            "&bull; Click a site marker for more information on Site Metadata panel<br>", //TODO: enable double-click zooming
                    //TODO: add (external) link to Google Maps help - http://www.google.com/intl/en_us/help/maps/tour/
                    "hint"));

            numSitesPanel = new VerticalPanel();
            numSitesPanel.setStyleName("jobDetailsNumSitesPanel");
            numSitesPanel.setVisible(false);

            googleMapPanel = new MapBox("Sequence Geography", null);
            setGoogleMapStyleName("readDetailMapBox");
            googleMapPanel.add(numSitesPanel);
            googleMapPanel.add(HtmlUtils.getHtml("&nbsp;", "mapBoxSeparatorLine"));
            googleMapPanel.add(mapHints);
            return googleMapPanel;
        }
        catch (RuntimeException e) {
            logger.error("MapPanel createGoogleMapBox caught exception:" + e.getMessage());
            throw e;
        }
    }

    /**
     * This method is used to create or create the Google map for markders representing the locations of sites for a sample
     *
     * @param markers the markers to display on the map
     */
    protected void recreateGoogleMap(Set markers) {
        try {
            if (markers == null || markers.size() == 0) {
                getGoogleMapBox().setMessage("No geographical information is available for sample " + siteManager.getCurrentSample().getSampleName(), "text");
                getNumSitesPanel().setVisible(false);
            }
            else {
                GoogleMap map = new GoogleMap(((Marker) markers.iterator().next()).getPoint(), 3, markers, getGoogleMapWidth(), getGoogleMapHeight(), new MapCompleteListener());
                map.setMapType(MapType.getHybridMap());
                getGoogleMapBox().setMap(map);
                updateSiteNumLabel();
            }
        }
        catch (RuntimeException e) {
            logger.error("MapPanel recreateGoogleMap caught exception:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Listener to capture completion of map display.  We want to display the Sites after completion of the map
     * because display of sites causes selection of an individual site which triggers the markerHtml window to pop
     * on a marker.  That would not happen if the map hasn't fully loaded and would cause javascript errors intermittently
     */
    private class MapCompleteListener implements BusyListener {
        public void onBusy(Widget widget) {
        }

        public void onBusyDone(Widget widget) {
            try {
                //logger.debug("*****SiteManager MapCompleteListener onBusyDone");
                //Call this after map is complete to take advantage of Site Tab/Change listeners events
                siteManager.displayInitialSiteDataPanel();
            }
            catch (RuntimeException e) {
                logger.error("MapPanel MapCompleteListener onBusyDone caught exception " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Updates the number of sites label to reflect currentSites for the currentSample.  Each marker could refer to multiple sites
     * getSampleLocationTableCell(hit* Method borrowed from JobDetail page.  JobDetail page assumed one marker per site. // todo refactor to common method
     */
    private void updateSiteNumLabel() {
        try {
            // Text after the popperupper
            HTML text = HtmlUtils.getHtml(((siteManager.getCurrentSites().size() > 1) ? "are" : "is") + " represented in this data set", "text");
            DOM.setStyleAttribute(text.getElement(), "display", "inline");

            // Create a panel with ">> N sites are represented in this data set" and add the link and text
            HTMLPanel numSites = new HTMLPanel(
                    "<span class='greaterGreater'>&gt;&gt;&nbsp;</span>" +
                            "<span id='numSitesLink'></span>&nbsp;<span id='numSitesCaption'></span>");
            numSites.setStyleName("text"); // for the space
            DOM.setStyleAttribute(numSites.getElement(), "display", "inline");

            // PopperUpper to show the sites
            PopperUpperHTML link = new PopperUpperHTML(String.valueOf(siteManager.getCurrentSites().size()) + " sample site" + ((siteManager.getCurrentSites().size() > 1) ? "s" : ""),
                    getSampleSitePopup());
            link.setLauncher(new PopupBelowLauncher()); // show popup underneath
            DOM.setStyleAttribute(link.getElement(), "display", "inline");
            numSites.add(link, "numSitesLink");
            numSites.add(text, "numSitesCaption");

            getNumSitesPanel().clear();
            getNumSitesPanel().add(numSites);
            getNumSitesPanel().setVisible(true);
        }
        catch (RuntimeException e) {
            logger.error("MapPanel updateSiteNumLabel onBusyDone caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get popup indicating the sites shown on the map for the currentSample.  Each marker could refer to multiple sites
     * Method borrowed from JobDetail page.  JobDetail page assumed one marker per site. // todo refactor to common method
     *
     * @return html to popup when user hovers over numSitesPanel
     */
    private HTML getSampleSitePopup() {
        try {
            StringBuffer buf = new StringBuffer();
            Iterator iter = siteManager.getCurrentSites().iterator();
            while (iter.hasNext()) {
                BioMaterial site = (BioMaterial) iter.next();
                buf.append("<span class='infoPrompt'>&bull;&nbsp; ");
                buf.append(site.getMaterialAcc());
                buf.append("&nbsp;</span>");
                buf.append("<span class='infoText'>");
                buf.append(site.getCollectionSite().getLocation());
                buf.append("</span>");
                buf.append("<br>");
            }
            return HtmlUtils.getHtml(buf.toString(), "infoText");
        }
        catch (RuntimeException e) {
            logger.error("MapPanel getSampleSitePopup onBusyDone caught exception " + e.getMessage());
            throw e;
        }
    }

    protected void setMapError() {
        getGoogleMapBox().setMessage("Error retrieving geographical information.", "error");
        getNumSitesPanel().setVisible(false);
    }

    protected void setMapNoDataMessage() {
        getGoogleMapBox().setMessage("No geographical information is available for these sequences.", "text");
        getNumSitesPanel().setVisible(false);
    }

    private Panel getNumSitesPanel() {
        return numSitesPanel;
    }

    public MapBox getGoogleMapBox() {
        return googleMapPanel;
    }

    public int getGoogleMapHeight() {
        return googleMapHeight;
    }

    public void setGoogleMapHeight(int height) {
        googleMapHeight = height;
    }

    public int getGoogleMapWidth() {
        return googleMapWidth;
    }

    public void setGoogleMapWidth(int width) {
        googleMapWidth = width;
    }

    public void setGoogleMapStyleName(String styleName) {
        googleMapPanel.setStyleName(styleName);
    }
}
