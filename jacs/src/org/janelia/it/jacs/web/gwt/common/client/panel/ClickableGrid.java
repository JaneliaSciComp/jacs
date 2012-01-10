
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SourcesClickEvents;

public class ClickableGrid extends Grid implements SourcesClickEvents {
    private ClickListenerCollection listeners = new ClickListenerCollection();

    /**
     * Ensure getting events.
     */
    public ClickableGrid() {
        // Tells 'framework' to send events here, when they happen.
        sinkEvents(Event.ONCLICK);
    }

    public ClickableGrid(int rows, int cols) {
        super(rows, cols);
        sinkEvents(Event.ONCLICK);
    }

    /**
     * Dispatcher for events of interest.  Using this to source mouse events.
     *
     * @param event what to dispatch.
     */
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK) {
            listeners.fireClick(this);
        }
    }

    /**
     * Registration method.  Add listener to hear about clicks.
     *
     * @param listener what will be listening.
     */
    public void addClickListener(ClickListener listener) {
        listeners.add(listener);
    }

    /**
     * De-registration method. Take listener out.
     *
     * @param listener what will no longer be hearing about clicks.
     */
    public void removeClickListener(ClickListener listener) {
        listeners.remove(listener);
    }

}
