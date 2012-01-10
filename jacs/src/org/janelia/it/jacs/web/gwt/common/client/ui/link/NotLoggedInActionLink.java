
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;

/**
 * This looks like a regular action link but pops up a "You must be logged in to view this data" dialog box when clicked.
 * This link can be used in place of a normal action link to login-protected data when the user is not logged in.
 *
 * @author Michael Press
 */
public class NotLoggedInActionLink extends ActionLink {
    public NotLoggedInActionLink(String linkText, Image image, String popupMessage) {
        super(linkText);
        setShowBrackets(false);
        setImage(image);
        init(popupMessage);
    }

    private void init(final String popupMessage) {
        addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                new PopupAboveLauncher(new NotLoggedInPopupPanel(popupMessage)).showPopup(widget);
            }
        });
        setLinkStyleName("notLoggedInActionLink");
    }
}