
package org.janelia.it.jacs.web.gwt.map.client;

import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.control.*;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.BusyListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.BusyListenerManager;
import org.janelia.it.jacs.web.gwt.common.client.ui.BusyNotifier;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Provides a Google Map.  Note that the caller MUST invoke realize() either by passing true to the constructor
 * or invoking explicitly later.  This is due to display problems that arise unless the configuration step of
 * the map is done on a timer after the browser completes the render of the current page.
 */
public class GoogleMap extends Widget implements BusyNotifier {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.map.client.GoogleMap");

    private MapWidget _mapWidget;
    private LatLng _mapCenter;
    private LatLng _marker;
    private int _zoomLevel = DEFAULT_ZOOM_LEVEL;
    private BusyListenerManager _busyListenerMgr = new BusyListenerManager(this);
    private int _height = DEFAULT_HEIGHT;
    private int _width = DEFAULT_WIDTH;
    private MapType _gMapType;
    private Set<Marker> _markers = null;
    private int _zoomControlType;
    private int _zoomControlWidth;
    private int _zoomControlHeight;
    private int _mapTypeControlWidth;
    private int _mapTypeControlHeight;
    private String _markerDirectory;

    public static final int LARGEZOOMCONTROL = 0;
    public static final int SMALLZOOMCONTROL = 1;

    private static final int DEFAULT_HEIGHT = 300;
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_ZOOM_LEVEL = 3;
    private static final int DEFAULT_ZOOM_LEVEL_ONE_MARKER = 18;

    public static final String MARKER_DIRECTORY_PROPERTY = "GoogleMapsApi.CustomMarkerDirectory";

    public GoogleMap(int zoom, int width, int height) {
        this(null, zoom, (Set<Marker>) null, width, height);
    }

    public GoogleMap(LatLng mapCenter, LatLng marker, boolean realizeNow) {
        this(null, DEFAULT_ZOOM_LEVEL, mapCenter, marker, DEFAULT_HEIGHT, DEFAULT_WIDTH, realizeNow, null);
    }

    public GoogleMap(LatLng marker, int height, int width) {
        this(null, DEFAULT_ZOOM_LEVEL, marker, marker, height, width, true, null);
    }

    public GoogleMap(MapType gMapType, int zoomLevel, LatLng marker, int height, int width) {
        this(gMapType, zoomLevel, marker, marker, height, width, true, null);
    }

    public GoogleMap(LatLng marker, int height, int width, boolean realizeNow) {
        this(null, DEFAULT_ZOOM_LEVEL, marker, marker, height, width, realizeNow, null);
    }

    public GoogleMap(MapType gMapType,
                     int zoomLevel,
                     LatLng mapCenter,
                     LatLng marker,
                     int height,
                     int width,
                     boolean realizeNow,
                     BusyListener listener) {
        this(gMapType,
                zoomLevel,
                mapCenter,
                marker,
                null,
                width,
                height,
                LARGEZOOMCONTROL,
                0,
                0,
                0,
                0,
                realizeNow,
                listener);
    }

    public GoogleMap(LatLng mapCenter, int zoomLevel, Set<Marker> markers, int width, int height) {
        this(mapCenter, zoomLevel, markers, width, height, null);
    }

    public GoogleMap(LatLng mapCenter,
                     int zoomLevel,
                     Set<Marker> markers,
                     int width,
                     int height,
                     BusyListener listener) {
        this(null,
                zoomLevel,
                mapCenter,
                mapCenter,
                markers,
                width,
                height,
                LARGEZOOMCONTROL,
                0,
                0,
                0,
                0,
                true,
                listener);
    }

    public GoogleMap(MapType gMapType,
                     int zoomLevel,
                     LatLng mapCenter,
                     LatLng marker,
                     Set<Marker> markers,
                     int width,
                     int height,
                     int zoomControlType,
                     int zoomControlWidth,
                     int zoomControlHeight,
                     int mapTypeControlWidth,
                     int mapTypeControlHeight,
                     boolean realizeNow,
                     BusyListener listener) {
        _mapCenter = mapCenter;
        _marker = marker;
        _markers = markers;
        _height = height;
        _width = width;
        _zoomLevel = zoomLevel;

        _gMapType = gMapType;
        _zoomControlType = zoomControlType;
        _zoomControlWidth = zoomControlWidth;
        _zoomControlHeight = zoomControlHeight;
        _mapTypeControlWidth = mapTypeControlWidth;
        _mapTypeControlHeight = mapTypeControlHeight;
        addBusyListener(listener);

        if (realizeNow) {
            realize();
        }
    }

    public Element getElement() {
        return _mapWidget.getElement();
    }

    public void realize() {
        _logger.debug("realizing map");

        String url = getMarkerDirectory();
        if (url != null)
            _markerDirectory = url;
        else
            _logger.error("Failed to retrieve " + MARKER_DIRECTORY_PROPERTY + " property; can't get URL for custom markers");

        // Have to create the map and add it to the DOM before we configure it or else it won't display right
        if (_mapCenter == null) {
            _mapWidget = new MapWidget();
            _mapWidget.setHeight(String.valueOf(_height));
            _mapWidget.setWidth(String.valueOf(_width));
            _mapWidget.setZoomLevel(_zoomLevel);
        }
        else {
            _mapWidget = new MapWidget();
            _mapWidget.setCenter(_mapCenter);
            _mapWidget.setHeight(String.valueOf(_height));
            _mapWidget.setWidth(String.valueOf(_width));
            _mapWidget.setZoomLevel(_zoomLevel);
        }


        // Notify listeners of impending busy period (until map is done displaying)
        _busyListenerMgr.notifyOnBusy();

        // Configure map using a timer or else IE doesn't get offsets right and the whole page layout is messed up
        new GoogleMap.DisplayMapTimer().schedule(1);
    }

    public static String getMarkerDirectory() {
        return SystemProps.getString(MARKER_DIRECTORY_PROPERTY, /*default value*/ null);
    }

    protected class DisplayMapTimer extends Timer {
        public void run() {
            configureMap();
            _busyListenerMgr.notifyOnBusyComplete();
        }
    }

    private void configureMap() {
        _logger.debug("configuring map.");

        if (_mapCenter != null) {
            _mapWidget.setCenter(_mapCenter);
        }
        if (_gMapType != null) {
            _mapWidget.setCurrentMapType(_gMapType);
        }
        Control zoomControl;
        if (_zoomControlType != SMALLZOOMCONTROL) {
            zoomControl = new LargeMapControl();
        }
        else {
            zoomControl = new SmallZoomControl();
        }
        MapTypeControl mapTypeControl = new MapTypeControl();
        if (_zoomControlWidth > 0 && _zoomControlHeight > 0) {
            ControlPosition zoomControlPos =
                    new ControlPosition(
                            ControlAnchor.TOP_LEFT,
                            _zoomControlWidth, _zoomControlHeight);
            _mapWidget.addControl(zoomControl, zoomControlPos);
        }
        else {
            _mapWidget.addControl(zoomControl);
        }
        if (_mapTypeControlWidth > 0 && _mapTypeControlHeight > 0) {
            ControlPosition mapTypeControlPos =
                    new ControlPosition(
                            ControlAnchor.TOP_RIGHT,
                            _mapTypeControlWidth, _mapTypeControlHeight);
            _mapWidget.addControl(mapTypeControl, mapTypeControlPos);
            //TODO: find a better way to specify no map control
        }
        else if (_mapTypeControlWidth < 0 && _mapTypeControlHeight < 0) {
            // no map type control.
        }
        else {
            _mapWidget.addControl(mapTypeControl);
        }

        if (_marker != null)
            _mapWidget.addOverlay(new Marker(_marker));

        // Add the markers to the map, and extend the bounds to include all markers so we can zoom to show them all
        addMarkers(_markers);
    }

    public void addMarkers(Set<Marker> markers) {
        addMarkers(markers, 0);
    }

    public void addMarkers(Set<Marker> markers, int markerColor) {
        // Put Set<Marker> in Collection
        Collection<Set<Marker>> markerCollection = new ArrayList<Set<Marker>>();
        markerCollection.add(markers);
        LatLngBounds bounds = addMarkersBulk(markerCollection, markerColor, false);

        // Update the zoom level to show all markers
        if (markers != null && markers.size() > 1)
            zoomToFitMarkers(_mapWidget, bounds);
        else if (markers != null && markers.size() == 1)
            setZoomLevel(DEFAULT_ZOOM_LEVEL_ONE_MARKER); // far enough out to see a single marker in the middle of the ocean
        else
            setZoomLevel(DEFAULT_ZOOM_LEVEL);
    }

    /**
     * Adds markers as fast as possible.
     */
    public LatLngBounds addMarkersBulk(Collection<Set<Marker>> markerMap, int markerColor, boolean incrementMarkerColors) {
        if (markerMap == null || markerMap.size() < 1)
            return null;

        LatLngBounds bounds = _mapWidget.getBounds();
        for (Set<Marker> markers : markerMap) {
            if (markers != null) {
                for (Marker marker : markers) {
                    if (markerColor > 0 && _markerDirectory != null) {
                        setMarkerColor(marker, markerColor);
                    }
                    _mapWidget.addOverlay(marker);
                    bounds.extend(marker.getLatLng());
                }
            }
            if (incrementMarkerColors)
                markerColor++;
        }

        // Center the map on the area bounding all of the markers
        _mapWidget.setCenter(bounds.getCenter());

        return bounds;
    }

    private void setMarkerColor(Marker marker, int markerColor) {
        if (markerColor > 0 && _markerDirectory != null) {
            marker.getIcon().setImageURL(_markerDirectory + markerColor + ".png");
            marker.getIcon().setIconSize(Size.newInstance(28, 28));
        }
    }

    //TODO: load markers from KML (wasn't faster and requires internet-accessible KML file */
    //_mapWidget.getGmap().addOverlay(new GGeoXml("http://sites.google.com/site/pack210wiki/Home/markers.kml?attredirects=0"));

    //TODO: use MarkerManager (didn't show markers) */
    //private final native void addMarker(MapWidget map, double lat, double lng) /*-{
    //    //var map = new MapWidget(document.getElementById("map_canvas"));
    //    var mgrOptions = { borderPadding: 50, maxZoom: 18, trackMarkers: false };
    //    //var mgr = new MarkerManager(map, mgrOptions);
    //    alert("0");
    //    var mgr = new MarkerManager(map, mgrOptions);
    //alert("1");
    //    mgr.addMarker(new Marker(new LatLng(lat, lng)));
    //alert("2");
    //}-*/;

    public void removeMarkers() {
        _mapWidget.clearOverlays();
    }

    /**
     * When all the marker points have been processed, fit the zoom level to show all of the markers, and set the
     * center as the center of the area containing the markers.
     */
    private void zoomToFitMarkers(MapWidget gmap, LatLngBounds bounds) {
        gmap.setZoomLevel(gmap.getBoundsZoomLevel(bounds));
        gmap.setCenter(bounds.getCenter());
    }

    public void addBusyListener(BusyListener listener) {
        if (listener != null)
            _busyListenerMgr.addBusyListener(listener);
    }

    public void removeBusyListener(BusyListener listener) {
        _busyListenerMgr.removeBusyListener(listener);
    }

    public MapWidget getMapWidget() {
        return _mapWidget;
    }

    public int getZoomLevel() {
        return _zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        _zoomLevel = zoomLevel;
    }

    public MapType getMapType() {
        return _gMapType;
    }

    public void setMapType(MapType gMapType) {
        _gMapType = gMapType;
    }

}
