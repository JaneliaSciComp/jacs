/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
