
package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

/**
 * @author Michael Press
 */
public class SmallRoundedButton extends RoundedButton {
    public static final String SMALL_BUTTON_STYLE = "smallButtonPanel";
    public static final String SMALL_BUTTON_HOVER_STYLE = "smallButtonPanelHover";

    public SmallRoundedButton(String text, ClickListener clickListener) {
        super(text, clickListener);
    }

    public SmallRoundedButton(String text) {
        super(text);
    }

    protected RoundedPanel2 createRoundedPanel(Widget contents) {
        return new RoundedPanel2(contents, RoundedPanel2.ALL, BORDER_COLOR, RoundedPanel2.ROUND_MEDIUM);
    }

    protected String getLabelStyleName() {
        return SMALL_BUTTON_STYLE;
    }

    protected String getLabelHoverStyleName() {
        return SMALL_BUTTON_HOVER_STYLE;
    }
}
