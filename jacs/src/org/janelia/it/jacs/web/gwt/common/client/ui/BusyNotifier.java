
package org.janelia.it.jacs.web.gwt.common.client.ui;

/**
 * @author Michael Press
 */
public interface BusyNotifier {
    public void addBusyListener(BusyListener listener);

    public void removeBusyListener(BusyListener listener);
}
