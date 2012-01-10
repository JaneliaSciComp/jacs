
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author Michael Press
 */
public class ButtonSet extends HorizontalPanel {
    private CenteredWidgetHorizontalPanel _panel;
    private HashMap<String, RoundedButton> _buttons = new HashMap<String, RoundedButton>();

    private static final String DEFAULT_STYLE_NAME = "buttonSet";

    public ButtonSet(RoundedButton[] buttons) {
        super();
        init(buttons);
    }

    private void init(RoundedButton[] buttons) {
        HorizontalPanel innerPanel = new HorizontalPanel();
        _panel = new CenteredWidgetHorizontalPanel();
        _panel.add(innerPanel);
        add(_panel);

        setStyleName(DEFAULT_STYLE_NAME);
        setWidth("100%");

        for (RoundedButton button : buttons) {
            innerPanel.add(button);
            innerPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
            _buttons.put(button.getText(), button);
        }
    }

    public void setStyleName(String style) {
        if (style != null)
            _panel.setStyleName(style);
    }

    public RoundedButton getButton(String name) {
        return _buttons.get(name);
    }

    public Collection<RoundedButton> getButtons() {
        return _buttons.values();
    }
}
