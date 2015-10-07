
package org.janelia.it.jacs.web.gwt.map.client.panel;

import com.google.gwt.user.client.ui.SimplePanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.map.client.GoogleMap;

public class MapBox extends TitledBox {
    private LoadingLabel _mapLoadingLabel;
    private GoogleMap _map;
    private SimplePanel _mapPanel;
    private static final String LOADING_MSG = "Loading map...";

    public MapBox(String title, GoogleMap map) {
        super(title, true);
        _map = map;
        if (_map != null)
            setMap(_map);
    }

    /**
     * Called by superclass before _map is set
     */
    protected void init() {
        super.init();
        setStyleName("mapBox");

        // Create a "Loading" label until the map loads
        _mapPanel = new SimplePanel();
        _mapPanel.setStyleName("mapPanelNoMap");

        addLoadingLabel();
        add(_mapPanel);
    }

    public void checkResizeMap() {
        if (_map != null) {
            _map.getMapWidget().checkResize();
        }
    }

    /**
     * Sets the map (makes it show up) on the TitledBox.
     */
    public void setMap(GoogleMap map) {
        reset();
        hideMessage();
        _map = map;
        _mapPanel.add(_map);
        _mapPanel.setStyleName("mapPanelWithMap");
    }

    public void reset() {
        if (_map != null)
            _mapPanel.remove(_map);
        _mapPanel.setStyleName("mapPanelNoMap");
        resetMessage();
        showMessage();
    }

    public void setMessage(String msg, String styleName) {
        _mapLoadingLabel.setText(msg);
        _mapLoadingLabel.setStyleName(styleName);
    }

    public void resetMessage() {
        _mapLoadingLabel.setText(LOADING_MSG);
        _mapLoadingLabel.setStyleName("loadingMsgText");
    }

    public void showMessage() {
        _mapLoadingLabel.setVisible(true);
    }

    public void hideMessage() {
        _mapLoadingLabel.setVisible(false);
    }

    public void addLoadingLabel() {
        // Create a "Loading" label until the map loads
        _mapLoadingLabel = new LoadingLabel(LOADING_MSG, true);
        add(_mapLoadingLabel);
    }
}
