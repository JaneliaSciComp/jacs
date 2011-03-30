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
