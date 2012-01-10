
package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.core.PlatformDetector;

/**
 * A simple alternative to GWT menus, which will not auto-close (leaving menus up after a casual mouse-by).  These
 * menus are vertical only, auto-open and auto-close.  A menu bar is the container for menus, and can contain
 * both SimpleMenus (which drop-down on mouseover) and SimpleMenuItems (which act like buttons).
 *
 * @author Michael Press
 */
public class SimpleMenuBar extends Composite {
    private static final String STYLE_NAME = "HeaderMenuBar";

    private HorizontalPanel _panel = new HorizontalPanel();

    /**
     * This variable returns the proper style name depending on the browser and/or platform requirements. The proper
     * subclass implementation is compiled into each browser-specific output of the GWT compiler, meaning the
     * proper platform and browser code is retrieved at runtime without knowing any code for other browsers.
     */
    //private static final SimpleMenuBarStyleImpl simpleMenuBarStyleImpl = GWT.create(SimpleMenuBarStyleImpl.class);
    public SimpleMenuBar() {
//        _panel.setStyleName(STYLE_NAME);
//        _panel.addStyleName(STYLE_NAME +  getStyleSuffix());
        //_panel.addStyleName(STYLE_NAME + simpleMenuBarStyleImpl.getPlatformAndBrowserSpecificStyleSuffix());

        initWidget(_panel);
    }

    /**
     * Seems like every combination of browser and platform has a different placement of the menu bar.  Have to
     * use an additional CSS style to move it to be aligned with the bottom of the background image.
     */
    private String getStyleSuffix() {
        String styleSuffix;
        if (BrowserDetector.isIE())
            styleSuffix = "IE";
        else {
            styleSuffix = BrowserDetector.isFF2() ? "FF2" : "FF3";
            if (PlatformDetector.isMac())
                styleSuffix += "Mac";
            else if (PlatformDetector.isLinux())
                styleSuffix += "Linux";
        }
        return styleSuffix;
    }

    /**
     * Add a menu item to a menu bar.  The menu item works the same in the menu bar as it does in a menu, but the
     * styling is overriden to look like the top-level label for a menu in the menu bar.
     */
    public void addItem(SimpleMenuItem item) {
        _panel.add(new SimpleMenu(item));
    }

    /**
     * Add a menu to a menu bar.  The menu includes the top-level label (the label in the menu bar) and the dropdown
     * menu that is shown on hover over the label.
     */
    public void addMenu(SimpleMenu menu) {
        _panel.add(menu);
    }
}
