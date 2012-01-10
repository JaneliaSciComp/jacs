
package org.janelia.it.jacs.web.gwt.common.client.panel.user;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
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
public class RemoveNodePopup extends ModalPopupPanel {
    private String nodeName;
    private String nodeId;
    private RemoveNodeListener nodeRemover;

    public RemoveNodePopup(String nodeName, String nodeId, RemoveNodeListener nodeRemover, boolean realizeNow) {
        super("Confirm Delete", realizeNow);
        this.nodeRemover = nodeRemover;
        this.nodeName = nodeName;
        this.nodeId = nodeId;
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
            public void onClick(Widget widget) {
                nodeRemover.removeNode(nodeId);
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
        add(HtmlUtils.getHtml("Delete sequence \"" + nodeName + "\" ?", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

}