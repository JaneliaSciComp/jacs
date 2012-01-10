
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.WidgetVerticalTabBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.VerticalTabPanel;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 12, 2006
 * Time: 10:21:52 AM
 * <p/>
 * This is vertical tab panel that has a widget-friendly vertical tab bar applied to it.
 */
public class WidgetTabBarVerticalTabPanel extends VerticalTabPanel {

    /**
     * Constructor is what sets the specific tab bar.
     */
    public WidgetTabBarVerticalTabPanel() {
        super(new WidgetVerticalTabBar());
    }

    /**
     * Pass through method to the tab bar.  Convenience method.
     *
     * @param w what to add as an adornment.
     */
    public void addTabBarAdornment(Widget w) {
        WidgetVerticalTabBar tabBar = getWidgetTabBar();
        tabBar.addAdornment(w);
    }

    /**
     * Convenience method to expose the specific tab bar used here.
     *
     * @return the tab bar.
     */
    public WidgetVerticalTabBar getWidgetTabBar() {
        return (WidgetVerticalTabBar) super.getTabBar();
    }

    /**
     * Adds a new tab with the specified widget.
     */
    public void add(ClickableGrid widget, Widget cardWidget) {
        getWidgetTabBar().addTab(widget);
        getDeckPanel().insert(cardWidget, getDeckPanel().getWidgetCount());
    }

}
