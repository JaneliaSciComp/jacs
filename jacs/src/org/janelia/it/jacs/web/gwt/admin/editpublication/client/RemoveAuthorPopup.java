
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;


/**
 * @author Guy Cao
 *         copycat: RemoveJobPopup.java
 */

class RemoveAuthorPopup extends ModalPopupPanel {

    protected RemoveAuthorListener authorRemover;
    protected String authorName;
    protected Integer authorNumber;


    public RemoveAuthorPopup(RemoveAuthorListener authorRemover, boolean realizeNow,
                             String authorName, Integer authorNumber) {

        super("Confirm delete", realizeNow);
        this.authorRemover = authorRemover;
        //this.jobStatus=jobStatus;
        this.authorName = authorName;
        this.authorNumber = authorNumber;

    }

    /* called by supper: BasePopupPanel */
    protected ButtonSet createButtons() {
        final RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
            public void onClick(Widget widget) {
                authorRemover.removeAuthor(authorNumber);
                tmpButtons[0].setEnabled(false);
                tmpButtons[1].setEnabled(false);
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


    protected void populateContent() {

        add(HtmlUtils.getHtml("Delete author " + "\"" + authorName + "\"" + "?", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }
}
