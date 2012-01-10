
package org.janelia.it.jacs.web.gwt.common.client.popup;

/**
 * Popup if not logged in.
 */
public class NotLoggedInPopupPanel extends ErrorPopupPanel {

    private static final String DEFAULT_ALERT_MESSAGE = "You must be logged in to view this data.";

    public NotLoggedInPopupPanel() {
        this(DEFAULT_ALERT_MESSAGE);
    }

    public NotLoggedInPopupPanel(String alertMessage) {
        super((alertMessage == null) ? DEFAULT_ALERT_MESSAGE : alertMessage);
    }
}