
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 4, 2007
 * Time: 4:39:10 PM
 */
public class FrvUserPipelinePopup extends BasePopupPanel {

    private VerticalPanel panel = new VerticalPanel();

    public FrvUserPipelinePopup() {
        super("Fragment Recruitment Viewer", /*realizeNow*/ false, /*autohide*/ true, /*modal*/ true);
    }

    protected void populateContent() {
        this.add(panel);
//        panel.setSize("400px", "100px");
        panel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        panel.add(new Label("Your pipeline request has been successful."));
    }

    protected ButtonSet createButtons() {
        RoundedButton continueButton = new RoundedButton("Continue Browsing", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
            }
        });
        RoundedButton statusButton = new RoundedButton("Watch Pipeline Status", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
                String url = UrlBuilder.getStatusUrl();
                Window.open(url, "_self", "");
            }
        });
        return new ButtonSet(new RoundedButton[]{continueButton, statusButton});
    }

    /**
     * Hook for inner classes to hide the popup
     */
    protected void hidePopup() {
        this.hide();
    }

}