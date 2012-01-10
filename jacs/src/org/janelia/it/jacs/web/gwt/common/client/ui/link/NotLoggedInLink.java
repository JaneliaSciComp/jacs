
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;

/**
 * This looks like a regular link but pops up a "You must be logged in to view this data" dialog box when clicked.
 * This link can be used in place of a normal link to login-protected data when the user is not logged in.
 *
 * @author Michael Press
 */
public class NotLoggedInLink extends Link {
    public NotLoggedInLink(String linkText) {
        this(linkText, null);
    }

    public NotLoggedInLink(String linkText, String popupMessage) {
        super(linkText, (ClickListener) null);
        init(popupMessage);
    }

    private void init(final String popupMessage) {
        addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                new PopupAboveLauncher(new NotLoggedInPopupPanel(popupMessage)).showPopup(widget);
            }
        });
        setHyperlinkStyleName("notLoggedInTextLink");
    }
}
