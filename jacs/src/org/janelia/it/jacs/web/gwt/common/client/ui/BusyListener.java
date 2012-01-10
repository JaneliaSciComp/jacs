
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Widget;

/**
 * @author Michael Press
 */
public interface BusyListener {
    public void onBusy(Widget widget);

    public void onBusyDone(Widget widget);
}
