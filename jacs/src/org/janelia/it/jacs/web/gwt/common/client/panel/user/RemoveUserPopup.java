
package org.janelia.it.jacs.web.gwt.common.client.panel.user;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.UserDataVO;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 21, 2007
 * Time: 10:09:36 AM
 */
public class RemoveUserPopup extends ModalPopupPanel {
    private UserDataVO user;
    private RemoveUserListener userRemover;

    public RemoveUserPopup(UserDataVO user, RemoveUserListener userRemover, boolean realizeNow) {
        super("Confirm Delete", realizeNow);
        this.userRemover = userRemover;
        this.user = user;
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
            public void onClick(Widget widget) {
                userRemover.removeUser(user.getUserId().toString());
                hide();
            }
        });
        tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {
        add(HtmlUtils.getHtml("Delete user \"" + user.getUserLogin() + "\" ?", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

}