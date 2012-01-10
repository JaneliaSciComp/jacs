
package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

/**
 * A SimpleMenuItem is an item in a menu.  It has a label and a Command that is executed when the item is clicked.
 *
 * @author Michael Press
 */
public class SimpleMenuItem extends HTML {
    public static final String STYLE_NAME = "HeaderMenuItem";
    public static final String HOVER_STYLE_NAME = "HeaderMenuItemHover";

    private Command _command;

    public SimpleMenuItem(String html, Command command) {
        super(html);
        _command = command;
        init();
    }

    private void init() {
        setStyleName(STYLE_NAME);
        addMouseListener(new MouseListenerAdapter() {
            public void onMouseLeave(Widget sender) {
                setStyleName(STYLE_NAME);
            }

            public void onMouseEnter(Widget sender) {
                setStyleName(HOVER_STYLE_NAME);
            }
        });
        addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _command.execute();
            }
        });
    }
}
