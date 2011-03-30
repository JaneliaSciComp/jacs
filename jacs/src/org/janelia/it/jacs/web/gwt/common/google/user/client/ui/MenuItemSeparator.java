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

package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;

/**
 * A separator that can be placed in a
 * {@link com.google.gwt.user.client.ui.MenuBar}.
 */
public class MenuItemSeparator extends UIObject {

    private static final String STYLENAME_DEFAULT = "gwt-MenuItemSeparator";

    private MenuBar parentMenu;

    /**
     * Constructs a new {@link MenuItemSeparator}.
     */
    public MenuItemSeparator() {
        setElement(DOM.createTD());
        setStyleName(STYLENAME_DEFAULT);

        // Add an inner element for styling purposes
        Element div = DOM.createDiv();
        DOM.appendChild(getElement(), div);
        setStyleName(div, "content");
    }

    /**
     * Gets the menu that contains this item.
     *
     * @return the parent menu, or <code>null</code> if none exists.
     */
    public MenuBar getParentMenu() {
        return parentMenu;
    }

    void setParentMenu(MenuBar parentMenu) {
        this.parentMenu = parentMenu;
    }
}

