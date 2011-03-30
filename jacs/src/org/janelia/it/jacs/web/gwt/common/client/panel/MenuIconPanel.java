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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class MenuIconPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.MenuIconPanel");

    private class MenuIconSelectionListener implements SelectionListener {
        public void onSelect(String value) {
            for (Iterator menuIconItr = _menuIcons.entrySet().iterator(); menuIconItr.hasNext();) {
                Map.Entry menuIconEntry = (Map.Entry) menuIconItr.next();
                String menuKey = (String) menuIconEntry.getKey();
                MenuIcon menuIcon = (MenuIcon) menuIconEntry.getValue();
                if (!menuKey.equals(value)) {
                    menuIcon.unSelect();
                }
            }
        }

        public void onUnSelect(String value) {
        }
    }

    private Map _menuIcons;
    private HorizontalPanel iconPanel;
    private SelectionListener defaultIconSelectionListener;

    public MenuIconPanel() {
        super();
        _menuIcons = new HashMap();
        iconPanel = new HorizontalPanel();
        defaultIconSelectionListener = new MenuIconSelectionListener();
        initWidget(iconPanel);
    }

    public Panel getIconPanel() {
        return iconPanel;
    }

    public void addIcon(MenuIcon menuIcon) {
        menuIcon.addSelectionListener(defaultIconSelectionListener);
        iconPanel.add(menuIcon);
        _menuIcons.put(menuIcon.getHeader(), menuIcon);

    }

    public List getSelectedIcons() {
        List selectedIcons = new ArrayList();
        Iterator iter = _menuIcons.values().iterator();
        while (iter.hasNext()) {
            MenuIcon mi = (MenuIcon) iter.next();
            if (mi.isSelected()) {
                selectedIcons.add(mi);
            }
        }
        return selectedIcons;
    }

    public Collection getMenuIconCollection() {
        return _menuIcons.values();
    }


    public void addIconFooter(String iconTitle, String footer) {
        MenuIcon menuIcon = (MenuIcon) _menuIcons.get(iconTitle);
        menuIcon.setFooter(footer);
    }

    public void removeIconFooter(String iconTitle) {
        MenuIcon menuIcon = (MenuIcon) _menuIcons.get(iconTitle);
        menuIcon.clearFooter();
    }

    public void clearFooters() {
        Iterator iter = _menuIcons.values().iterator();
        while (iter.hasNext()) {
            MenuIcon menuIcon = (MenuIcon) iter.next();
            menuIcon.clearFooter();
        }
    }

}

