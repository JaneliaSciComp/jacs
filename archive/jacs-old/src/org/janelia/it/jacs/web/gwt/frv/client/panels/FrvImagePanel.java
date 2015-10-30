
package org.janelia.it.jacs.web.gwt.frv.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.*;
import com.google.gwt.maps.client.control.ControlAnchor;
import com.google.gwt.maps.client.control.ControlPosition;
import com.google.gwt.maps.client.control.SmallMapControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapMoveEndHandler;
import com.google.gwt.maps.client.event.MapMoveHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupOffscreenLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.frv.client.Frv;
import org.janelia.it.jacs.web.gwt.frv.client.FrvBounds;
import org.janelia.it.jacs.web.gwt.frv.client.FrvBoundsChangedListener;
import org.janelia.it.jacs.web.gwt.frv.client.FrvMapUtils;
import org.janelia.it.jacs.web.gwt.frv.client.map.GSelectRegionControl;
import org.janelia.it.jacs.web.gwt.frv.client.popups.AnnotationInfoPopup;
import org.janelia.it.jacs.web.gwt.frv.client.popups.FrvAnnotationTablePopup;
import org.janelia.it.jacs.web.gwt.frv.client.popups.FrvRegionExportPopup;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMapUtils;
import org.janelia.it.jacs.web.gwt.map.client.MapMoveRequestListener;
import org.janelia.it.jacs.web.gwt.map.client.googleMaps.MapControllable;
import org.janelia.it.jacs.web.gwt.map.client.googleMaps.NonRepeatingGEuclideanProjection;
import org.janelia.it.jacs.web.gwt.map.client.googleMaps.SafeMapWidget;

import java.util.Date;

/**
 * @author Michael Press
 */
public class FrvImagePanel extends TitledBox implements IsJobSettable {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel");

    private RecruitableJobInfo _job = null;
    private LoadingLabel _loadingLabel;
    private VerticalPanel _contentPanel;
    private FrvLegendPanel _legendPanel;
    //    private FrvHistogramPanel _histogramPanel;
    private HTML _noDataMsg;

    private SafeMapWidget _map = null;
    private SafeMapWidget _xaxis = null;
    private SafeMapWidget _yaxis = null;
    private HTML _buffer;
    //private MapEventHandler _mapListener;  // supplied listener for other panels
    private MapMoveRequestListener _mapMoveRequestListener; // called by other panels to move the map
    private static String _tileURL = "localhost:8080/jacs/tiledImage.srv"; // default; value retrieved from properties

    // Export Region controls
    private GSelectRegionControl _selectRegionControl;
    private FrvRegionExportPopup _exportRegionPopup;
    private PopupCenteredLauncher _exportRegionPopupLauncher;

    private FrvAnnotationTablePopup _annotationTablePopup;
    private PopupAtRelativePixelLauncher _annotationTablePopupLauncher;
    private AnnotationInfoPopup annotationPopup;
    private boolean isExportSelected;

    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 4; // 5th zoom level

    private static final int START_ZOOM = 1;
    private static final int START_MAP_HEIGHT = 512; // 2 tiles at zoom level 1
    private static final int START_MAP_WIDTH = 512; // 2 tiles at zoom level 1

    private static final int TILE_WIDTH = 256;
    private static final int FULL_MAP_HEIGHT = 512;
    private static final int FULL_MAP_WIDTH = 800;
    private static final int X_AXIS_HEIGHT = 70;
    private static final int Y_AXIS_WIDTH = 35;
    private static final int X_AXIS = 0;
    private static final int Y_AXIS = 1;
    public static final int BUFFER_WIDTH = FULL_MAP_WIDTH - START_MAP_WIDTH;
    private static final int IE6_REFRESH_TIME_DELAY = 1000;

    private static final String COPYRIGHT_HTML = "&copy;&nbsp;2007&nbsp;JaCS";
    private static final String CUSTOM_MAP_NAME = "FRV";

    public FrvImagePanel(String title) {
        super(title, true); // title to be set when job is knowna
        addStyleName("frvImagePanel");
        getBaseTileURL();
    }

    private void getBaseTileURL() {
        String url = SystemProps.getString("RecruitmentViewer.TileURL", null);
        if (url != null) {
            _tileURL = url;
            _logger.info("Retrieved RecruitmentViewer.TileURL property=" + _tileURL);
        }
        else {
            _logger.error("Failed to retrieve RecruitmentViewer.TileURL property");
        }
    }

    protected void popuplateContentPanel() {
        _noDataMsg = HtmlUtils.getHtml("No data selected.", "text"); /* in case default microbe not found */
        _contentPanel = new VerticalPanel();
        _loadingLabel = new LoadingLabel("Loading viewer...", /*visible*/ false);
        _mapMoveRequestListener = new FrvImagePanel.MapMoveRequested();

        add(_noDataMsg);
        add(_loadingLabel);
        add(_contentPanel);
    }

    protected void createActionLinks() {
        if (null == _legendPanel) {
            // Create legend panel now because it's needed by the action link, which is created early on by the superclass
            _legendPanel = new FrvLegendPanel();
            _legendPanel.setWidth("800px");
        }

//        _histogramPanel = new FrvHistogramPanel();
//        _histogramPanel.setWidth("800px");

        // Add action links
        addActionLink(getLegendActionLink());
        addActionLink(getExportActionLink());
        if (null != _job && null != _job.getGiNumberOfSourceData() && !"".equals(_job.getGiNumberOfSourceData())) {
            addActionLink(getAnnotationActionLink());
        }
//        addActionLink(getHistogramActionLink());
    }

    private HasActionLink getLegendActionLink() {
        Image showImage = ImageBundleFactory.getControlImageBundle().getLegendShowImage().createImage();
        Image hideImage = ImageBundleFactory.getControlImageBundle().getLegendHideImage().createImage();
        final HideShowActionLink link = new HideShowActionLink(_legendPanel.getPanel(), null,
                HideShowActionLink.SECONDARY_STATE, "hide legend", "show legend", hideImage, showImage);

        if (BrowserDetector.isIE6()) {
            link.addPrimaryStateChangeListener(new StateChangeListener() {
                public void onStateChange() {
                    new ie6mapRefreshTimer(link).schedule(IE6_REFRESH_TIME_DELAY);
                }
            });
            link.addSecondaryStateChangeListener(new StateChangeListener() {
                public void onStateChange() {
                    new ie6mapRefreshTimer(link).schedule(IE6_REFRESH_TIME_DELAY);
                }
            });
        }
        return link;
    }

    private HasActionLink getExportActionLink() {
        Image image = ImageBundleFactory.getControlImageBundle().getExportImage().createImage();
        return new ActionLink("export region", image, new GSelectClickListener(true, this));
    }

    private HasActionLink getAnnotationActionLink() {
        Image image = ImageBundleFactory.getControlImageBundle().getAnnotationsImage().createImage();
        return new ActionLink("select annotations", image, new GSelectClickListener(false, this));
    }

//    private HasActionLink getHistogramActionLink()
//    {
//        Image showImage = ImageBundleFactory.getControlImageBundle().getLegendShowImage().createImage();
//        Image hideImage = ImageBundleFactory.getControlImageBundle().getLegendHideImage().createImage();
//        final HideShowActionLink link = new HideShowActionLink(_histogramPanel.getPanel(), null,
//            HideShowActionLink.SECONDARY_STATE, "hide histogram", "show histogram", hideImage, showImage);
//
//        if (BrowserDetector.isIE6()) {
//            link.addPrimaryStateChangeListener(new StateChangeListener() {
//                public void onStateChange()
//                {
//                    new ie6mapRefreshTimer(link).schedule(IE6_REFRESH_TIME_DELAY);
//                }
//            });
//            link.addSecondaryStateChangeListener(new StateChangeListener() {
//                public void onStateChange()
//                {
//                    new ie6mapRefreshTimer(link).schedule(IE6_REFRESH_TIME_DELAY);
//                }
//            });
//        }
//        return link;
//    }

    public void setJob(RecruitableJobInfo job) {
        _logger.debug("setJob()");
        _job = job;
        if (_job != null) {
            _noDataMsg.setVisible(false);
            Frv.trackActivity("FRV.View", job);
            // Reset action links to catch any changes with annotations
            removeActionLinks();
            createActionLinks();
            swapImages();
        }
    }

    private void createImageForJob() {
        if (_job != null) {
            createLegend();
            createMap();
            createXAxis();
            createYAxis();

            HorizontalPanel row = new HorizontalPanel();
            VerticalPanel yAxisCol = new VerticalPanel();
            VerticalPanel mapCol = new VerticalPanel();

            final SimplePanel yAxisPanel = new SimplePanel();
            yAxisPanel.setStyleName("frvChartYAxis");
            yAxisPanel.add(_yaxis);

            SimplePanel bottomLeftCornerFiller = new SimplePanel();
            bottomLeftCornerFiller.setStyleName("frvChartBottomLeftCornerFiller");

            SimplePanel mapPanel = new SimplePanel();
            mapPanel.setStyleName("frvChart");
            mapPanel.add(_map);

            SimplePanel xAxisPanel = new SimplePanel();
            xAxisPanel.setStyleName("frvChartXAxis");
            xAxisPanel.add(_xaxis);

            HTML xAxisLabel = HtmlUtils.getHtml("Base pair position", "infoPrompt");
            SimplePanel xAxisLabelPanel = new SimplePanel();
            xAxisLabelPanel.setStyleName("frvXAxisLabelPanel");
            xAxisLabelPanel.add(xAxisLabel);

            HTML yAxisLabel = HtmlUtils.getHtml("P<br>e<br>r<br>c<br>e<br>n<br>t<br>&nbsp;<br>I<br>d<br>e<br>n<br>t<br>i<br>t<br>y", "infoPrompt");
            SimplePanel yAxisLabelPanel = new SimplePanel();
            yAxisLabelPanel.setStyleName("frvYAxisLabelPanel");
            yAxisLabelPanel.add(yAxisLabel);

            yAxisCol.add(yAxisPanel);
            yAxisCol.add(bottomLeftCornerFiller);

            HTML hints = HtmlUtils.getHtml(
                    "&nbsp;&nbsp;&bull;&nbsp;Use the +/- buttons to zoom." +
                            "&nbsp;&nbsp;&bull;&nbsp;Drag the image to pan (the image will snap back to an edge if dragged too far)." +
                            "&nbsp;&nbsp;&bull;&nbsp;Double-click to change region and zoom." +
                            "<br>" +
                            "&nbsp;&nbsp;&bull;&nbsp;The currently visible area is highlighted in the Overview panel." +
                            "", "hint");
            hints.setWidth("100%");

            mapCol.add(mapPanel);
            mapCol.add(xAxisPanel);
            mapCol.add(xAxisLabelPanel);

            row.add(yAxisLabelPanel);
            row.add(yAxisCol);
            row.add(mapCol);

            _buffer = new HTML("&nbsp;");
            _contentPanel.add(_legendPanel);
            if (BrowserDetector.isIE6()) // Have to provide refresh button for IE6 DOM bugginess
                _contentPanel.add(getRefreshHackPanel());
            _contentPanel.add(row);
            _contentPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
            _contentPanel.add(hints);

            // Hide the duplicate google logos on the axes
            hideGoogleLogo(xAxisPanel);
            hideGoogleLogo(yAxisPanel);

            // Create the export popup now so it's fast later
            _exportRegionPopup = new FrvRegionExportPopup(_job, new FrvBoundsChanged(this), _map.getGmap());
            _exportRegionPopup.addPopupListener(new PopupClosedListener(this, _selectRegionControl));
            //TODO: show the popup at the top of the exported area
            _exportRegionPopupLauncher = new PopupCenteredLauncher(_exportRegionPopup);

            // Create the Annotation Table popup now so it's fast later
            _annotationTablePopup = new FrvAnnotationTablePopup(_job, new FrvBoundsChanged(this), _map.getGmap());
            _annotationTablePopup.addPopupListener(new PopupClosedListener(this, _selectRegionControl));
            _annotationTablePopupLauncher = new PopupAtRelativePixelLauncher(_annotationTablePopup, -200, 100);
        }
    }

    /**
     * IE6 blanks out the Google Maps from time to time so we need to give the user a Refresh button that will make
     * the map show up.
     *
     * @return returns the panel
     */
    private HorizontalPanel getRefreshHackPanel() {
        // Panel with Refresh link
        HorizontalPanel refreshHackButtonPanel = new HorizontalPanel();
        refreshHackButtonPanel.setStyleName("frvRefreshHackButtonPanel");
        refreshHackButtonPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        SmallLink refresh = new SmallLink("Refresh", new RefreshClickListener());
        refreshHackButtonPanel.add(refresh);

        return refreshHackButtonPanel;
    }

    public class RefreshClickListener implements ClickListener {
        public void onClick(Widget sender) {
            ie6mapRefresh(sender);
        }
    }

    public class ie6mapRefreshTimer extends Timer {
        Widget _sender;

        public ie6mapRefreshTimer(Widget sender) {
            _sender = sender;
        }

        public void run() {
            ie6mapRefresh(_sender);
        }
    }

    /**
     * IE6 blanks out the Google Maps from time to time so we need to give the user a Refresh button that will make
     * the map show up.
     *
     * @param sender widget sending the refresh request
     */
    private void ie6mapRefresh(Widget sender) {
        BasePopupPanel popup = new BasePopupPanel() {
            protected void populateContent() {
                add(new HTML("&nbsp;"));
            }

        };
        new PopupOffscreenLauncher(popup).showPopup(sender);
        popup.hide();
    }


    public class GSelectClickListener implements ClickListener {
        private FrvImagePanel _frv;
        private boolean isExportListener;

        public GSelectClickListener(boolean isExportListener, FrvImagePanel frv) {
            this._frv = frv;
            this.isExportListener = isExportListener;
        }


        public void onClick(Widget sender) {
            isExportSelected = isExportListener;
            _selectRegionControl.promptInit(_frv);
        }
    }

    /**
     * When the user edits the region to export in the popup, we need up update the dashed box showing the
     * selected area
     */
    public class FrvBoundsChanged implements FrvBoundsChangedListener {
        private FrvImagePanel _frv;

        public FrvBoundsChanged(FrvImagePanel frv) {
            _frv = frv;
        }

        public void onBoundsChange(FrvBounds bounds) {
            _selectRegionControl.redraw(_frv, FrvMapUtils.convertFrvBoundsToLatLng(bounds, _job));
        }
    }

    /**
     * Hides the redundant "powered by Google" image and the "Terms of Use" link on the x and y axes. They are
     * still shown on the main map.
     *
     * @param axisPanel the axis panel widget making the hide logo call
     */
    private void hideGoogleLogo(Widget axisPanel) {
        // Retrieve the axis Element. It has 1 child, which has 3 children.  The "powered by Google" logo and the
        // copyright/Terms of Use info are children 2 and 3, so turn off their display attributes.
        Element element = axisPanel.getElement();
        if (DOM.getChildCount(element) > 0) {
            Element child = DOM.getChild(element, 0);
            if (child != null) {
                int numGrandChildren = DOM.getChildCount(child);
                for (int i = 0; i < numGrandChildren; i++)
                    if (i == 1 || i == 2)
                        DOM.setStyleAttribute(DOM.getChild(child, i), "display", "none");
            }
        }
    }

    /**
     * loads the legend in the background
     */
    private void createLegend() {
        new LegendTimer().schedule(1000);
    }

    public class LegendTimer extends Timer {
        public void run() {
            _legendPanel.setJob(_job);
        }
    }

    public void onLog(String msg) {
        _logger.debug(msg);
    }

    /**
     * When a region is selected, determing the export bounds (base pair and pctId ranges) and show a popup
     *
     * @param nwX northwest x coordinate
     * @param nwY northwest y coordinate
     * @param seX southeast x coordinate
     * @param seY southeast y coordinate
     */
    public void onRegionSelected(float nwX, float nwY, float seX, float seY) {
        if (isExportSelected) {
            _exportRegionPopup.setBounds(FrvMapUtils.convertLatLngToFrvBounds(_job, nwX, nwY, seX, seY));
            _exportRegionPopupLauncher.showPopup(this);
        }
        else {
            _annotationTablePopup.clearDataAndDisplay();
            _annotationTablePopup.setBounds(FrvMapUtils.convertLatLngToFrvBounds(_job, nwX, nwY, seX, seY));
            _annotationTablePopupLauncher.showPopup(this);
            _annotationTablePopup.getData();
        }
    }

//    /** When a region is selected, determing the export bounds (base pair and pctId ranges) and show a popup */
//    public void onAnnotationsSelected(float nwX, float nwY, float seX, float seY)
//    {
//        _annotationTablePopup.clearDataAndDisplay();
//        _annotationTablePopup.setBounds(FrvMapUtils.convertLatLngToFrvBounds(_job, nwX, nwY, seX, seY));
//        _annotationTablePopupLauncher.showPopup(this);
//        _annotationTablePopup.getData();
//    }

    /**
     * When the Export Region popup closes, reset the region-select controls and rubber-band artifacts
     */
    public class PopupClosedListener implements PopupListener {
        private FrvImagePanel _frv;
        private MapControllable mapControl;

        public PopupClosedListener(FrvImagePanel frv, MapControllable mapControl) {
            _frv = frv;
            this.mapControl = mapControl;
        }

        public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
            mapControl.reset(_frv);
            if (BrowserDetector.isIE6())
                new ie6mapRefreshTimer(_contentPanel).schedule(IE6_REFRESH_TIME_DELAY);
        }
    }

    private void createMap() {
        // Create the map, sized for zoom level 1
        _map = new SafeMapWidget(String.valueOf(START_MAP_HEIGHT), String.valueOf(START_MAP_WIDTH), LatLng.newInstance(0, 0), START_ZOOM, /* options */null);
        MapWidget gmap = _map.getGmap();

        gmap.setZoomLevel(START_ZOOM);

        // Add zoom and region-select controls
        gmap.addControl(new SmallMapControl());
        //gmap.addControl(GControl.GLargeMapControl());
        //gmap.addControl(GControl.narrowToGControl(GSelectRegionControl(this)),
        //    new GControlPosition(GControlAnchor.G_ANCHOR_TOP_LEFT(), new GSize(18, 170))); // this locates it under the zoom controls
        _selectRegionControl = new GSelectRegionControl(this);
        gmap.addControl(_selectRegionControl,
                new ControlPosition(ControlAnchor.TOP_LEFT, 7, 107)); // this locates it under the zoom controls

        // Have to do this after adding all controls
        gmap.setCenter(LatLng.newInstance(0, 0), START_ZOOM);

        // Create the custom map layer
        TileLayer[] tilelayers = createTileLayers();
        MapType customMap = new MapType(tilelayers, new NonRepeatingGEuclideanProjection(MAX_ZOOM + 1), CUSTOM_MAP_NAME, new MapTypeOptions());
        // todo Configure the tile layer
//        configureTileLayer(tilelayers[0].jsoPeer, this);

        // Add the custom map and set as default
        gmap.addMapType(customMap);
        gmap.setCurrentMapType(customMap);

        // Sync x and y axes to drag and zoom events on the main window
        // todo Fix this sync
        gmap.addMapMoveHandler(new MapMoveHandler() {
            public void onMove(MapMoveEvent moveEvent) {
                Window.alert("Map move event");
//                MapWidget mainMap = MapWidget.narrowToGMap2(jsObject);
//                if (_xaxis != null)
//                    _xaxis.getGmap().setCenter(LatLng.newInstance(0, mainMap.getCenter().getLongitude()));
//                if (_yaxis != null)
//                    _yaxis.getGmap().setCenter(LatLng.newInstance(mainMap.getCenter().getLatitude(), 0));
            }
        });
        gmap.addMapZoomEndHandler(new MapZoomEndHandler() {
            public void onZoomEnd(MapZoomEndEvent zoomEndEvent) {
                Window.alert("Map zoom end");
//                MapWidget mainMap = GMap2.narrowToGMap2(jsObject);
//                int zoom = mainMap.getZoomLevel();
//                if (_xaxis != null)
//                    _xaxis.getGmap().setZoomLevel(zoom);
//                if (_yaxis != null)
//                    _yaxis.getGmap().setZoomLevel(zoom);

//                Frv.trackActivity("FRV.ZoomLevel." + zoom, _job);
                if (BrowserDetector.isIE6())
                    new ie6mapRefreshTimer(_contentPanel).schedule(IE6_REFRESH_TIME_DELAY);
            }
        });

        // Make sure user doesn't drag or pan the image such that blank space is exposed.  When this happens,
        // slide the image over to the exposed edge
        gmap.addMapMoveEndHandler(new MapMoveEndHandler() {
            public void onMoveEnd(MapMoveEndEvent moveEndEvent) {
                MapWidget map = _map.getGmap();

                // Zoom level 1 shows the entire map, so just recenter after any drag
                if (map.getZoomLevel() < 2) {
                    if (map.getCenter().getLongitude() != 0.0 || map.getCenter().getLatitude() != 0)
                        _map.getGmap().setCenter(LatLng.newInstance(0, 0));
                }
                // For deeper zoom levels, calculate the amount exposed and slide the map back that far
                else {
                    GoogleMapUtils.restrictTopAndBottomBounds(map);
                    GoogleMapUtils.restrictLeftAndRightBounds(map);
                }
            }
        });

        // Adapt the map size to the current zoom level - shrink the width for zoom 1, 400x800 for deeper zooms
        gmap.addMapZoomEndHandler(new MapZoomEndHandler() {
            public void onZoomEnd(MapZoomEndEvent zoomEndEvent) {
                int zoomlevel = _map.getGmap().getZoomLevel();
                int height = FULL_MAP_HEIGHT;
                int width = FULL_MAP_WIDTH;
                int bufferWidth = 0;

                if (zoomlevel < 2) { // shrink to the image size
                    height = width = TILE_WIDTH * (zoomlevel + 1);
                    bufferWidth = BUFFER_WIDTH;
                }

                _map.setHeight(String.valueOf(height));
                _map.setWidth(String.valueOf(width));
                _xaxis.setWidth(String.valueOf(width));
                _yaxis.setHeight(String.valueOf(height));
                _buffer.setWidth(String.valueOf(bufferWidth));

                if (zoomlevel < 2) // recenter the little zoom levels
                    _map.getGmap().setCenter(LatLng.newInstance(0, 0));
            }
        });

        // Notify external listener if supplied (only on completion of move and zoom)
//        if (_mapListener != null) {
//            GEvent.addListener(gmap, "moveend", _mapListener);
//            GEvent.addListener(gmap, "zoomend", _mapListener);
//        }
    }

    private TileLayer[] createTileLayers() {
        CopyrightCollection copyrightCollection = getCopyright();
        TileLayer[] tilelayers = new TileLayer[1];
        // todo create the tile layer
//        tilelayers[0] = new TileLayer(copyrightCollection, MIN_ZOOM, MAX_ZOOM);

        return tilelayers;
    }

    private CopyrightCollection getCopyright() {
        Copyright copyright = new Copyright(/*id*/1, LatLngBounds.newInstance(LatLng.newInstance(-90, -180), LatLng.newInstance(90, 180)), MIN_ZOOM, "&nbsp;");
        CopyrightCollection copyrightCollection = new CopyrightCollection(COPYRIGHT_HTML);
        copyrightCollection.addCopyright(copyright);

        return copyrightCollection;
    }

    private void createXAxis() {
        _xaxis = createAxis(String.valueOf(X_AXIS_HEIGHT), String.valueOf(START_MAP_WIDTH), X_AXIS);
        MapWidget axisMap = _xaxis.getGmap();
        axisMap.addMapClickHandler(new MapClickHandler() {
            public void onClick(MapClickEvent clickEvent) {
                LatLng loc = clickEvent.getLatLng();// LatLng.narrowToGLatLng(param[1]);
                //Window.alert("Got the call:"+loc.lat()+", "+loc.lng());
                long ntLocation = (long) (((loc.getLongitude() + 180) / 360) * _job.getRefAxisEndCoord());//_job.getRefAxisEndCoord()
                if (null != _job.getGiNumberOfSourceData() && !"".equals(_job.getGiNumberOfSourceData())) {
                    annotationPopup = new AnnotationInfoPopup(_job.getGiNumberOfSourceData(), _job.getRecruitableNodeId(),
                            ntLocation, _job.getAnnotationFilterString());
                    annotationPopup.center();
                    new PopupAtRelativePixelLauncher(annotationPopup, 100, 100).showPopup(_contentPanel);
                }
            }
        });
    }

    private void createYAxis() {
        _yaxis = createAxis(String.valueOf(START_MAP_HEIGHT), String.valueOf(Y_AXIS_WIDTH), Y_AXIS);
    }

    /**
     * @param height    height of the map widget
     * @param width     width of the map widget
     * @param whichAxis one of XAXIS or YAXIS
     * @return returns the SafeGMap2Widget
     */
    private SafeMapWidget createAxis(String height, String width, int whichAxis) {
        SafeMapWidget axis = new SafeMapWidget(height, width, LatLng.newInstance(0, 0), START_ZOOM, /* options */null);
        MapWidget gmap = axis.getGmap();

        // Disable direct user controls on the axis
        gmap.setDraggable(false);
        gmap.setDoubleClickZoom(false);
        gmap.setContinuousZoom(false);
        gmap.setInfoWindowEnabled(false);

        // Create the custom map layer
        TileLayer[] tilelayers = createTileLayers();
        MapType customMap = new MapType(tilelayers, new NonRepeatingGEuclideanProjection(MAX_ZOOM + 1), CUSTOM_MAP_NAME, new MapTypeOptions());
        // nasty hack
        // todo Reenable the axis layers
//        if (whichAxis == X_AXIS)
//            configureXAxisLayer(tilelayers[0], this);
//        else
//            configureYAxisLayer(tilelayers[0], this);

        // Add the custom map and set as default
        gmap.addMapType(customMap);
        gmap.setCurrentMapType(customMap);

        return axis;
    }

    /**
     * This native JavaScript method is used to set the function on the Google Map that will be invoked to retrieve
     * image tiles.  It just calls the Java method getTileUrl(), but is necessary because the Google Maps API can
     * only take a JavaScript function to call for the image tile URL.
     *
     * @param layer javascript tile layer
     * @param frv   image panel object
     */
    private static native void configureTileLayer(JavaScriptObject layer, FrvImagePanel frv) /*-{
            layer.getTileUrl = function(t,z)
            {
                // This funky syntax is the GWT JSNI syntax to call the getTileURL() method.  For syntax description,
                // see http://code.google.com/webtoolkit/documentation/com.google.gwt.doc.DeveloperGuide.JavaScriptNativeInterface.JavaFromJavaScript.html
                return frv.@org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel::getTileUrl(III) (t.x, t.y, z);
            };

            layer.isPng = function() { return true; };
//            layer.getOpacity = function() { return mtl.@com.mycompany.mypackage.MyTileLayer::getOpacity()(); };
    }-*/;

    public String getTileUrl(int x, int y, int zoom) {
        return getTileUrl(x, y, zoom, _job);
    }

    public static String getTileUrl(int x, int y, int zoom, RecruitableJobInfo job) {
        return getTileUrl(x, y, zoom, job.getRecruitmentResultsFileNodeId(), job.getUsername());
    }

    public static String getTileUrl(int x, int y, int zoom, String nodeId, String nodeOwner) {
        return new StringBuffer(_tileURL)
                .append("?x=").append(x)
                .append("&y=").append(y)
                .append("&z=").append(zoom)
                .append("&nodeId=").append(nodeId)
                .append("&nodeOwner=").append(nodeOwner)
                .append("&nocache=").append(new Date().getTime())
                .toString();
    }

    private static native void configureXAxisLayer(JavaScriptObject layer, FrvImagePanel frv) /*-{
            layer.getTileUrl = function(t,z)
            {
                return frv.@org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel::getXAxisTileUrl(III) (t.x, t.y, z);
            };
            layer.isPng = function() { return true; };
    }-*/;

    public String getXAxisTileUrl(int x, int y, int zoom) {
        return new StringBuffer(_tileURL)
                .append("?x=").append(x)
                .append("&y=").append(y)
                .append("&z=").append(zoom)
                .append("&nodeId=").append((_job == null) ? "" : _job.getRecruitmentResultsFileNodeId())
                .append("&nodeOwner=").append((_job == null) ? "" : _job.getUsername())
                .append("&xAxis=true")
                .append("&nocache=").append(new Date().getTime())
                .toString();
    }

    private static native void configureYAxisLayer(JavaScriptObject layer, FrvImagePanel frv) /*-{
            layer.getTileUrl = function(t,z)
            {
                return frv.@org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel::getYAxisTileUrl(III) (t.x, t.y, z);
            };
            layer.isPng = function() { return true; };
    }-*/;

    public String getYAxisTileUrl(int x, int y, int zoom) {
        return new StringBuffer(_tileURL)
                .append("?x=").append(x)
                .append("&y=").append(y)
                .append("&z=").append(zoom)
                .append("&nodeId=").append((_job == null) ? "" : _job.getRecruitmentResultsFileNodeId())
                .append("&nodeOwner=").append((_job == null) ? "" : _job.getUsername())
                .append("&yAxis=true")
                .append("&nocache=").append(new Date().getTime())
                .toString();
    }

    //private void swapImages(final boolean firstLoad)
    private void swapImages() {
        if (!GWT.isScript())
            swapImagesImmediate(); // GWT hosted mode
        else
            swapImagesFade(); // normal browser
    }

    private void swapImagesImmediate() {
        final Callback turnOffLoadingLabelCallback = new Callback() {
            public void execute() {
                _loadingLabel.setVisible(false);
            }
        };
        _contentPanel.clear();
        _loadingLabel.setVisible(true);
        new ShowImageTimer(turnOffLoadingLabelCallback).schedule(100);
    }

    //TODO: make and use a generic callback chain
    private void swapImagesFade() {
        // Step 3 - new image has been loaded, so fade in content panel
        final Callback fadeInContentCallback = new Callback() {
            public void execute() {
                _loadingLabel.setVisible(false);
                SafeEffect.fade(_contentPanel, new EffectOption[]{
                        new EffectOption("to", "1"),
                });
            }
        };

        // Step 2, called after old image fadeout is done: set loading label visible, then create the new image in the
        // (faded-out) content panel in a timer (so loading label has time to show).
        Callback loadImageCallback = new Callback() {
            public void execute() {
                _contentPanel.clear(); // it's now invisible
                _loadingLabel.setVisible(true);
                new ShowImageTimer(fadeInContentCallback).schedule(100);
            }
        };

        // Step 1: fadeout the images
        SafeEffect.fade(_contentPanel, new EffectOption[]{
                new EffectOption("to", "0.01"), // Fadeout old chart (must be non-0 or it gets removed)
                new EffectOption("afterFinish", loadImageCallback)
        });
    }

    /**
     * Creates the new chart.  Calls the provided callback when complete
     */
    public class ShowImageTimer extends Timer {
        private Callback _callback = null;

        public ShowImageTimer(Callback callback) {
            _callback = callback;
        }

        public void run() {
            createImageForJob();
            if (_callback != null)
                _callback.execute();
        }
    }

    // todo Hook back up the map listener
//    public GEventHandler getMapListener()
//    {
//        return _mapListener;
//    }
//
//    public void setMapListener(GEventHandler mapListener)
//    {
//        _mapListener = mapListener;
//    }

    public MapMoveRequestListener getMapMoveRequestListener() {
        return _mapMoveRequestListener;
    }

    public class MapMoveRequested implements MapMoveRequestListener {
        public void moveTo(LatLng center) {
            _map.getGmap().setCenter(center);
        }
    }

}
