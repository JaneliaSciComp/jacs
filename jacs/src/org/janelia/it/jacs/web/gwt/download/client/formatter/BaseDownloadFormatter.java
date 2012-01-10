
package org.janelia.it.jacs.web.gwt.download.client.formatter;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.ClickableGrid;
import org.janelia.it.jacs.web.gwt.common.client.panel.WidgetTabBarVerticalTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

public class BaseDownloadFormatter {
    protected CommonFormatter common = new CommonFormatter();

    /**
     * Creates, and returns, a tab listener to update the image in a tab.
     *
     * @return the listener.
     */
    public TabListener getTabWidgetImageSwapper(WidgetTabBarVerticalTabPanel panel) {
        return new BaseDownloadFormatter.TabWidgetImageSwapper(panel);
    }

    public class TabWidgetImageSwapper implements TabListener {
        private WidgetTabBarVerticalTabPanel correctSource;

        public TabWidgetImageSwapper(WidgetTabBarVerticalTabPanel correctSource) {
            this.correctSource = correctSource;
        }

        /*Details*
         * Called before the tab is selected.
         *
         * @param source who fired this event?
         * @param tabNumber which tab was selected?
         * @return return false to disallow.  Here, always return true.
         */
        public boolean onBeforeTabSelected(SourcesTabEvents source, int tabNumber) {
            // Only deal with events on what we intend to listen.
            if (source.equals(correctSource)) {
                int oldSelectedTab = correctSource.getWidgetTabBar().getSelectedTab();
                if (oldSelectedTab != -1)
                    setImage(oldSelectedTab, ImageBundleFactory.getControlImageBundle().getSquareBulletUnselectedImage());
                setImage(tabNumber, ImageBundleFactory.getControlImageBundle().getSquareBulletSelectedImage());
            }
            return true;
        }

        /**
         * Called when tab is selected.
         *
         * @param source    what was the source.
         * @param tabNumber which tab was selected.
         */
        public void onTabSelected(SourcesTabEvents source, int tabNumber) {
        }

        /**
         * Helper to change the image URL on the bullet image stored in the Grid
         * at the tab whose number is given.
         *
         * @param tabNumber      which tab to change
         * @param imagePrototype template for the bullet image
         */
        private void setImage(int tabNumber, AbstractImagePrototype imagePrototype) {
            ClickableGrid grid = correctSource.getWidgetTabBar().getTab(tabNumber);
            imagePrototype.applyTo((Image) grid.getWidget(0, 0));
        }
    }
}
