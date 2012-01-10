
package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;

/**
 * Extends MenuItem but replaces the specified Command action with a NotLoggedInPopupPanel if the user
 * is not logged in.  Also sets the menu item style name to gwt-MenuItem-NotLoggedIn.
 *
 * @author Michael Press
 */
public class LoginProtectedMenuItem extends MenuItem {
    public LoginProtectedMenuItem(String text, boolean asHTML, Command cmd) {
        super(text, asHTML, cmd);
        init();
    }

    public LoginProtectedMenuItem(String text, boolean asHTML, MenuBar subMenu) {
        super(text, asHTML, subMenu);
        init();
    }

    public LoginProtectedMenuItem(String text, Command cmd) {
        super(text, cmd);
        init();
    }

    public LoginProtectedMenuItem(String text, MenuBar subMenu) {
        super(text, subMenu);
        init();
    }

    private void init() {
        setStyleName("gwt-MenuItem-NotLoggedIn");
    }

    public Command getCommand() {
        if (!ClientSecurityUtils.isAuthenticated()) {
            return new Command() {
                public void execute() {
                    Widget parent = getParentMenu().getParent();
                    new PopupAboveLauncher(new NotLoggedInPopupPanel("You must be logged in to export.")).showPopup(parent);
                }
            };
        }
        else return super.getCommand();
    }

    void setSelectionStyle(boolean selected) {
    }
}
