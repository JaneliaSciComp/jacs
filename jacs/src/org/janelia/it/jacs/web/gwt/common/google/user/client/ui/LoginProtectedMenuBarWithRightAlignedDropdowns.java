
package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.Command;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;


/**
 * @author Michael Press
 */
public class LoginProtectedMenuBarWithRightAlignedDropdowns extends MenuBarWithRightAlignedDropdowns {
    public static final String DEFAULT_MESSAGE = "You must be logged in to perform this function.";

    public LoginProtectedMenuBarWithRightAlignedDropdowns() {
        this(false);
    }

    public LoginProtectedMenuBarWithRightAlignedDropdowns(boolean vertical) {
        super(vertical);
        init(DEFAULT_MESSAGE);
    }

    public LoginProtectedMenuBarWithRightAlignedDropdowns(boolean vertical, String notLoggedInMessage) {
        super(vertical);
        init(notLoggedInMessage);
    }

    private void init(String msg) {
        if (!ClientSecurityUtils.isAuthenticated()) {
            MenuItem item = new MenuItem(msg, new Command() {
                public void execute() { /* do nothing */ }
            });
            item.addStyleName("gwt-MenuItem-NotLoggedIn");
            super.addItem(item);
        }
    }

    public void addItem(MenuItem item) {
        if (ClientSecurityUtils.isAuthenticated())
            super.addItem(item);
    }

    public MenuItem addItem(String text, boolean asHTML, Command cmd) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, asHTML, cmd);
        else
            return null;
    }

    public MenuItem addItem(String text, boolean asHTML, MenuBar popup) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, asHTML, popup);
        else
            return null;
    }

    public MenuItem addItem(String text, Command cmd) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, cmd);
        else
            return null;
    }

    public MenuItem addItem(String text, MenuBar popup) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, popup);
        else
            return null;
    }
}
