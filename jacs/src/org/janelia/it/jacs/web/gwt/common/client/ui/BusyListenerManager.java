
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public class BusyListenerManager {
    private ArrayList<BusyListener> _listeners = new ArrayList<BusyListener>();
    private Widget _caller;

    public BusyListenerManager(Widget busyCaller) {
        _caller = busyCaller;
    }

    public void addBusyListener(BusyListener listener) {
        _listeners.add(listener);
    }

    public void removeBusyListener(BusyListener listener) {
        _listeners.remove(listener);
    }

    public void notifyOnBusy() {
        for (BusyListener _listener : _listeners) {
            _listener.onBusy(_caller);
        }
    }

    public void notifyOnBusyComplete() {
        for (BusyListener _listener : _listeners) {
            _listener.onBusyDone(_caller);
        }
    }
}
