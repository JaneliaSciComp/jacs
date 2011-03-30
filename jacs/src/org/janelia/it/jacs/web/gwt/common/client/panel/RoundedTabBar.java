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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;


/**
 * @author Michael Press
 */
public class RoundedTabBar extends org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabBar {
    private HashMap _labels = new HashMap();

    /**
     * Overrides com.google.gwt.user.client.ui.TabBar to insert a rounded tab instead of a plain label
     */
    public void insertTab(String text, boolean asHTML, int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex > getTabCount()))
            throw new IndexOutOfBoundsException();

        Label item;
        if (asHTML)
            item = new HTML(text);
        else
            item = new Label(text);

        item.setWordWrap(false);
        item.setStyleName("roundedTabBarItem");
        item.addClickListener(new ItemClickListener(beforeIndex));

        RoundedPanel2 panel = new RoundedPanel2(item, RoundedPanel2.TOP, "#AAAAAA");
        panel.setStyleName("roundedTab");
        panel.setCornerStyleName("roundedTabBarItemRounding");

        // Save the panel->label relationship for when a tab is selected
        _labels.put(panel.getElement().hashCode(), item);

        getPanel().insert(panel, beforeIndex + 1);
    }

    /**
     * Overrides com.google.gwt.user.client.ui.TabBar to set our rounded tab styles
     */
    protected void setSelectionStyle(Widget item, boolean selected) {
        if (item != null) {
            RoundedPanel2 panel = (RoundedPanel2) item;
            Widget label = (Widget) _labels.get(new Integer(panel.getElement().hashCode()));
            if (selected) {
                panel.setCornerStyleName("roundedTabBarItemRounding-selected");
                label.addStyleName("roundedTabBarItem-selected");
            }
            else {
                panel.setCornerStyleName("roundedTabBarItemRounding");
                label.removeStyleName("roundedTabBarItem-selected");
            }
        }
    }

    // TODO: This won't work if tabs are reordered or a new tab is inserted before this one
    protected class ItemClickListener implements ClickListener {
        int _tabNum;

        public ItemClickListener(int tabNum) {
            _tabNum = tabNum;
        }

        public void onClick(Widget widget) {
            selectTab(_tabNum);
        }
    }

}
