
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.Hyperlink;

/**
 * This is an extension of the GWT Hyperlink that doesn't cause history events to be stored/fired if the
 * targetHistoryToken is null, which it is by default, so use of hyperlinks doesn't add to the browser's
 * history stack unless targetHistoryToken is explicitly set.
 *
 * @author Michael Press
 */
public class HistorySafeHyperlink extends Hyperlink {
    /**
     * Replaces the Hyperlink implementation with an identical one except that the targetHistoryToken
     * is not added to the history stack if null.
     */
    @Override
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK) {
            if (clickListeners != null) {
                clickListeners.fireClick(this);
            }
            if (getTargetHistoryToken() != null) {  // new restriction
                History.newItem(targetHistoryToken);
            }
            DOM.eventPreventDefault(event);
        }
    }

    /**
     * Sets the history token referenced by this hyperlink. This is the history
     * token that will be passed to {@link History#newItem} when this link is
     * clicked.  Ignored if null.
     *
     * @param targetHistoryToken the new target history token
     */
    @Override
    public void setTargetHistoryToken(String targetHistoryToken) {
        this.targetHistoryToken = targetHistoryToken;
        if (targetHistoryToken != null) // new restriction
            DOM.setElementProperty(anchorElem, "href", "#" + targetHistoryToken);
    }
}
