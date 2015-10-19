
package org.janelia.it.jacs.web.gwt.frv.client.panels;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.style.Color;
import org.gwtwidgets.client.wrap.JsGraphicsPanel;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.panel.DraggableTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.map.client.MapMoveRequestListener;

/**
 * Panel to show the overview of the entire FRV.  Shows the 0,0,0 tile (the only tile at zoom level 0), and overlays
 * a purple box showing where the main FRV map is zoomed into, just like the Google Maps overview thing in the corner.
 *
 * @author Michael Press
 */
public class FrvOverviewPanel extends DraggableTitledBox implements IsJobSettable, MapClickHandler {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.panels.FrvOverviewPanel");

    private Image _image;
    private JsGraphicsPanel _graphPanel;
    private MapMoveRequestListener _mapMoveRequestListener;
    private SimplePanel _hintPanel;

    private static final String ZOOM_BOX_DIV_NAME = "zoomBoxDiv";
    public static final int IMAGE_SQUARE_WIDTH = 200;
    public static final int IMAGE_SQUARE_HEIGHT = 200;
    private static final int DEFAULT_ZOOM = 1;

    private static final int DEGREES_LATITUDE_PER_HEMISPHERE = 90;
    private static final int DEGREES_LONGITUDE_PER_HEMISPHERE = 180;
    private static final int TOTAL_DEGREES_LATITUDE = DEGREES_LATITUDE_PER_HEMISPHERE * 2;
    private static final int TOTAL_DEGREES_LONGITUDE = DEGREES_LONGITUDE_PER_HEMISPHERE * 2;

    public FrvOverviewPanel(String title, MapMoveRequestListener mapMoveRequestListener, DragController dragController) {
        super(title, dragController);
        setStyleName("frvOverviewPanel");
        setOverviewSize();
        updateHint(DEFAULT_ZOOM);
        _mapMoveRequestListener = mapMoveRequestListener;
    }

    protected void popuplateContentPanel() {
        //TODO: put this in CSS file
        DOM.setStyleAttribute(getElement(), "position", "absolute");
        DOM.setStyleAttribute(getElement(), "left", "670px");
        DOM.setStyleAttribute(getElement(), "top", "100px");

        // Create a named, empty zoomBoxDiv for the JsGraphicsPanel to use to draw the purple zoom box
        HTML zoomBoxDiv = new HTML();
        DOM.setElementProperty(zoomBoxDiv.getElement(), "id", ZOOM_BOX_DIV_NAME);
        zoomBoxDiv.setStyleName("frvOverviewZoomBoxDiv");
        add(zoomBoxDiv);

        // Create the Image;  the URL to find the zoom-level-0 tile will be set when the job is known
        _image = new DoubleClickableImage();
        _image.setStyleName("frvOverviewImage");
        add(_image);
        getContentPanel().setCellHorizontalAlignment(_image, VerticalPanel.ALIGN_CENTER); // center the image

        // Add a hint about double-clicking
        _hintPanel = new SimplePanel();
        _hintPanel.setWidth("100%");
        _hintPanel.setStyleName("frvOverviewPanelHint");
        _hintPanel.add(HtmlUtils.getHtml("Double-click to change region", "hint"));
        add(_hintPanel);
    }

    /**
     * Extension of regular Image class to capture double-click requests.  On a double click, we'll calculate
     * the lat/long that correspond to the point clicked, and notify the map to update itself
     */
    public class DoubleClickableImage extends Image {
        public DoubleClickableImage() {
            sinkEvents(Event.ONDBLCLICK);
        }

        /**
         * When the user double-clicks on the image, calculate the x and y position on the image, then determine the
         * percentage of the entire image (which represents the whole "world") that x and y represent. Figure out the
         * lat and long that correspond to the x and y percentage of the world, and update the map.
         */
        public void onBrowserEvent(Event event) {
            if (DOM.eventGetType(event) == Event.ONDBLCLICK) {
                int vertScroll = DOM.getAbsoluteTop(DOM.getParent(RootPanel.getBodyElement()));
                int horizScroll = DOM.getAbsoluteLeft(DOM.getParent(RootPanel.getBodyElement()));
                int x = DOM.eventGetClientX(event) - _image.getAbsoluteLeft() + horizScroll;
                int y = DOM.eventGetClientY(event) - _image.getAbsoluteTop() + vertScroll;
                double pctX = (double) x / _image.getWidth();
                double pctY = (double) y / _image.getHeight();
                double lat = (pctY * TOTAL_DEGREES_LATITUDE - DEGREES_LATITUDE_PER_HEMISPHERE) * -1;
                double lng = pctX * TOTAL_DEGREES_LONGITUDE - DEGREES_LONGITUDE_PER_HEMISPHERE;

                _mapMoveRequestListener.moveTo(LatLng.newInstance(lat, lng));
            }
            else // let superclass handle other events
                super.onBrowserEvent(event);
        }
    }

    /**
     * Update the overview image with the 0,0,0 tile for the specified job (using a timer so the page has a chance
     * to display first, or the initial image won't come up.
     */
    public void setJob(RecruitableJobInfo job) {
        _image.setVisible(false);
        new OverviewImageTimer(job).schedule(1000);
    }

    public class OverviewImageTimer extends Timer {
        private RecruitableJobInfo _job;

        public OverviewImageTimer(RecruitableJobInfo job) {
            _job = job;
        }

        public void run() {
            if (_job != null) {
                setImage(_job);
                setVisible(true);
            }
        }
    }

    private void setImage(RecruitableJobInfo job) {
        if (job != null)
            _image.setUrl(FrvImagePanel.getTileUrl(0, 0, 0, job));
        _image.setVisible(true);
        setOverviewSize();
        drawZoomOutline(1.0, 0.0, 0.0, 1.0); // entire image
    }

    private void setOverviewSize() {
        _image.setHeight(IMAGE_SQUARE_HEIGHT + "px");
        _image.setWidth(IMAGE_SQUARE_WIDTH + "px");
    }

    /**
     * Fulfills interface GEventHandler.  This handler is called when the main map changes, so we can update the
     * little zoom box.
     */
    public void onClick(MapClickEvent clickEvent) {
        // todo do the update
//        MapWidget map = MapWidget.narrowToGMap2(jsObject);
//        updateHint(map.getZoomLevel());
//        updateZoomBox(map);
    }

    private void updateHint(int zoomLevel) {
        if (zoomLevel < 2)
            _hintPanel.setVisible(false);
        else
            _hintPanel.setVisible(true);
    }

    private void updateZoomBox(MapWidget map) {
        // Convert the NE and SW points to a 0..180 scale (lat) and 0..360 scale (long)
        double neLat = map.getBounds().getNorthEast().getLatitude() + DEGREES_LATITUDE_PER_HEMISPHERE;
        double neLng = map.getBounds().getNorthEast().getLongitude() + DEGREES_LONGITUDE_PER_HEMISPHERE;
        double swLat = map.getBounds().getSouthWest().getLatitude() + DEGREES_LATITUDE_PER_HEMISPHERE;
        double swLng = map.getBounds().getSouthWest().getLongitude() + DEGREES_LONGITUDE_PER_HEMISPHERE;
        double ctrLng = map.getCenter().getLongitude() + DEGREES_LONGITUDE_PER_HEMISPHERE;

        // We get the NE and SW points from Google Maps, but we really need the NW and SE points
        // for the JsGraphicsPanel rectangle
        double nwLng = swLng;
        double seLng = neLng;

        // If the map is dragged such that the left or right edge is visible, the longitude wraps and the box will
        // be drawn incorrectly, so reset the wrapped longitude to the outer bound.  Note - this reset will fail
        // if the edge is dragged beyond the halfway point, so elsewhere we've restriced drag to 25% of the map area.
        if (nwLng > ctrLng)
            nwLng = 0; // leftmost point
        else if (seLng < ctrLng)
            seLng = 360; // rightmost point

        // Compute NW and SE points as a percentage of the world
        double nwLatPct = neLat / TOTAL_DEGREES_LATITUDE;
        double nwLngPct = nwLng / TOTAL_DEGREES_LONGITUDE;
        double seLatPct = swLat / TOTAL_DEGREES_LATITUDE;
        double seLngPct = seLng / TOTAL_DEGREES_LONGITUDE;

        drawZoomOutline(nwLatPct, nwLngPct, seLatPct, seLngPct);
    }

    private void drawZoomOutline(double nwLatPct, double nwLngPct, double seLatPct, double seLngPct) {
        //TODO: find a way to do this check once instead of on every move event
        try {
            if (_graphPanel == null) {
                _graphPanel = new JsGraphicsPanel(ZOOM_BOX_DIV_NAME);
                //TODO: find a way to get this in CSS
                //_graphPanel.setColor(new Color(212, 179, 255));  // lighter purple
                _graphPanel.setColor(new Color(0, 0, 255));        // "link" blue
                _graphPanel.setStrokeWidth(2);
            }
        }
        catch (Exception e) {
            _logger.warn("Exception drawing zoom box: " + e.getMessage()); // This just happens in hosted mode when viewing fake data with no overview
            return;
        }

        // top left of box
        int x1 = (int) (nwLngPct * IMAGE_SQUARE_WIDTH);
        int y1 = (IMAGE_SQUARE_HEIGHT - (int) (nwLatPct * IMAGE_SQUARE_HEIGHT)); // can't use _image.getHeight() on initial draw

        // bottom right of box
        int x2 = (int) (seLngPct * IMAGE_SQUARE_WIDTH);
        int y2 = (IMAGE_SQUARE_HEIGHT - (int) (seLatPct * IMAGE_SQUARE_HEIGHT));

        int width = x2 - x1;
        int height = y2 - y1;

        if (_logger.isDebugEnabled())
            _logger.debug("drawing FrvOverviewPanel zoom box at (" + x1 + "," + y1 + "), width=" + width + ", height=" + height);

        _graphPanel.clear();
        _graphPanel.drawRect(x1, y1, width, height);
        _graphPanel.paint();
    }
}
