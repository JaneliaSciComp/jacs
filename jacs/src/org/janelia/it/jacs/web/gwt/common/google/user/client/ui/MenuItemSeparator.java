
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

