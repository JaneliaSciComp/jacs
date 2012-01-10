
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Modal alert popup.
 */
public class MessagePopupPanel extends BasePopupPanel {
    private String _buttonText = "Ok";
    private String _message = "";

    public MessagePopupPanel(String title, String message) {
        super(title, /*realize now*/ false);
        _message = message;
    }

    public MessagePopupPanel(String title, String message, String buttonText) {
        super(title, /*realize now*/ false);
        _message = message;
        setButtonText(buttonText);
    }

    private void setButtonText(String buttonText) {
        _buttonText = buttonText;
        // Update button if already created
        if (getButtonSet() != null && getButtonSet().getButtons() != null && getButtonSet().getButtons().size() == 1)
            getButtonSet().getButtons().iterator().next().setText(_buttonText);
    }

    protected void populateContent() {
        add(HtmlUtils.getHtml(_message, "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

    protected ButtonSet createButtons() {
        return new ButtonSet(new RoundedButton[]{
                new RoundedButton(_buttonText, new ClickListener() {
                    public void onClick(Widget widget) {
                        hide();
                    }
                })
        });
    }
}