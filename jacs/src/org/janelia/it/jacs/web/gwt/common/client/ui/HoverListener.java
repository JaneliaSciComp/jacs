
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Widget;

/**
 * Used for widgets to report that the mouse has entered or exited a hovering mode.
 *
 * @author Michael Press
 */
public interface HoverListener {
    public void onHover(Widget widget);

    public void afterHover(Widget widget);
}
