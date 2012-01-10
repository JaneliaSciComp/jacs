
package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.ui.*;

/**
 * A menu that can be placed in a SimpleMenuBar.  A menu has a top-level label (like "File") and a dropdown containing
 * SimpleMenuItems.  The dropdown is shown when the mouse hovers on the label, and is closed when the mouse leaves
 * (unlike GWT menus which will not auto-close).
 *
 * @author Michael Press
 */
public class SimpleMenu extends Composite {
    private static final String STYLE_NAME = "HeaderTopLevelMenuItem";
    private static final String HOVER_STYLE_NAME = "HeaderTopLevelMenuItemHover";
    private static final String DROPDOWN_STYLENAME = "HeaderMenuBarPopup";
    private VerticalPanel _menuContents;
    private SimpleMenuBarController _controller;

    public SimpleMenu(SimpleMenuItem item) {
        //TODO: SimpleMenuItem should extend Composite
        init(item, /*hasMenu*/ false);
    }

    public SimpleMenu(HTML topLevelMenuItem, SimpleMenuItem[] items) {
        init(topLevelMenuItem, /*hasMenu*/ true);
        addItems(items);
    }

    private void init(HTML topLevelMenuItem, boolean hasMenu) {
        // Style the top level menu item
        topLevelMenuItem.setStyleName(STYLE_NAME);
        topLevelMenuItem.addMouseListener(new TopLevelMenuItemStyleSetter(topLevelMenuItem));

        // If the top level menu item will show a menu, create the popup for the dropdown
        if (hasMenu) {
            // Create a panel for the menu items in the menu dropdown
            _menuContents = new VerticalPanel();
            _menuContents.setStyleName(DROPDOWN_STYLENAME);

            // Create a popup that will show the menu
            PopupPanel popup = new PopupPanel();
            popup.add(_menuContents);

            // The controller needs to know about the popup so it can close it
            _controller = new SimpleMenuBarController(topLevelMenuItem, popup);
        }

        initWidget(topLevelMenuItem);
    }

    /**
     * Adds a SimpleMenuItem to the menu.
     *
     * @param item the item to add to the menu
     * @throws IllegalStateException if this SimpleMenu was constructed with a SimpleMenuItem instead of a SimpleMenu
     *                               (meaning if this SimpleMenu is just a button, you can't add items to it)
     */
    public void addItem(SimpleMenuItem item)
            throws IllegalStateException {
        if (_menuContents == null)
            throw new IllegalStateException("You can't add a SimpleMenuItem to a SimpleMenu that's just one SimpleMenu. " +
                    "Use the SimpleMenu(SimpleMenuItem[]) constructor instead");

        _menuContents.add(item);
        item.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _controller.onMenuItemSelected();
            }
        });
    }

    public void addItems(SimpleMenuItem[] items) {
        for (SimpleMenuItem item : items)
            addItem(item);
    }

    private class TopLevelMenuItemStyleSetter extends MouseListenerAdapter {
        private Widget _label;

        private TopLevelMenuItemStyleSetter(HTML label) {
            _label = label;
        }

        public void onMouseLeave(Widget sender) {
            _label.setStyleName(STYLE_NAME);
        }

        public void onMouseEnter(Widget sender) {
            _label.setStyleName(HOVER_STYLE_NAME);
        }
    }
}
